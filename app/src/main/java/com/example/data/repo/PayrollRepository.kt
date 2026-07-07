package com.example.data.repo

import com.example.data.db.AppDatabase
import com.example.data.model.PayrollRecord
import com.example.data.model.Worker
import com.example.domain.PayrollCalculator
import java.time.LocalDate
import java.time.YearMonth

class PayrollRepository(private val db: AppDatabase) {

    data class PayrollRow(
        val worker: Worker,
        val attendance: PayrollCalculator.AttendanceSummary,
        val pay: PayrollCalculator.PaySummary,
        val status: String, // unsaved, pending, paid
        val recordId: Long?,
    )

    data class PayrollTotals(
        val totalGross: Double,
        val totalDeductions: Double,
        val totalNet: Double,
        val paidCount: Int,
        val pendingCount: Int,
        val workerCount: Int,
    )

    /** Build one payroll row per active worker for the given month, computing
     *  the full pay summary from attendance, transactions, closures & projects. */
    suspend fun buildRows(month: Int, year: Int): Pair<List<PayrollRow>, PayrollTotals> {
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        val workers = db.workerDao().activeOnce()
        val projectsById = db.projectDao().allOnce().associateBy { it.id }
        val allAssignments = db.assignmentDao().allOnce().groupBy { it.workerId }
        val closures = db.closureDao().betweenDates(start, end)

        val rows = mutableListOf<PayrollRow>()
        var totalGross = 0.0; var totalDeductions = 0.0; var totalNet = 0.0
        var paidCount = 0; var pendingCount = 0

        for (worker in workers) {
            val records = db.attendanceDao().forWorkerBetween(worker.id, start, end)
            val txns = db.transactionDao().activeForWorkerBetween(worker.id, start, end)
            val assignments = allAssignments[worker.id] ?: emptyList()
            val leaveAdjustments = db.leaveAdjustmentDao().forWorkerOnce(worker.id)
            val leaveUsedBefore = db.attendanceDao().leaveDaysBefore(worker.id, start)

            val att = PayrollCalculator.attendanceSummary(records)
            val pay = PayrollCalculator.paySummary(
                PayrollCalculator.PayInputs(
                    worker = worker,
                    records = records,
                    periodStart = start,
                    periodEnd = end,
                    transactions = txns,
                    assignments = assignments,
                    projectsById = projectsById,
                    closures = closures,
                    leaveAdjustments = leaveAdjustments,
                    leaveDaysUsedBefore = leaveUsedBefore,
                )
            )

            val existing = db.payrollDao().find(worker.id, month, year)
            val status = existing?.status ?: "unsaved"

            totalGross += pay.grossPay
            totalDeductions += pay.totalDeductions
            totalNet += pay.estimatedPay
            if (status == "paid") paidCount++ else pendingCount++

            rows += PayrollRow(worker, att, pay, status, existing?.id)
        }

        val totals = PayrollTotals(
            PayrollCalculator.round2(totalGross),
            PayrollCalculator.round2(totalDeductions),
            PayrollCalculator.round2(totalNet),
            paidCount, pendingCount, rows.size,
        )
        return rows to totals
    }

    /** Persist the computed payroll for the month (keeps existing 'paid' status). */
    suspend fun generate(month: Int, year: Int): Int {
        val (rows, _) = buildRows(month, year)
        for (row in rows) {
            val existing = db.payrollDao().find(row.worker.id, month, year)
            val record = (existing ?: PayrollRecord(
                workerId = row.worker.id, month = month, year = year,
                totalDays = 0, presentDays = 0, grossPay = 0.0, netPay = 0.0,
            )).copy(
                totalDays = row.attendance.totalMarkedDays,
                presentDays = row.pay.paidDays,
                overtimeHours = PayrollCalculator.round2(row.pay.overtimeMinutes / 60.0),
                grossPay = row.pay.grossPay,
                deductions = row.pay.totalDeductions,
                netPay = row.pay.estimatedPay,
                status = if (existing?.status == "paid") "paid" else "pending",
            )
            db.payrollDao().upsert(record)
        }
        return rows.size
    }

    suspend fun togglePaid(recordId: Long) {
        val record = db.payrollDao().byId(recordId) ?: return
        db.payrollDao().upsert(record.copy(status = if (record.status == "paid") "pending" else "paid"))
    }

    suspend fun slip(worker: Worker, month: Int, year: Int): PayrollRow {
        val (rows, _) = buildRows(month, year)
        return rows.firstOrNull { it.worker.id == worker.id }
            ?: PayrollRow(
                worker,
                PayrollCalculator.AttendanceSummary(0, 0, 0, 0),
                PayrollCalculator.paySummary(
                    PayrollCalculator.PayInputs(worker, emptyList(), YearMonth.of(year, month).atDay(1), YearMonth.of(year, month).atEndOfMonth())
                ),
                "unsaved", null,
            )
    }

    suspend fun historyForWorker(workerId: Long, limit: Int = 12): List<PayrollRecord> =
        db.payrollDao().historyForWorker(workerId, limit)
}

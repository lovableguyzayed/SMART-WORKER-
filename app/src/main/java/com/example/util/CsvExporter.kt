package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.repo.PayrollRepository
import java.io.File
import java.time.YearMonth

/**
 * CSV export system (port of Flask /payroll/export.csv and /export_data).
 * Files are written to cache and handed to the system share sheet.
 */
object CsvExporter {

    private fun esc(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"${s.replace("\"", "\"\"")}\""
        } else s
    }

    private fun row(vararg cells: Any?): String = cells.joinToString(",") { esc(it) }

    fun payrollCsv(rows: List<PayrollRepository.PayrollRow>, period: YearMonth): String = buildString {
        appendLine(
            row(
                "Worker ID", "Name", "Department", "Pay Type", "Days Marked", "Paid Days",
                "Overtime Min", "Base Pay", "Overtime Pay", "Bonus/Extras", "Gross Pay",
                "Leave Deduction", "Late Deduction", "Delay Penalty", "Advances/Recoveries",
                "Total Deductions", "Net Pay", "Status",
            )
        )
        rows.forEach { r ->
            appendLine(
                row(
                    r.worker.workerCode, r.worker.fullName, r.worker.department, r.worker.payType,
                    r.attendance.totalMarkedDays, r.pay.paidDays, r.pay.overtimeMinutes,
                    r.pay.basePay, r.pay.overtimePay, r.pay.transactionEarnings, r.pay.grossPay,
                    r.pay.leaveDeductions, r.pay.lateDeductions, r.pay.delayPenalty,
                    r.pay.transactionDeductions, r.pay.totalDeductions, r.pay.estimatedPay, r.status,
                )
            )
        }
        appendLine()
        appendLine(row("Period", period.toString()))
    }

    fun workersCsv(workers: List<Worker>): String = buildString {
        appendLine(
            row(
                "Worker ID", "Name", "Phone", "Email", "Position", "Department",
                "Employee Type", "Pay Type", "Daily Rate", "Monthly Salary", "Hourly Rate",
                "Project Rate", "Join Date", "Status",
            )
        )
        workers.forEach { w ->
            appendLine(
                row(
                    w.workerCode, w.fullName, w.phone, w.email, w.position, w.department,
                    w.employeeType, w.payType, w.dailyRate, w.monthlySalary, w.hourlyRate,
                    w.projectRate, w.joinDate, w.status,
                )
            )
        }
    }

    fun attendanceCsv(records: List<AttendanceRecord>, workersById: Map<Long, Worker>): String = buildString {
        appendLine(row("Date", "Worker ID", "Name", "Status", "Check In", "Check Out", "Late Min", "OT Min", "Leave Type", "Marked Via"))
        records.sortedBy { it.date }.forEach { r ->
            val w = workersById[r.workerId]
            appendLine(
                row(
                    r.date, w?.workerCode ?: r.workerId, w?.fullName ?: "?", r.status,
                    r.checkInTime ?: "", r.checkOutTime ?: "", r.lateMinutes, r.overtimeMinutes,
                    r.leaveType ?: "", r.markedVia,
                )
            )
        }
    }

    fun transactionsCsv(txns: List<WorkerTransaction>, workersById: Map<Long, Worker>): String = buildString {
        appendLine(row("Date", "Worker ID", "Name", "Type", "Direction", "Amount", "Status", "Description"))
        txns.sortedBy { it.date }.forEach { t ->
            val w = workersById[t.workerId]
            appendLine(
                row(
                    t.date, w?.workerCode ?: t.workerId, w?.fullName ?: "?", t.txnType,
                    if (t.isEarning) "earning" else "deduction", t.amount, t.status, t.description,
                )
            )
        }
    }

    /** Write [content] to cache and open the system share sheet. */
    fun share(context: Context, fileName: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $fileName"))
    }
}

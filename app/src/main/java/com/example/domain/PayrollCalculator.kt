package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClosureDay
import com.example.data.model.ClosureScope
import com.example.data.model.LeaveAdjustment
import com.example.data.model.PayTypes
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Pure-Kotlin port of the SmartWorker payroll & attendance business rules.
 * Mirrors the Flask reference implementation (routes.py) so both systems
 * produce identical numbers for the same inputs.
 */
object PayrollCalculator {

    // ── Attendance / shift helpers ──────────────────────────────────────────

    fun scheduledMinutesForDay(worker: Worker): Int {
        val start = worker.startTime ?: return 0
        val end = worker.endTime ?: return 0
        var minutes = Duration.between(start, end).toMinutes()
        if (minutes <= 0) minutes += 24 * 60 // overnight shift wraps to next day
        return maxOf(minutes.toInt(), 0)
    }

    fun workedMinutes(record: AttendanceRecord?): Int = record?.workedMinutes ?: 0

    fun halfDayGraceMinutes(worker: Worker): Int = worker.halfDayGraceMinutes.coerceIn(15, 25)

    fun lateGraceMinutes(worker: Worker): Int = maxOf(worker.lateGraceMinutes, 0)

    enum class PayMode { NONE, FULL_DAY, HALF_DAY, PRO_RATA }

    /** Daily wage owed for one attendance record (daily-rate workers). */
    fun dailyWageForRecord(worker: Worker, record: AttendanceRecord?): Pair<Double, PayMode> {
        val dailyRate = worker.dailyRate ?: 0.0
        val halfDayRate = worker.halfDayRate ?: if (dailyRate > 0) dailyRate / 2.0 else 0.0

        if (record == null || record.status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) {
            return 0.0 to PayMode.NONE
        }

        val scheduled = scheduledMinutesForDay(worker)
        val worked = workedMinutes(record)
        if (scheduled <= 0 || worked <= 0) return dailyRate to PayMode.FULL_DAY

        val halfDayLimit = scheduled / 2.0 + halfDayGraceMinutes(worker)
        if (worked <= halfDayLimit) return halfDayRate to PayMode.HALF_DAY
        if (worked >= scheduled) return dailyRate to PayMode.FULL_DAY

        // After the grace window, pay follows exact minute-based pro-rata.
        return (worked.toDouble() / scheduled) * dailyRate to PayMode.PRO_RATA
    }

    fun overtimeMinutesForRecord(worker: Worker, record: AttendanceRecord?): Int {
        if (!worker.overtimeEnabled || record == null) return 0
        val worked = workedMinutes(record)
        if (worked <= 0) return 0
        var scheduled = scheduledMinutesForDay(worker)
        if (scheduled <= 0) scheduled = maxOf(worker.standardWorkingHours, 1) * 60
        return maxOf(worked - scheduled, 0)
    }

    fun lateMinutesForRecord(worker: Worker, record: AttendanceRecord?): Int {
        if (record == null) return 0
        if (record.status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) return 0
        val checkIn = record.checkInTime ?: return 0
        val shiftStart = worker.startTime ?: return 0
        val startAt = LocalDateTime.of(record.date, shiftStart)
        val late = Duration.between(startAt, checkIn).toMinutes()
        return maxOf(late, 0).toInt()
    }

    fun isLateCheckIn(worker: Worker, date: LocalDate, checkIn: LocalDateTime?): Boolean {
        val shiftStart = worker.startTime ?: return false
        if (checkIn == null) return false
        val threshold = LocalDateTime.of(date, shiftStart).plusMinutes(lateGraceMinutes(worker).toLong())
        return checkIn > threshold
    }

    fun overtimePay(worker: Worker, overtimeMinutes: Int): Double {
        if (!worker.overtimeEnabled || overtimeMinutes <= 0) return 0.0
        val rate = worker.overtimeRate ?: return 0.0
        val units = if (worker.overtimeType == "minute") overtimeMinutes.toDouble() else overtimeMinutes / 60.0
        return units * rate
    }

    // ── Closures ────────────────────────────────────────────────────────────

    fun closureAppliesToWorker(
        closure: ClosureDay,
        assignments: List<ProjectAssignment>,
        onDate: LocalDate? = null,
    ): Boolean {
        if (closure.scope.isBlank() || closure.scope == ClosureScope.COMPANY) return true
        val checkDate = onDate ?: closure.date
        for (a in assignments) {
            if (!a.activeOn(checkDate)) continue
            if (closure.scope == ClosureScope.SITE && closure.siteId != null && a.siteId == closure.siteId) return true
            if (closure.scope == ClosureScope.PROJECT && closure.projectId != null && a.projectId == closure.projectId) return true
        }
        return false
    }

    /** The closure that governs this worker on a date; a locked closure wins. */
    fun closureForWorkerOnDate(
        closures: List<ClosureDay>,
        assignments: List<ProjectAssignment>,
        onDate: LocalDate,
    ): ClosureDay? {
        val applicable = closures.filter { it.date == onDate && closureAppliesToWorker(it, assignments, onDate) }
        if (applicable.isEmpty()) return null
        return applicable.firstOrNull { !it.allowAttendance } ?: applicable.first()
    }

    // ── Leave ledger ────────────────────────────────────────────────────────

    data class LeaveBalance(
        val monthlyQuota: Int,
        val monthsAccrued: Int,
        val accruedTotal: Int,
        val manualAdjustment: Double,
        val usedBefore: Int,
        val usedThisMonth: Int,
        val balanceBefore: Double,
        val availableThisMonth: Double,
        val chargeableDays: Int,
        val balanceAfter: Double,
    )

    private fun monthsInRange(start: LocalDate, end: LocalDate): Int {
        if (end < start) return 0
        return ((end.year - start.year) * 12 + (end.monthValue - start.monthValue) + 1)
    }

    /**
     * Full leave ledger from joining through the payroll month. Unused monthly
     * quota accumulates; extra leaves consume the accumulated balance first and
     * only the remainder is chargeable against salary.
     *
     * @param adjustments all manual leave adjustments for the worker
     * @param usedBefore count of leave-status attendance days before [periodStart]
     * @param usedThisMonth count of leave-status days within the period
     */
    fun leaveBalance(
        worker: Worker,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        adjustments: List<LeaveAdjustment>,
        usedBefore: Int,
        usedThisMonth: Int,
    ): LeaveBalance {
        val quota = maxOf(worker.allowedLeavesPerMonth, 0)
        val joinDate = worker.joinDate
        val monthsTotal = monthsInRange(joinDate, periodEnd)
        val monthsBefore = maxOf(monthsTotal - 1, 0)
        val accruedBefore = monthsBefore * quota
        val accruedTotal = monthsTotal * quota

        // NULL effective date means "applies from creation"; bound those by
        // createdAt so later adjustments never leak into past payroll months.
        val periodEndTs = periodEnd.atTime(23, 59, 59)
        val manualAdjustment = adjustments.filter { adj ->
            val eff = adj.effectiveDate
            (eff == null && adj.createdAt <= periodEndTs) || (eff != null && eff <= periodEnd)
        }.sumOf { it.days }

        val balanceBefore = accruedBefore + manualAdjustment - usedBefore
        val availableThisMonth = maxOf(balanceBefore, 0.0) + quota
        val chargeableDays = maxOf((usedThisMonth - availableThisMonth).toInt(), 0)
        val balanceAfter = balanceBefore + quota - usedThisMonth

        return LeaveBalance(
            monthlyQuota = quota,
            monthsAccrued = monthsTotal,
            accruedTotal = accruedTotal,
            manualAdjustment = manualAdjustment,
            usedBefore = usedBefore,
            usedThisMonth = usedThisMonth,
            balanceBefore = balanceBefore,
            availableThisMonth = availableThisMonth,
            chargeableDays = chargeableDays,
            balanceAfter = balanceAfter,
        )
    }

    // ── Project delay penalty ───────────────────────────────────────────────

    data class DelayCharge(val projectName: String, val days: Long, val perDay: Double, val amount: Double)

    /**
     * Per-day project delay penalty for project-based workers. Only overdue
     * days inside this payroll period are charged, so a delay spanning months
     * is never double-billed.
     */
    fun delayPenalty(
        worker: Worker,
        assignments: List<ProjectAssignment>,
        projectsById: Map<Long, Project>,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): Pair<Double, List<DelayCharge>> {
        if (worker.payType != PayTypes.PROJECT) return 0.0 to emptyList()
        var total = 0.0
        val breakdown = mutableListOf<DelayCharge>()
        val seen = mutableSetOf<Long>()
        for (a in assignments) {
            val aStart = a.startDate
            val aEnd = a.endDate ?: periodEnd
            if (aStart > periodEnd || aEnd < periodStart) continue
            val project = a.projectId?.let { projectsById[it] } ?: continue
            if (!seen.add(project.id)) continue
            val deadline = project.deadline ?: continue
            if (project.penaltyType !in listOf("fixed", "percent")) continue
            if (project.penaltyValue <= 0.0) continue

            val overdueStart = deadline.plusDays(1)
            val overdueEnd = project.completionDate ?: periodEnd
            val chargeStart = maxOf(overdueStart, maxOf(periodStart, aStart))
            val chargeEnd = minOf(overdueEnd, minOf(periodEnd, aEnd))
            if (chargeEnd < chargeStart) continue

            val days = ChronoUnit.DAYS.between(chargeStart, chargeEnd) + 1
            val perDay = if (project.penaltyType == "fixed") {
                project.penaltyValue
            } else {
                (project.penaltyValue / 100.0) * (worker.projectRate ?: 0.0)
            }
            val amount = round2(days * perDay)
            if (amount > 0) {
                total += amount
                breakdown += DelayCharge(project.name, days, round2(perDay), amount)
            }
        }
        return round2(total) to breakdown
    }

    // ── Attendance summary ──────────────────────────────────────────────────

    data class AttendanceSummary(
        val presentDays: Int,
        val absentDays: Int,
        val lateDays: Int,
        val leaveDays: Int,
    ) {
        val totalMarkedDays: Int get() = presentDays + absentDays + lateDays + leaveDays
        val paidDays: Int get() = presentDays + lateDays
    }

    fun attendanceSummary(records: List<AttendanceRecord>): AttendanceSummary {
        var p = 0; var a = 0; var l = 0; var lv = 0
        for (r in records) when (r.status) {
            AttendanceStatus.PRESENT -> p++
            AttendanceStatus.ABSENT -> a++
            AttendanceStatus.LATE -> l++
            AttendanceStatus.LEAVE -> lv++
        }
        return AttendanceSummary(p, a, l, lv)
    }

    // ── Full pay summary ────────────────────────────────────────────────────

    data class PaySummary(
        val payType: String,
        val basePay: Double,
        val overtimePay: Double,
        val leaveDeductions: Double,
        val lateDeductions: Double,
        val delayPenalty: Double,
        val delayBreakdown: List<DelayCharge>,
        val transactionEarnings: Double,
        val transactionDeductions: Double,
        val transactions: List<WorkerTransaction>,
        val leaveBalance: LeaveBalance?,
        val deductions: Double,
        val estimatedPay: Double,
        val paidDays: Int,
        val fullDayDays: Int,
        val halfDayDays: Int,
        val proratedDays: Int,
        val paidAbsentDays: Int,
        val workedMinutes: Int,
        val overtimeMinutes: Int,
        val overtimeUnits: Double,
        val lateMinutes: Int,
        val lateUnits: Double,
        val lateUnitLabel: String,
        val leaveDays: Int,
        val extraLeaveDays: Int,
        val closureExtraPay: Double,
        val policyNotes: List<String>,
    ) {
        val grossPay: Double get() = round2(basePay + overtimePay + transactionEarnings)
        val totalDeductions: Double get() = round2(deductions + transactionDeductions)
    }

    data class PayInputs(
        val worker: Worker,
        val records: List<AttendanceRecord>,
        val periodStart: LocalDate,
        val periodEnd: LocalDate,
        val transactions: List<WorkerTransaction> = emptyList(),
        val assignments: List<ProjectAssignment> = emptyList(),
        val projectsById: Map<Long, Project> = emptyMap(),
        val closures: List<ClosureDay> = emptyList(),
        val leaveAdjustments: List<LeaveAdjustment> = emptyList(),
        /** Leave-status attendance days recorded before the period (for the leave ledger). */
        val leaveDaysUsedBefore: Int = 0,
    )

    fun paySummary(inputs: PayInputs): PaySummary {
        val worker = inputs.worker
        val records = inputs.records
        val summary = attendanceSummary(records)
        val notes = mutableListOf<String>()

        val overtimeMinutes = records.sumOf { maxOf(it.overtimeMinutes, 0) }
        var overtimeUnits = 0.0
        var overtimePay = 0.0
        if (worker.overtimeEnabled && (worker.overtimeRate ?: 0.0) > 0 && overtimeMinutes > 0) {
            overtimeUnits = if (worker.overtimeType == "minute") overtimeMinutes.toDouble() else overtimeMinutes / 60.0
            overtimePay = overtimeUnits * (worker.overtimeRate ?: 0.0)
        }

        var workedMinutes = 0
        var lateMinutes = 0
        for (r in records) {
            if (r.status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) continue
            if (r.checkInTime == null || r.checkOutTime == null) continue
            if (r.checkOutTime <= r.checkInTime) continue
            workedMinutes += r.workedMinutes
            lateMinutes += lateMinutesForRecord(worker, r)
        }

        var basePay = 0.0
        var leaveDeductions = 0.0
        var lateDeductions = 0.0
        var lateUnits = 0.0
        var lateUnitLabel = "day"
        var halfDayDays = 0
        var fullDayDays = 0
        var proratedDays = 0
        var paidAbsentDays = 0
        var extraLeaveDays = 0

        if (worker.payType == PayTypes.DAILY) {
            val dailyRate = worker.dailyRate ?: 0.0
            val grace = halfDayGraceMinutes(worker)
            for (r in records) {
                if (r.status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) continue
                val (dayPay, mode) = dailyWageForRecord(worker, r)
                basePay += dayPay
                when (mode) {
                    PayMode.HALF_DAY -> halfDayDays++
                    PayMode.PRO_RATA -> proratedDays++
                    else -> fullDayDays++
                }
            }
            if (!worker.noWorkNoPay && summary.absentDays > 0) {
                paidAbsentDays = summary.absentDays
                basePay += dailyRate * summary.absentDays
                notes += "Absent days are paid because no-work-no-pay is disabled."
            } else if (summary.absentDays > 0) {
                notes += "No-work-no-pay applied for absent days."
            }
            if (halfDayDays > 0) notes += "Half-day rate applied for $halfDayDays day(s) (half-shift + $grace min grace window)."
            if (proratedDays > 0) notes += "Pro-rata minute-based daily wage applied for $proratedDays day(s) after grace window."
        }

        var leaveBalance: LeaveBalance? = null
        if (worker.payType == PayTypes.MONTHLY && worker.leavePolicyEnabled) {
            val usedThisMonth = summary.leaveDays
            leaveBalance = leaveBalance(
                worker, inputs.periodStart, inputs.periodEnd,
                inputs.leaveAdjustments, inputs.leaveDaysUsedBefore, usedThisMonth,
            )
        }

        when (worker.payType) {
            PayTypes.MONTHLY -> {
                basePay = worker.monthlySalary ?: 0.0
                if (worker.leavePolicyEnabled && worker.leaveDeductionPerDay != null) {
                    extraLeaveDays = leaveBalance?.chargeableDays
                        ?: maxOf(0, summary.leaveDays - worker.allowedLeavesPerMonth)
                    leaveDeductions = extraLeaveDays * (worker.leaveDeductionPerDay)
                    if (leaveBalance != null && leaveBalance.balanceBefore > 0 && summary.leaveDays > worker.allowedLeavesPerMonth) {
                        notes += "Accumulated leave balance (${leaveBalance.balanceBefore} day(s)) adjusted before salary deduction."
                    }
                    notes += "Monthly salary applies leave deduction after quota and accumulated balance."
                } else {
                    notes += "Monthly salary with leave policy disabled."
                }
            }
            PayTypes.HOURLY -> {
                basePay = (workedMinutes / 60.0) * (worker.hourlyRate ?: 0.0)
                notes += "Hourly wage is based on check-in/check-out duration."
            }
            PayTypes.PROJECT -> {
                basePay = worker.projectRate ?: 0.0
                notes += "Project rate is fixed for the period."
            }
        }

        if (worker.latePolicyEnabled && worker.lateDeductionPerUnit != null) {
            when (worker.lateDeductionType.lowercase()) {
                "minute" -> { lateUnits = lateMinutes.toDouble(); lateUnitLabel = "minute" }
                "hour" -> { lateUnits = lateMinutes / 60.0; lateUnitLabel = "hour" }
                else -> { lateUnits = summary.lateDays.toDouble(); lateUnitLabel = "day" }
            }
            lateDeductions = lateUnits * worker.lateDeductionPerUnit
            if (lateDeductions > 0) notes += "Late policy deduction applied per $lateUnitLabel."
        }

        if (overtimePay > 0) notes += "Overtime policy added extra pay."

        // Closure day extra pay
        var closureExtraPay = 0.0
        if (worker.closureExtraPayEnabled && records.isNotEmpty()) {
            val applicableClosures = inputs.closures.filter {
                it.date >= inputs.periodStart && it.date <= inputs.periodEnd && it.allowAttendance &&
                    closureAppliesToWorker(it, inputs.assignments)
            }.associateBy { it.date }
            if (applicableClosures.isNotEmpty()) {
                val pct = maxOf(worker.closureExtraPercentage, 0.0) / 100.0
                val method = worker.closureCalculationMethod.ifBlank { "daily_percent" }
                val mwd = if (worker.monthlyWorkingDays > 0) worker.monthlyWorkingDays else 26
                val swh = if (worker.standardWorkingHours > 0) worker.standardWorkingHours else 8
                val dailyBase = when (worker.payType) {
                    PayTypes.DAILY -> worker.dailyRate ?: 0.0
                    PayTypes.MONTHLY -> (worker.monthlySalary ?: 0.0) / mwd
                    PayTypes.HOURLY -> (worker.hourlyRate ?: 0.0) * swh
                    else -> 0.0
                }
                val hourlyBase = dailyBase / swh
                val minuteBase = hourlyBase / 60.0

                for (r in records) {
                    if (r.date !in applicableClosures) continue
                    if (r.status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) continue
                    val dayBonus = when (method) {
                        "daily_percent" -> {
                            val dayPay = if (worker.payType == PayTypes.DAILY) dailyWageForRecord(worker, r).first else dailyBase
                            dayPay * pct
                        }
                        else -> {
                            val recMins = if (r.workedMinutes > 0) r.workedMinutes else swh * 60
                            if (method == "hourly_percent") (recMins / 60.0) * hourlyBase * pct
                            else recMins * minuteBase * pct
                        }
                    }
                    if (dayBonus > 0) closureExtraPay += dayBonus
                }
                if (closureExtraPay > 0) {
                    notes += "Closure day extra pay (${worker.closureExtraPercentage}% via ${method.replace('_', ' ')}) applied."
                }
            }
        }
        basePay += closureExtraPay

        val activeTxns = inputs.transactions.filter {
            it.status == "active" && it.date >= inputs.periodStart && it.date <= inputs.periodEnd
        }.sortedBy { it.date }
        val txnEarnings = activeTxns.filter { it.isEarning }.sumOf { it.amount }
        val txnDeductions = activeTxns.filter { !it.isEarning }.sumOf { it.amount }
        if (txnEarnings > 0) notes += "Bonus/extra payment transactions added to pay."
        if (txnDeductions > 0) notes += "Advance/loan/deduction transactions recovered from pay."

        val (penalty, delayBreakdown) = delayPenalty(
            worker, inputs.assignments, inputs.projectsById, inputs.periodStart, inputs.periodEnd,
        )
        if (penalty > 0) notes += "Project delay penalty applied."

        val deductions = leaveDeductions + lateDeductions + penalty
        val estimated = basePay + overtimePay + txnEarnings - deductions - txnDeductions

        return PaySummary(
            payType = worker.payType,
            basePay = round2(basePay),
            overtimePay = round2(overtimePay),
            leaveDeductions = round2(leaveDeductions),
            lateDeductions = round2(lateDeductions),
            delayPenalty = penalty,
            delayBreakdown = delayBreakdown,
            transactionEarnings = round2(txnEarnings),
            transactionDeductions = round2(txnDeductions),
            transactions = activeTxns,
            leaveBalance = leaveBalance,
            deductions = round2(deductions),
            estimatedPay = round2(estimated),
            paidDays = summary.paidDays,
            fullDayDays = fullDayDays,
            halfDayDays = halfDayDays,
            proratedDays = proratedDays,
            paidAbsentDays = paidAbsentDays,
            workedMinutes = workedMinutes,
            overtimeMinutes = overtimeMinutes,
            overtimeUnits = round2(overtimeUnits),
            lateMinutes = lateMinutes,
            lateUnits = round2(lateUnits),
            lateUnitLabel = lateUnitLabel,
            leaveDays = summary.leaveDays,
            extraLeaveDays = extraLeaveDays,
            closureExtraPay = round2(closureExtraPay),
            policyNotes = notes,
        )
    }

    fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0
}

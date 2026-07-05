package com.example

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClosureDay
import com.example.data.model.ClosureScope
import com.example.data.model.PayTypes
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.domain.PayrollCalculator
import com.example.domain.PayrollCalculator.PayInputs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Verifies the Kotlin payroll engine reproduces the Flask reference numbers.
 * Pure JVM test — no Android dependencies, runs with `./gradlew test`.
 */
class PayrollCalculatorTest {

    private val start = LocalDate.of(2024, 5, 1)
    private val end = LocalDate.of(2024, 5, 31)

    private fun rec(d: LocalDate, status: String, cin: LocalTime?, cout: LocalTime?, ot: Int = 0, late: Int = 0) =
        AttendanceRecord(
            workerId = 1, date = d, status = status,
            checkInTime = cin?.let { LocalDateTime.of(d, it) },
            checkOutTime = cout?.let { LocalDateTime.of(d, it) },
            overtimeMinutes = ot, lateMinutes = late,
        )

    private val dailyWorker = Worker(
        id = 1, workerCode = "MA001", fullName = "Ramesh", phone = "1", position = "Mason",
        department = "Masonry", employeeType = "Daily Wage", joinDate = LocalDate.of(2024, 1, 1),
        payType = PayTypes.DAILY, dailyRate = 800.0, halfDayRate = 400.0,
        startTime = LocalTime.of(9, 0), endTime = LocalTime.of(18, 0),
        overtimeEnabled = true, overtimeRate = 100.0, overtimeType = "hour",
        noWorkNoPay = true, halfDayGraceMinutes = 20,
    )

    @Test
    fun dailyWage_fullHalfProRata() {
        val records = listOf(
            rec(start.plusDays(0), "present", LocalTime.of(9, 0), LocalTime.of(18, 0)),
            rec(start.plusDays(1), "present", LocalTime.of(9, 0), LocalTime.of(13, 30)),
            rec(start.plusDays(2), "present", LocalTime.of(9, 0), LocalTime.of(15, 0)),
            rec(start.plusDays(3), "absent", null, null),
        )
        val pay = PayrollCalculator.paySummary(PayInputs(dailyWorker, records, start, end))
        assertEquals(1733.33, pay.basePay, 0.01)
        assertEquals(1, pay.halfDayDays)
        assertEquals(1, pay.proratedDays)
        assertEquals(1, pay.fullDayDays)
        assertEquals(0, pay.paidAbsentDays)
    }

    @Test
    fun noWorkNoPayDisabled_paysAbsentDays() {
        val records = listOf(
            rec(start, "present", LocalTime.of(9, 0), LocalTime.of(18, 0)),
            rec(start.plusDays(1), "absent", null, null),
        )
        val pay = PayrollCalculator.paySummary(PayInputs(dailyWorker.copy(noWorkNoPay = false), records, start, end))
        assertEquals(1600.0, pay.basePay, 0.01)
        assertEquals(1, pay.paidAbsentDays)
    }

    @Test
    fun overtimePay_hourType() {
        val records = listOf(rec(start, "present", LocalTime.of(9, 0), LocalTime.of(20, 0), ot = 120))
        val pay = PayrollCalculator.paySummary(PayInputs(dailyWorker, records, start, end))
        assertEquals(200.0, pay.overtimePay, 0.01)
    }

    private val monthlyWorker = Worker(
        id = 3, workerCode = "EL001", fullName = "Arun", phone = "3", position = "Electrician",
        department = "Electrical", employeeType = "Full Time", joinDate = LocalDate.of(2024, 1, 1),
        payType = PayTypes.MONTHLY, monthlySalary = 26000.0,
        leavePolicyEnabled = true, leaveDeductionPerDay = 1000.0, allowedLeavesPerMonth = 2,
    )

    @Test
    fun monthlyLeave_withinAccrual_noDeduction() {
        val records = (0 until 4).map { rec(start.plusDays(it.toLong()), "leave", null, null).copy(workerId = 3) }
        val pay = PayrollCalculator.paySummary(PayInputs(monthlyWorker, records, start, end, leaveDaysUsedBefore = 0))
        assertEquals(26000.0, pay.basePay, 0.01)
        assertEquals(0.0, pay.leaveDeductions, 0.01)
        assertNotNull(pay.leaveBalance)
        assertEquals(10.0, pay.leaveBalance!!.availableThisMonth, 0.01)
    }

    @Test
    fun monthlyLeave_excess_chargeable() {
        val records = (0 until 4).map { rec(start.plusDays(it.toLong()), "leave", null, null).copy(workerId = 3) }
        val pay = PayrollCalculator.paySummary(PayInputs(monthlyWorker, records, start, end, leaveDaysUsedBefore = 9))
        assertEquals(2, pay.extraLeaveDays)
        assertEquals(2000.0, pay.leaveDeductions, 0.01)
    }

    @Test
    fun hourlyWage_byWorkedMinutes() {
        val hourly = Worker(
            id = 4, workerCode = "ST001", fullName = "Rohit", phone = "4", position = "Steel Fixer",
            department = "Steel", employeeType = "Daily Wage", joinDate = LocalDate.of(2024, 1, 1),
            payType = PayTypes.HOURLY, hourlyRate = 120.0,
            startTime = LocalTime.of(9, 0), endTime = LocalTime.of(18, 0),
        )
        val records = listOf(
            rec(start, "present", LocalTime.of(9, 0), LocalTime.of(17, 0)).copy(workerId = 4),
            rec(start.plusDays(1), "present", LocalTime.of(9, 0), LocalTime.of(13, 0)).copy(workerId = 4),
        )
        val pay = PayrollCalculator.paySummary(PayInputs(hourly, records, start, end))
        assertEquals(1440.0, pay.basePay, 0.01)
    }

    @Test
    fun transactions_earningsAddDeductionsRecover_cancelledIgnored() {
        val records = listOf(rec(start, "present", LocalTime.of(9, 0), LocalTime.of(18, 0)))
        val txns = listOf(
            WorkerTransaction(workerId = 1, txnType = "bonus", amount = 500.0, date = start.plusDays(1)),
            WorkerTransaction(workerId = 1, txnType = "advance", amount = 300.0, date = start.plusDays(2)),
            WorkerTransaction(workerId = 1, txnType = "loan", amount = 100.0, date = start.plusDays(2), status = "cancelled"),
        )
        val pay = PayrollCalculator.paySummary(PayInputs(dailyWorker, records, start, end, transactions = txns))
        assertEquals(500.0, pay.transactionEarnings, 0.01)
        assertEquals(300.0, pay.transactionDeductions, 0.01)
        assertEquals(800.0 + 500.0 - 300.0, pay.estimatedPay, 0.01)
    }

    @Test
    fun projectDelayPenalty_fixed() {
        val projWorker = Worker(
            id = 5, workerCode = "CO001", fullName = "Vijay", phone = "5", position = "Carpenter",
            department = "Carpentry", employeeType = "Contract", joinDate = LocalDate.of(2024, 1, 1),
            payType = PayTypes.PROJECT, projectRate = 50000.0,
        )
        val project = Project(id = 10, name = "Tower A", deadline = LocalDate.of(2024, 5, 10), penaltyType = "fixed", penaltyValue = 500.0)
        val assignment = ProjectAssignment(id = 1, workerId = 5, projectId = 10, startDate = LocalDate.of(2024, 4, 1))
        val pay = PayrollCalculator.paySummary(
            PayInputs(projWorker, emptyList(), start, end, assignments = listOf(assignment), projectsById = mapOf(10L to project))
        )
        assertEquals(50000.0, pay.basePay, 0.01)
        assertEquals(10500.0, pay.delayPenalty, 0.01)
    }

    @Test
    fun lateDetection_respectsGraceWindow() {
        val w = dailyWorker.copy(lateGraceMinutes = 10)
        assertFalse(PayrollCalculator.isLateCheckIn(w, start, LocalDateTime.of(start, LocalTime.of(9, 8))))
        assertTrue(PayrollCalculator.isLateCheckIn(w, start, LocalDateTime.of(start, LocalTime.of(9, 15))))
    }

    @Test
    fun closureScope_companyHitsAll_siteNeedsAssignment() {
        val company = ClosureDay(id = 1, date = start, reason = "Holi", scope = ClosureScope.COMPANY)
        assertTrue(PayrollCalculator.closureAppliesToWorker(company, emptyList()))
        val site = ClosureDay(id = 2, date = start, reason = "Shut", scope = ClosureScope.SITE, siteId = 99)
        val assignment = ProjectAssignment(id = 1, workerId = 5, projectId = 10, startDate = LocalDate.of(2024, 4, 1))
        assertFalse(PayrollCalculator.closureAppliesToWorker(site, listOf(assignment)))
    }
}

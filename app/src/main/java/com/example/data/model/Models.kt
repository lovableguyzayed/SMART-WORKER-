package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ─────────────────────────────────────────────────────────────────────────────
//  Domain constants (ported from the Flask reference implementation)
// ─────────────────────────────────────────────────────────────────────────────
object Roles {
    const val ADMIN = "admin"
    const val MANAGER = "manager"
    const val ATTENDANCE = "attendance"
}

object PayTypes {
    const val DAILY = "daily"
    const val MONTHLY = "monthly"
    const val HOURLY = "hourly"
    const val PROJECT = "project"
    val ALL = listOf(DAILY, MONTHLY, HOURLY, PROJECT)
}

object AttendanceStatus {
    const val PRESENT = "present"
    const val ABSENT = "absent"
    const val LATE = "late"
    const val LEAVE = "leave"
    val ALL = listOf(PRESENT, ABSENT, LATE, LEAVE)
}

object TxnTypes {
    val EARNINGS = listOf("bonus", "extra_payment", "incentive", "refreshment")
    val DEDUCTIONS = listOf("advance", "loan", "cash_advance", "recovery", "deduction")
    val LABELS = mapOf(
        "bonus" to "Bonus",
        "extra_payment" to "Extra Payment",
        "incentive" to "Incentive",
        "refreshment" to "Refreshment",
        "advance" to "Advance",
        "loan" to "Loan",
        "cash_advance" to "Cash Advance",
        "recovery" to "Recovery",
        "deduction" to "Deduction",
    )

    fun isEarning(type: String) = type in EARNINGS
}

object LeaveTypes {
    val ALL = listOf("casual", "sick", "paid", "unpaid", "other")
}

object ClosureScope {
    const val COMPANY = "company"
    const val SITE = "site"
    const val PROJECT = "project"
}

// ─────────────────────────────────────────────────────────────────────────────
//  Entities
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String = "",
    val phone: String? = null,
    val passwordHash: String,
    val fullName: String,
    val role: String = Roles.ATTENDANCE,
    /** Comma-separated site ids an attendance user may see. Empty = all. */
    val assignedSiteIds: String = "",
    /** Comma-separated project ids an attendance user may see. Empty = all. */
    val assignedProjectIds: String = "",
    val status: String = "active",
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val isAdmin: Boolean get() = role == Roles.ADMIN || role == Roles.MANAGER
    val siteIdList: List<Long>
        get() = assignedSiteIds.split(',').mapNotNull { it.trim().toLongOrNull() }
    val projectIdList: List<Long>
        get() = assignedProjectIds.split(',').mapNotNull { it.trim().toLongOrNull() }
}

@Entity(tableName = "company_settings")
data class CompanySetting(
    @PrimaryKey
    val id: Long = 1,
    val name: String = "SmartWorker",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val gstNumber: String = "",
    val registrationNumber: String = "",
    /** Absolute path of the company logo inside app storage (optional). */
    val logo: String? = null,
)

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String = "",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val status: String = "active", // active, archived
)

@Entity(tableName = "departments")
data class Department(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val status: String = "active",
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val siteId: Long? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val deadline: LocalDate? = null,
    val completionDate: LocalDate? = null,
    val penaltyType: String = "none", // none, fixed, percent
    val penaltyValue: Double = 0.0,
    val status: String = "active", // active, completed, archived
) {
    fun delayDays(today: LocalDate = LocalDate.now()): Long {
        val dl = deadline ?: return 0
        val reference = completionDate ?: today
        return maxOf(java.time.temporal.ChronoUnit.DAYS.between(dl, reference), 0)
    }
}

@Entity(tableName = "work_tasks")
data class WorkTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val projectId: Long? = null,
    val category: String = "",
    val status: String = "active",
)

@Entity(tableName = "project_assignments", indices = [Index("workerId")])
data class ProjectAssignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val projectId: Long? = null,
    val siteId: Long? = null,
    val taskId: Long? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String = "active", // active, completed, transferred
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun activeOn(onDate: LocalDate): Boolean {
        if (startDate > onDate) return false
        endDate?.let { if (it < onDate) return false }
        if (endDate == null && status != "active") return startDate == onDate
        return true
    }
}

@Entity(tableName = "worker_modifications", indices = [Index("workerId")])
data class WorkerModification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val modType: String, // promotion, salary_change, category_change, leave_grant, profile_edit, other
    val fieldName: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val description: String? = null,
    val effectiveDate: LocalDate? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity(tableName = "leave_adjustments", indices = [Index("workerId")])
data class LeaveAdjustment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    /** positive = credit, negative = debit */
    val days: Double,
    val reason: String = "",
    val effectiveDate: LocalDate? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity(tableName = "worker_transactions", indices = [Index("workerId"), Index("date")])
data class WorkerTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val txnType: String,
    val amount: Double,
    val date: LocalDate,
    val description: String = "",
    val status: String = "active", // active, cancelled
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val isEarning: Boolean get() = TxnTypes.isEarning(txnType)
}

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String = "",
    val category: String = "attendance", // attendance, system, sync
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity(tableName = "workers", indices = [Index(value = ["workerCode"], unique = true)])
data class Worker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Human-facing unique id, e.g. MA001 (department prefix + sequence). */
    val workerCode: String,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val position: String,
    val department: String,
    val employeeType: String, // Daily Wage, Full Time, Part Time, Contract
    val joinDate: LocalDate,

    // Payment settings
    val payType: String, // daily, monthly, hourly, project
    val dailyRate: Double? = null,
    val monthlySalary: Double? = null,
    val hourlyRate: Double? = null,
    val projectRate: Double? = null,

    // Working hours (shift)
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val breakDuration: Int = 60,

    // Overtime policy
    val overtimeEnabled: Boolean = false,
    val overtimeRate: Double? = null,
    val overtimeType: String = "hour", // hour, minute

    // Late policy
    val latePolicyEnabled: Boolean = false,
    val lateDeductionPerUnit: Double? = null,
    val lateDeductionType: String = "day", // day, hour, minute
    val lateGraceMinutes: Int = 10,

    // Daily wage absence / half-day policy
    val noWorkNoPay: Boolean = true,
    val halfDayRate: Double? = null,
    val halfDayGraceMinutes: Int = 20,

    // Monthly derivation helpers
    val monthlyWorkingDays: Int = 26,
    val standardWorkingHours: Int = 8,

    // Closure day extra pay
    val closureExtraPayEnabled: Boolean = false,
    val closureCalculationMethod: String = "daily_percent", // daily_percent, hourly_percent, minute_percent
    val closureExtraPercentage: Double = 0.0,

    // Leave policy (salaried)
    val allowedLeavesPerMonth: Int = 2,
    val leaveDeductionPerDay: Double? = null,
    val leavePolicyEnabled: Boolean = true,

    val status: String = "active", // active, inactive
    /** Absolute path of the worker's photo inside app storage (optional). */
    val profileImage: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["workerId", "date"], unique = true), Index("date")],
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val date: LocalDate,
    val checkInTime: LocalDateTime? = null,
    val checkOutTime: LocalDateTime? = null,
    val status: String, // present, absent, late, leave
    val overtimeMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val leaveType: String? = null,
    val siteId: Long? = null,
    val markedBy: Long? = null,
    val markedVia: String = "manual", // manual, worker_id, bulk
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val workedMinutes: Int
        get() {
            val cin = checkInTime ?: return 0
            val cout = checkOutTime ?: return 0
            if (cout <= cin) return 0
            return (java.time.Duration.between(cin, cout).seconds / 60).toInt()
        }
}

@Entity(
    tableName = "closure_days",
    indices = [Index("date")],
)
data class ClosureDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val reason: String,
    val type: String = "holiday", // holiday, site, project, emergency, maintenance
    val scope: String = ClosureScope.COMPANY, // company, site, project
    val siteId: Long? = null,
    val projectId: Long? = null,
    val allowAttendance: Boolean = true,
)

@Entity(
    tableName = "payroll_records",
    indices = [Index(value = ["workerId", "month", "year"], unique = true)],
)
data class PayrollRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val month: Int,
    val year: Int,
    val totalDays: Int,
    val presentDays: Int,
    val overtimeHours: Double = 0.0,
    val grossPay: Double,
    val deductions: Double = 0.0,
    val netPay: Double,
    val status: String = "pending", // pending, paid
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

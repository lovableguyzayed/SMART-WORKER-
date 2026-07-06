package com.example.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.model.ClosureDay
import com.example.data.model.CompanySetting
import com.example.data.model.Department
import com.example.data.model.LeaveAdjustment
import com.example.data.model.PayTypes
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Site
import com.example.data.model.User
import com.example.data.model.WorkTask
import com.example.data.model.Worker
import com.example.data.model.WorkerModification
import com.example.data.repo.PayrollRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val EMPLOYEE_TYPES = listOf("Daily Wage", "Full Time", "Part Time", "Contract")

// ─────────────────────────────────────────────────────────────────────────────
//  Worker add/edit form — full port of the Flask add_worker/edit_worker policy
//  configuration for every pay type.
// ─────────────────────────────────────────────────────────────────────────────
class WorkerFormViewModel(private val c: AppContainer) : ViewModel() {

    data class FormState(
        val editingId: Long? = null,
        val fullName: String = "",
        val phone: String = "",
        val email: String = "",
        val address: String = "",
        val position: String = "",
        val department: String = "",
        val employeeType: String = EMPLOYEE_TYPES.first(),
        val joinDate: LocalDate = LocalDate.now(),
        val payType: String = PayTypes.DAILY,
        val dailyRate: String = "",
        val monthlySalary: String = "",
        val hourlyRate: String = "",
        val projectRate: String = "",
        val startTime: String = "09:00",
        val endTime: String = "18:00",
        val overtimeEnabled: Boolean = false,
        val overtimeRate: String = "",
        val overtimeType: String = "hour",
        val latePolicyEnabled: Boolean = false,
        val lateDeduction: String = "",
        val lateDeductionType: String = "day",
        val lateGraceMinutes: String = "10",
        val noWorkNoPay: Boolean = true,
        val halfDayRate: String = "",
        val halfDayGraceMinutes: String = "20",
        val monthlyWorkingDays: String = "26",
        val standardWorkingHours: String = "8",
        val allowedLeaves: String = "2",
        val leaveDeduction: String = "",
        val leavePolicyEnabled: Boolean = true,
        val closureExtraPayEnabled: Boolean = false,
        val closureCalculationMethod: String = "daily_percent",
        val closureExtraPercentage: String = "0",
        val status: String = "active",
        val profileImage: String? = null,
    )

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val departments: StateFlow<List<Department>> =
        c.catalogRepository.activeDepartments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun update(transform: (FormState) -> FormState) { _form.value = transform(_form.value) }
    fun clearError() { _error.value = null }

    /** Pre-fill the form from an existing worker (edit mode). */
    fun load(workerId: Long) {
        viewModelScope.launch {
            val w = c.workerRepository.workerOnce(workerId) ?: return@launch
            val t = DateTimeFormatter.ofPattern("HH:mm")
            _form.value = FormState(
                editingId = w.id,
                fullName = w.fullName, phone = w.phone, email = w.email, address = w.address,
                position = w.position, department = w.department, employeeType = w.employeeType,
                joinDate = w.joinDate, payType = w.payType,
                dailyRate = w.dailyRate?.fmt() ?: "", monthlySalary = w.monthlySalary?.fmt() ?: "",
                hourlyRate = w.hourlyRate?.fmt() ?: "", projectRate = w.projectRate?.fmt() ?: "",
                startTime = w.startTime?.format(t) ?: "", endTime = w.endTime?.format(t) ?: "",
                overtimeEnabled = w.overtimeEnabled, overtimeRate = w.overtimeRate?.fmt() ?: "",
                overtimeType = w.overtimeType,
                latePolicyEnabled = w.latePolicyEnabled, lateDeduction = w.lateDeductionPerUnit?.fmt() ?: "",
                lateDeductionType = w.lateDeductionType, lateGraceMinutes = w.lateGraceMinutes.toString(),
                noWorkNoPay = w.noWorkNoPay, halfDayRate = w.halfDayRate?.fmt() ?: "",
                halfDayGraceMinutes = w.halfDayGraceMinutes.toString(),
                monthlyWorkingDays = w.monthlyWorkingDays.toString(),
                standardWorkingHours = w.standardWorkingHours.toString(),
                allowedLeaves = w.allowedLeavesPerMonth.toString(),
                leaveDeduction = w.leaveDeductionPerDay?.fmt() ?: "",
                leavePolicyEnabled = w.leavePolicyEnabled,
                closureExtraPayEnabled = w.closureExtraPayEnabled,
                closureCalculationMethod = w.closureCalculationMethod,
                closureExtraPercentage = w.closureExtraPercentage.fmt(),
                status = w.status,
                profileImage = w.profileImage,
            )
        }
    }

    fun save(onSaved: (String) -> Unit) {
        val f = _form.value
        val problem = validate(f)
        if (problem != null) { _error.value = problem; return }

        viewModelScope.launch {
            val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
            fun parseTime(v: String): LocalTime? = try {
                if (v.isBlank()) null else LocalTime.parse(v.trim(), timeFmt)
            } catch (_: Exception) { null }

            val usesShift = f.payType == PayTypes.DAILY || f.payType == PayTypes.MONTHLY
            val base = Worker(
                id = f.editingId ?: 0,
                workerCode = "", // set below for inserts, preserved for edits
                fullName = f.fullName.trim(), phone = f.phone.trim(), email = f.email.trim(),
                address = f.address.trim(), position = f.position.trim(), department = f.department,
                employeeType = f.employeeType, joinDate = f.joinDate, payType = f.payType,
                dailyRate = if (f.payType == PayTypes.DAILY) f.dailyRate.num() else null,
                monthlySalary = if (f.payType == PayTypes.MONTHLY) f.monthlySalary.num() else null,
                hourlyRate = if (f.payType == PayTypes.HOURLY) f.hourlyRate.num() else null,
                projectRate = if (f.payType == PayTypes.PROJECT) f.projectRate.num() else null,
                startTime = if (usesShift) parseTime(f.startTime) else null,
                endTime = if (usesShift) parseTime(f.endTime) else null,
                overtimeEnabled = usesShift && f.overtimeEnabled,
                overtimeRate = if (usesShift && f.overtimeEnabled) f.overtimeRate.num() else null,
                overtimeType = if (f.overtimeType in listOf("hour", "minute")) f.overtimeType else "hour",
                latePolicyEnabled = usesShift && f.latePolicyEnabled,
                lateDeductionPerUnit = if (usesShift && f.latePolicyEnabled) f.lateDeduction.num() else null,
                lateDeductionType = if (f.lateDeductionType in listOf("day", "hour", "minute")) f.lateDeductionType else "day",
                lateGraceMinutes = f.lateGraceMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 10,
                noWorkNoPay = f.noWorkNoPay,
                halfDayRate = if (f.payType == PayTypes.DAILY) {
                    f.halfDayRate.num() ?: f.dailyRate.num()?.div(2.0)
                } else f.halfDayRate.num(),
                halfDayGraceMinutes = (f.halfDayGraceMinutes.toIntOrNull() ?: 20).coerceIn(15, 25),
                monthlyWorkingDays = f.monthlyWorkingDays.toIntOrNull()?.takeIf { it > 0 } ?: 26,
                standardWorkingHours = f.standardWorkingHours.toIntOrNull()?.takeIf { it > 0 } ?: 8,
                allowedLeavesPerMonth = f.allowedLeaves.toIntOrNull()?.coerceAtLeast(0) ?: 2,
                leaveDeductionPerDay = f.leaveDeduction.num(),
                leavePolicyEnabled = f.payType == PayTypes.MONTHLY && f.leavePolicyEnabled,
                closureExtraPayEnabled = f.payType == PayTypes.MONTHLY && f.closureExtraPayEnabled,
                closureCalculationMethod = if (f.closureCalculationMethod in
                    listOf("daily_percent", "hourly_percent", "minute_percent")
                ) f.closureCalculationMethod else "daily_percent",
                closureExtraPercentage = f.closureExtraPercentage.num()?.coerceAtLeast(0.0) ?: 0.0,
                status = f.status,
                profileImage = f.profileImage,
            )

            if (f.editingId == null) {
                val code = c.workerRepository.generateWorkerCode(f.department)
                c.workerRepository.addWorker(base.copy(workerCode = code))
                onSaved("Worker ${f.fullName.trim()} added with ID $code.")
            } else {
                val before = c.workerRepository.workerOnce(f.editingId) ?: return@launch
                c.workerRepository.updateWorker(
                    before,
                    base.copy(workerCode = before.workerCode, createdAt = before.createdAt),
                )
                onSaved("Worker ${f.fullName.trim()} updated.")
            }
        }
    }

    private fun validate(f: FormState): String? = when {
        f.fullName.isBlank() -> "Full name is required."
        f.phone.isBlank() -> "Phone number is required."
        f.position.isBlank() -> "Designation is required."
        f.department.isBlank() -> "Please choose a department."
        f.payType == PayTypes.DAILY && (f.dailyRate.num() ?: 0.0) <= 0 -> "Daily rate must be greater than zero."
        f.payType == PayTypes.MONTHLY && (f.monthlySalary.num() ?: 0.0) <= 0 -> "Monthly salary must be greater than zero."
        f.payType == PayTypes.HOURLY && (f.hourlyRate.num() ?: 0.0) <= 0 -> "Hourly rate must be greater than zero."
        f.payType == PayTypes.PROJECT && (f.projectRate.num() ?: 0.0) <= 0 -> "Project rate must be greater than zero."
        else -> null
    }

    private fun String.num(): Double? = trim().toDoubleOrNull()
    private fun Double.fmt(): String = if (this == toLong().toDouble()) toLong().toString() else toString()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Worker admin actions: assignments, promotion, leave adjustment, history.
// ─────────────────────────────────────────────────────────────────────────────
class WorkerAdminViewModel(private val c: AppContainer) : ViewModel() {

    private val workerId = MutableStateFlow(0L)
    fun setWorker(id: Long) { workerId.value = id }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val assignments: StateFlow<List<ProjectAssignment>> = workerId.flatMapLatest { id ->
        if (id > 0) c.workerRepository.assignmentsFor(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val modifications: StateFlow<List<WorkerModification>> = workerId.flatMapLatest { id ->
        if (id > 0) c.db.modificationDao().forWorker(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val leaveAdjustments: StateFlow<List<LeaveAdjustment>> = workerId.flatMapLatest { id ->
        if (id > 0) c.db.leaveAdjustmentDao().forWorker(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sites: StateFlow<List<Site>> =
        c.catalogRepository.activeSites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val projects: StateFlow<List<Project>> =
        c.catalogRepository.activeProjects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tasks: StateFlow<List<WorkTask>> =
        c.catalogRepository.activeTasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun assign(projectId: Long?, siteId: Long?, taskId: Long?, startDate: LocalDate, notes: String, onDone: (String) -> Unit) {
        val id = workerId.value
        if (id <= 0) return
        if (projectId == null && siteId == null) { onDone("Choose a project or a site for the assignment."); return }
        viewModelScope.launch {
            c.workerRepository.assign(id, projectId, siteId, taskId, startDate, notes)
            onDone("Assignment saved. Previous active assignment was closed automatically.")
        }
    }

    fun endAssignment(assignmentId: Long, onDone: (String) -> Unit) {
        viewModelScope.launch {
            c.workerRepository.endAssignment(assignmentId)
            onDone("Assignment ended.")
        }
    }

    fun promote(newPosition: String?, newRate: Double?, description: String?, onDone: (String) -> Unit) {
        val id = workerId.value
        if (id <= 0) return
        if (newPosition.isNullOrBlank() && (newRate == null || newRate <= 0)) {
            onDone("Enter a new designation and/or a new pay rate."); return
        }
        viewModelScope.launch {
            c.workerRepository.promote(id, newPosition, newRate, description, LocalDate.now())
            onDone("Promotion recorded.")
        }
    }

    fun adjustLeave(days: Double, reason: String, onDone: (String) -> Unit) {
        val id = workerId.value
        if (id <= 0) return
        if (days == 0.0) { onDone("Days must not be zero — use positive to credit, negative to debit."); return }
        viewModelScope.launch {
            c.workerRepository.adjustLeave(id, days, reason, LocalDate.now())
            onDone(if (days > 0) "Credited $days leave day(s)." else "Debited ${-days} leave day(s).")
        }
    }

    fun toggleActive(onDone: (String?) -> Unit) {
        val id = workerId.value
        if (id <= 0) return
        viewModelScope.launch { onDone(c.workerRepository.toggleActive(id)) }
    }

    // ── Worker report (6-month history, Flask worker_report port) ──────────
    data class MonthRow(val label: String, val row: PayrollRepository.PayrollRow)
    data class ReportState(val months: List<MonthRow> = emptyList(), val loading: Boolean = false)

    private val _report = MutableStateFlow(ReportState())
    val report: StateFlow<ReportState> = _report.asStateFlow()

    fun buildReport() {
        val id = workerId.value
        if (id <= 0) return
        viewModelScope.launch {
            _report.value = ReportState(loading = true)
            val worker = c.workerRepository.workerOnce(id) ?: run {
                _report.value = ReportState(); return@launch
            }
            val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")
            val months = mutableListOf<MonthRow>()
            var ym = YearMonth.now()
            repeat(6) {
                if (!ym.atEndOfMonth().isBefore(worker.joinDate)) {
                    months += MonthRow(ym.format(fmt), c.payrollRepository.slip(worker, ym.monthValue, ym.year))
                }
                ym = ym.minusMonths(1)
            }
            _report.value = ReportState(months = months, loading = false)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Settings: company, sites/projects/tasks/departments CRUD, closures, users.
// ─────────────────────────────────────────────────────────────────────────────
class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val company: StateFlow<CompanySetting?> =
        c.catalogRepository.company.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sites: StateFlow<List<Site>> =
        c.catalogRepository.sites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val projects: StateFlow<List<Project>> =
        c.catalogRepository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks: StateFlow<List<WorkTask>> =
        c.catalogRepository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val departments: StateFlow<List<Department>> =
        c.catalogRepository.departments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val closures: StateFlow<List<ClosureDay>> =
        c.catalogRepository.upcomingClosures().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendanceUsers: StateFlow<List<User>> =
        c.catalogRepository.attendanceUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }
    private fun say(m: String) { _message.value = m }

    fun saveCompany(setting: CompanySetting) {
        viewModelScope.launch {
            c.catalogRepository.saveCompany(setting.copy(id = 1))
            say("Company details saved.")
        }
    }

    fun saveSite(site: Site) = viewModelScope.launch {
        if (site.name.isBlank()) { say("Site name is required."); return@launch }
        c.catalogRepository.saveSite(site); say("Site saved.")
    }
    fun archiveSite(id: Long) = viewModelScope.launch { c.catalogRepository.archiveSite(id); say("Site status updated.") }
    fun deleteSite(id: Long) = viewModelScope.launch { say(c.catalogRepository.deleteSite(id) ?: "Site deleted.") }

    fun saveProject(project: Project) = viewModelScope.launch {
        if (project.name.isBlank()) { say("Project name is required."); return@launch }
        c.catalogRepository.saveProject(project); say("Project saved.")
    }
    fun archiveProject(id: Long) = viewModelScope.launch { c.catalogRepository.archiveProject(id); say("Project status updated.") }
    fun deleteProject(id: Long) = viewModelScope.launch { say(c.catalogRepository.deleteProject(id) ?: "Project deleted.") }

    fun saveTask(task: WorkTask) = viewModelScope.launch {
        if (task.name.isBlank()) { say("Task name is required."); return@launch }
        c.catalogRepository.saveTask(task); say("Task saved.")
    }
    fun deleteTask(task: WorkTask) = viewModelScope.launch { say(c.catalogRepository.deleteTask(task) ?: "Task deleted.") }

    fun saveDepartment(dept: Department) = viewModelScope.launch {
        if (dept.name.isBlank()) { say("Department name is required."); return@launch }
        c.catalogRepository.saveDepartment(dept); say("Department saved.")
    }
    fun deleteDepartment(dept: Department) = viewModelScope.launch {
        say(c.catalogRepository.deleteDepartment(dept) ?: "Department deleted.")
    }

    fun saveClosure(
        date: LocalDate, reason: String, type: String, scope: String,
        siteId: Long?, projectId: Long?, allowAttendance: Boolean,
    ) = viewModelScope.launch {
        if (reason.isBlank()) { say("Closure reason is required."); return@launch }
        say(c.catalogRepository.saveClosure(date, reason, type, scope, siteId, projectId, allowAttendance))
    }
    fun deleteClosure(closure: ClosureDay) = viewModelScope.launch {
        c.catalogRepository.deleteClosure(closure); say("Closure removed.")
    }

    fun saveAttendanceUser(
        existingId: Long?, username: String, fullName: String, phone: String?,
        password: String?, siteIds: List<Long>, projectIds: List<Long>,
    ) = viewModelScope.launch {
        val err = c.authRepository.saveAttendanceUser(existingId, username, fullName, phone, password, siteIds, projectIds)
        say(err ?: "Attendance user saved.")
    }
    fun toggleUser(id: Long) = viewModelScope.launch { c.authRepository.toggleUserStatus(id); say("User status updated.") }
    fun deleteUser(id: Long) = viewModelScope.launch { c.authRepository.deleteUser(id); say("User deleted.") }
}

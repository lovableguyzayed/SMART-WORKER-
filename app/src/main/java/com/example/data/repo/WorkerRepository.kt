package com.example.data.repo

import com.example.data.db.AppDatabase
import com.example.data.model.LeaveAdjustment
import com.example.data.model.ProjectAssignment
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.data.model.WorkerModification
import java.time.LocalDate

class WorkerRepository(private val db: AppDatabase) {

    val activeWorkers = db.workerDao().active()
    val allWorkers = db.workerDao().all()

    fun worker(id: Long) = db.workerDao().byIdFlow(id)

    suspend fun workerOnce(id: Long) = db.workerDao().byId(id)

    suspend fun workerByCode(code: String): Worker? {
        val cleaned = code.trim().removePrefix("SMARTWORKER:").trim()
        if (cleaned.isEmpty()) return null
        return db.workerDao().byCode(cleaned)
    }

    /** Department-prefixed sequential id, e.g. "MA001" for Masonry. */
    suspend fun generateWorkerCode(department: String): String {
        val prefix = department.trim().take(2).uppercase().ifBlank { "WK" }
        val last = db.workerDao().lastCodeWithPrefix(prefix)
        val next = last?.drop(2)?.toIntOrNull()?.plus(1) ?: 1
        return "%s%03d".format(prefix, next)
    }

    suspend fun addWorker(worker: Worker): Long = db.workerDao().insert(worker)

    private val trackedFields: List<Pair<String, (Worker) -> String?>> = listOf(
        "Designation" to { w -> w.position },
        "Department" to { w -> w.department },
        "Worker Category" to { w -> w.employeeType },
        "Pay Type" to { w -> w.payType },
        "Daily Rate" to { w -> w.dailyRate?.toString() },
        "Monthly Salary" to { w -> w.monthlySalary?.toString() },
        "Hourly Rate" to { w -> w.hourlyRate?.toString() },
        "Project Rate" to { w -> w.projectRate?.toString() },
        "Allowed Leaves / Month" to { w -> w.allowedLeavesPerMonth.toString() },
    )

    /** Update a worker, writing a permanent audit row for each tracked change. */
    suspend fun updateWorker(before: Worker, after: Worker) {
        db.workerDao().update(after.copy(updatedAt = java.time.LocalDateTime.now()))
        for ((label, extract) in trackedFields) {
            val old = extract(before)
            val new = extract(after)
            if (old != new) {
                db.modificationDao().insert(
                    WorkerModification(
                        workerId = after.id,
                        modType = "profile_edit",
                        fieldName = label,
                        oldValue = old,
                        newValue = new,
                        effectiveDate = LocalDate.now(),
                    )
                )
            }
        }
    }

    suspend fun toggleActive(workerId: Long): String? {
        val worker = db.workerDao().byId(workerId) ?: return null
        val newStatus = if (worker.status == "active") "inactive" else "active"
        db.workerDao().update(worker.copy(status = newStatus))
        return if (newStatus == "inactive") "${worker.fullName} has been deactivated."
        else "${worker.fullName} has been reactivated."
    }

    suspend fun promote(
        workerId: Long,
        newPosition: String?,
        newSalaryOrRate: Double?,
        description: String?,
        effectiveDate: LocalDate,
    ) {
        val worker = db.workerDao().byId(workerId) ?: return
        var updated = worker
        if (!newPosition.isNullOrBlank() && newPosition != worker.position) {
            db.modificationDao().insert(
                WorkerModification(
                    workerId = workerId, modType = "promotion", fieldName = "Designation",
                    oldValue = worker.position, newValue = newPosition,
                    description = description, effectiveDate = effectiveDate,
                )
            )
            updated = updated.copy(position = newPosition)
        }
        if (newSalaryOrRate != null && newSalaryOrRate > 0) {
            val (old, applied) = when (worker.payType) {
                "monthly" -> worker.monthlySalary to updated.copy(monthlySalary = newSalaryOrRate)
                "daily" -> worker.dailyRate to updated.copy(dailyRate = newSalaryOrRate)
                "hourly" -> worker.hourlyRate to updated.copy(hourlyRate = newSalaryOrRate)
                else -> worker.projectRate to updated.copy(projectRate = newSalaryOrRate)
            }
            db.modificationDao().insert(
                WorkerModification(
                    workerId = workerId, modType = "salary_change", fieldName = "Pay Rate",
                    oldValue = old?.toString(), newValue = newSalaryOrRate.toString(),
                    description = description, effectiveDate = effectiveDate,
                )
            )
            updated = applied
        }
        if (updated != worker) db.workerDao().update(updated.copy(updatedAt = java.time.LocalDateTime.now()))
    }

    suspend fun adjustLeave(workerId: Long, days: Double, reason: String, effectiveDate: LocalDate?) {
        db.leaveAdjustmentDao().insert(
            LeaveAdjustment(workerId = workerId, days = days, reason = reason, effectiveDate = effectiveDate)
        )
        db.modificationDao().insert(
            WorkerModification(
                workerId = workerId, modType = "leave_grant", fieldName = "Leave Balance",
                newValue = if (days >= 0) "+$days day(s)" else "$days day(s)",
                description = reason, effectiveDate = effectiveDate,
            )
        )
    }

    // ── Assignments ─────────────────────────────────────────────────────────

    fun assignmentsFor(workerId: Long) = db.assignmentDao().forWorker(workerId)

    suspend fun assign(
        workerId: Long,
        projectId: Long?,
        siteId: Long?,
        taskId: Long?,
        startDate: LocalDate,
        notes: String,
    ) {
        // End the current active assignment (transfer semantics).
        val current = db.assignmentDao().forWorkerOnce(workerId).firstOrNull { it.status == "active" }
        if (current != null) {
            db.assignmentDao().upsert(
                current.copy(status = "transferred", endDate = startDate.minusDays(1))
            )
        }
        db.assignmentDao().upsert(
            ProjectAssignment(
                workerId = workerId, projectId = projectId, siteId = siteId, taskId = taskId,
                startDate = startDate, notes = notes,
            )
        )
    }

    suspend fun endAssignment(assignmentId: Long, endDate: LocalDate = LocalDate.now()) {
        db.assignmentDao().endAssignment(assignmentId, endDate)
    }

    /** Attendance users only see workers assigned to their sites/projects. */
    suspend fun visibleToUser(worker: Worker, user: User, onDate: LocalDate? = null): Boolean {
        if (user.isAdmin) return true
        val siteIds = user.siteIdList
        val projectIds = user.projectIdList
        if (siteIds.isEmpty() && projectIds.isEmpty()) return true
        val assignments = db.assignmentDao().forWorkerOnce(worker.id)
        val assignment = if (onDate != null) {
            assignments.firstOrNull { it.activeOn(onDate) }
        } else {
            assignments.firstOrNull { it.status == "active" }
        } ?: return false
        if (siteIds.isNotEmpty() && assignment.siteId in siteIds) return true
        if (projectIds.isNotEmpty() && assignment.projectId in projectIds) return true
        return false
    }
}

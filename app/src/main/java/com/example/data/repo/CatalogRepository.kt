package com.example.data.repo

import com.example.data.db.AppDatabase
import com.example.data.model.ClosureDay
import com.example.data.model.ClosureScope
import com.example.data.model.CompanySetting
import com.example.data.model.Department
import com.example.data.model.Notification
import com.example.data.model.Project
import com.example.data.model.Site
import com.example.data.model.WorkTask
import com.example.data.model.WorkerTransaction
import java.time.LocalDate

/** Sites, departments, projects, tasks, closures, transactions, notifications, company. */
class CatalogRepository(private val db: AppDatabase) {

    // ── Sites ─────────────────────────────────────────────────────────────
    val sites = db.siteDao().all()
    val activeSites = db.siteDao().active()
    suspend fun saveSite(site: Site) = db.siteDao().upsert(site)
    suspend fun archiveSite(id: Long) {
        db.siteDao().byId(id)?.let { db.siteDao().upsert(it.copy(status = if (it.status == "active") "archived" else "active")) }
    }
    suspend fun deleteSite(id: Long): String? {
        if (db.siteDao().referenceCount(id) > 0) return "This site is referenced by assignments and cannot be deleted. Archive it instead."
        db.siteDao().byId(id)?.let { db.siteDao().delete(it) }
        return null
    }

    // ── Departments ───────────────────────────────────────────────────────
    val departments = db.departmentDao().all()
    val activeDepartments = db.departmentDao().active()
    suspend fun saveDepartment(dept: Department) = db.departmentDao().upsert(dept)
    suspend fun deleteDepartment(dept: Department): String? {
        if (db.departmentDao().referenceCount(dept.name) > 0) return "Workers still belong to this department. Reassign them first."
        db.departmentDao().delete(dept)
        return null
    }

    // ── Projects ──────────────────────────────────────────────────────────
    val projects = db.projectDao().all()
    val activeProjects = db.projectDao().activeOrCompleted()
    suspend fun saveProject(project: Project) = db.projectDao().upsert(project)
    suspend fun archiveProject(id: Long) {
        db.projectDao().byId(id)?.let { db.projectDao().upsert(it.copy(status = if (it.status == "archived") "active" else "archived")) }
    }
    suspend fun deleteProject(id: Long): String? {
        if (db.projectDao().referenceCount(id) > 0) return "This project has assignments and cannot be deleted. Archive it instead."
        db.projectDao().byId(id)?.let { db.projectDao().delete(it) }
        return null
    }

    // ── Tasks ─────────────────────────────────────────────────────────────
    val tasks = db.workTaskDao().all()
    val activeTasks = db.workTaskDao().activeTasks()
    suspend fun saveTask(task: WorkTask) = db.workTaskDao().upsert(task)
    suspend fun deleteTask(task: WorkTask): String? {
        if (db.workTaskDao().referenceCount(task.id) > 0) return "This task is referenced by assignments. Archive it instead."
        db.workTaskDao().delete(task)
        return null
    }

    // ── Closures ──────────────────────────────────────────────────────────
    fun upcomingClosures(from: LocalDate = LocalDate.now()) = db.closureDao().upcoming(from)

    suspend fun saveClosure(
        date: LocalDate,
        reason: String,
        type: String,
        scope: String,
        siteId: Long?,
        projectId: Long?,
        allowAttendance: Boolean,
    ): String {
        var normScope = if (scope in listOf(ClosureScope.COMPANY, ClosureScope.SITE, ClosureScope.PROJECT)) scope else ClosureScope.COMPANY
        var normSite = siteId
        var normProject = projectId
        if (normScope == ClosureScope.SITE && normSite == null) normScope = ClosureScope.COMPANY
        if (normScope == ClosureScope.PROJECT && normProject == null) normScope = ClosureScope.COMPANY
        if (normScope != ClosureScope.SITE) normSite = null
        if (normScope != ClosureScope.PROJECT) normProject = null

        val existing = db.closureDao().find(date, normScope, normSite, normProject)
        return if (existing != null) {
            db.closureDao().upsert(existing.copy(reason = reason, type = type, allowAttendance = allowAttendance))
            "Closure day updated successfully."
        } else {
            db.closureDao().upsert(
                ClosureDay(
                    date = date, reason = reason, type = type, scope = normScope,
                    siteId = normSite, projectId = normProject, allowAttendance = allowAttendance,
                )
            )
            "Closure day added successfully."
        }
    }

    suspend fun deleteClosure(closure: ClosureDay) = db.closureDao().delete(closure)

    // ── Transactions ──────────────────────────────────────────────────────
    fun transactionsBetween(start: LocalDate, end: LocalDate) = db.transactionDao().betweenDates(start, end)
    fun recentTransactionsForWorker(workerId: Long, limit: Int = 10) = db.transactionDao().recentForWorker(workerId, limit)

    suspend fun saveTransaction(txn: WorkerTransaction): String? {
        if (txn.txnType !in com.example.data.model.TxnTypes.LABELS) return "Please choose a valid transaction type."
        if (txn.amount <= 0) return "Transaction amount must be greater than zero."
        db.transactionDao().upsert(txn)
        return null
    }

    suspend fun toggleTransaction(id: Long) {
        db.transactionDao().byId(id)?.let {
            db.transactionDao().upsert(it.copy(status = if (it.status == "active") "cancelled" else "active"))
        }
    }

    suspend fun deleteTransaction(id: Long) {
        db.transactionDao().byId(id)?.let { db.transactionDao().delete(it) }
    }

    // ── Notifications ─────────────────────────────────────────────────────
    val notifications = db.notificationDao().recent()
    val unreadCount = db.notificationDao().unreadCount()
    suspend fun markNotificationsRead() = db.notificationDao().markAllRead()
    suspend fun addNotification(notification: Notification) = db.notificationDao().insert(notification)

    // ── Company ───────────────────────────────────────────────────────────
    val company = db.companyDao().settings()
    suspend fun saveCompany(setting: CompanySetting) = db.companyDao().upsert(setting)

    // ── Users (for settings screen) ───────────────────────────────────────
    val attendanceUsers = db.userDao().attendanceUsers()
}

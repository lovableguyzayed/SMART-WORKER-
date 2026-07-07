package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClosureDay
import com.example.data.model.CompanySetting
import com.example.data.model.Department
import com.example.data.model.LeaveAdjustment
import com.example.data.model.Notification
import com.example.data.model.PayrollRecord
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Site
import com.example.data.model.User
import com.example.data.model.WorkTask
import com.example.data.model.Worker
import com.example.data.model.WorkerModification
import com.example.data.model.WorkerTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): User?

    @Query("SELECT * FROM users WHERE role = 'attendance' ORDER BY fullName")
    fun attendanceUsers(): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User): Long

    @Delete
    suspend fun delete(user: User)
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company_settings WHERE id = 1")
    fun settings(): Flow<CompanySetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: CompanySetting)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY fullName")
    fun all(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE status = 'active' ORDER BY fullName")
    fun active(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE status = 'active' ORDER BY fullName")
    suspend fun activeOnce(): List<Worker>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun byIdFlow(id: Long): Flow<Worker?>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun byId(id: Long): Worker?

    @Query("SELECT * FROM workers WHERE UPPER(workerCode) = UPPER(:code) LIMIT 1")
    suspend fun byCode(code: String): Worker?

    @Query("SELECT workerCode FROM workers WHERE workerCode LIKE :prefix || '%' ORDER BY workerCode DESC LIMIT 1")
    suspend fun lastCodeWithPrefix(prefix: String): String?

    @Query("SELECT DISTINCT department FROM workers ORDER BY department")
    fun departmentsInUse(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM workers WHERE status = 'active'")
    fun activeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(worker: Worker): Long

    @Update
    suspend fun update(worker: Worker)

    @Delete
    suspend fun delete(worker: Worker)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun byDate(date: LocalDate): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun byWorkerAndDate(workerId: Long, date: LocalDate): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY date DESC LIMIT :limit")
    fun recentForWorker(workerId: Long, limit: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date BETWEEN :start AND :end ORDER BY date")
    suspend fun forWorkerBetween(workerId: Long, start: LocalDate, end: LocalDate): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :start AND :end")
    suspend fun betweenDates(start: LocalDate, end: LocalDate): List<AttendanceRecord>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE workerId = :workerId AND status = 'leave' AND date < :before")
    suspend fun leaveDaysBefore(workerId: Long, before: LocalDate): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE date = :date AND status IN ('present', 'late')")
    fun presentCountOn(date: LocalDate): Flow<Int>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY createdAt DESC LIMIT :limit")
    fun recentOn(date: LocalDate, limit: Int): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceRecord): Long

    @Delete
    suspend fun delete(record: AttendanceRecord)
}

@Dao
interface PayrollDao {
    @Query("SELECT * FROM payroll_records WHERE month = :month AND year = :year")
    fun forMonth(month: Int, year: Int): Flow<List<PayrollRecord>>

    @Query("SELECT * FROM payroll_records WHERE workerId = :workerId AND month = :month AND year = :year LIMIT 1")
    suspend fun find(workerId: Long, month: Int, year: Int): PayrollRecord?

    @Query("SELECT * FROM payroll_records WHERE id = :id")
    suspend fun byId(id: Long): PayrollRecord?

    @Query("SELECT * FROM payroll_records WHERE workerId = :workerId ORDER BY year DESC, month DESC LIMIT :limit")
    suspend fun historyForWorker(workerId: Long, limit: Int): List<PayrollRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PayrollRecord): Long
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM worker_transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun betweenDates(start: LocalDate, end: LocalDate): Flow<List<WorkerTransaction>>

    @Query("SELECT * FROM worker_transactions WHERE workerId = :workerId AND status = 'active' AND date BETWEEN :start AND :end ORDER BY date")
    suspend fun activeForWorkerBetween(workerId: Long, start: LocalDate, end: LocalDate): List<WorkerTransaction>

    @Query("SELECT * FROM worker_transactions WHERE workerId = :workerId ORDER BY date DESC LIMIT :limit")
    fun recentForWorker(workerId: Long, limit: Int): Flow<List<WorkerTransaction>>

    @Query("SELECT * FROM worker_transactions WHERE id = :id")
    suspend fun byId(id: Long): WorkerTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(txn: WorkerTransaction): Long

    @Delete
    suspend fun delete(txn: WorkerTransaction)
}

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name")
    fun all(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE status = 'active' ORDER BY name")
    fun active(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun byId(id: Long): Site?

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: Site): Long

    @Delete
    suspend fun delete(site: Site)

    @Query("SELECT COUNT(*) FROM project_assignments WHERE siteId = :siteId")
    suspend fun referenceCount(siteId: Long): Int
}

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name")
    fun all(): Flow<List<Department>>

    @Query("SELECT * FROM departments WHERE status = 'active' ORDER BY name")
    fun active(): Flow<List<Department>>

    @Query("SELECT COUNT(*) FROM departments")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(department: Department): Long

    @Delete
    suspend fun delete(department: Department)

    @Query("SELECT COUNT(*) FROM workers WHERE department = :name")
    suspend fun referenceCount(name: String): Int
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name")
    fun all(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE status != 'archived' ORDER BY name")
    fun activeOrCompleted(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun allOnce(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun byId(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project): Long

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT COUNT(*) FROM project_assignments WHERE projectId = :projectId")
    suspend fun referenceCount(projectId: Long): Int
}

@Dao
interface WorkTaskDao {
    @Query("SELECT * FROM work_tasks ORDER BY name")
    fun all(): Flow<List<WorkTask>>

    @Query("SELECT * FROM work_tasks WHERE status != 'archived' ORDER BY name")
    fun activeTasks(): Flow<List<WorkTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: WorkTask): Long

    @Delete
    suspend fun delete(task: WorkTask)

    @Query("SELECT COUNT(*) FROM project_assignments WHERE taskId = :taskId")
    suspend fun referenceCount(taskId: Long): Int
}

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM project_assignments WHERE workerId = :workerId ORDER BY startDate DESC")
    fun forWorker(workerId: Long): Flow<List<ProjectAssignment>>

    @Query("SELECT * FROM project_assignments WHERE workerId = :workerId ORDER BY startDate DESC")
    suspend fun forWorkerOnce(workerId: Long): List<ProjectAssignment>

    @Query("SELECT * FROM project_assignments ORDER BY startDate DESC")
    fun all(): Flow<List<ProjectAssignment>>

    @Query("SELECT * FROM project_assignments")
    suspend fun allOnce(): List<ProjectAssignment>

    @Query("SELECT * FROM project_assignments WHERE id = :id")
    suspend fun byId(id: Long): ProjectAssignment?

    @Query("UPDATE project_assignments SET status = 'completed', endDate = :endDate WHERE id = :id")
    suspend fun endAssignment(id: Long, endDate: LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(assignment: ProjectAssignment): Long
}

@Dao
interface ClosureDao {
    @Query("SELECT * FROM closure_days WHERE date >= :from ORDER BY date")
    fun upcoming(from: LocalDate): Flow<List<ClosureDay>>

    @Query("SELECT * FROM closure_days WHERE date = :date")
    suspend fun onDate(date: LocalDate): List<ClosureDay>

    @Query("SELECT * FROM closure_days WHERE date BETWEEN :start AND :end")
    suspend fun betweenDates(start: LocalDate, end: LocalDate): List<ClosureDay>

    @Query("SELECT * FROM closure_days WHERE date = :date AND scope = :scope AND siteId IS :siteId AND projectId IS :projectId LIMIT 1")
    suspend fun find(date: LocalDate, scope: String, siteId: Long?, projectId: Long?): ClosureDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(closure: ClosureDay): Long

    @Delete
    suspend fun delete(closure: ClosureDay)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 100")
    fun recent(): Flow<List<Notification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun unreadCount(): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Insert
    suspend fun insert(notification: Notification)
}

@Dao
interface ModificationDao {
    @Query("SELECT * FROM worker_modifications WHERE workerId = :workerId ORDER BY createdAt DESC")
    fun forWorker(workerId: Long): Flow<List<WorkerModification>>

    @Insert
    suspend fun insert(modification: WorkerModification)
}

@Dao
interface LeaveAdjustmentDao {
    @Query("SELECT * FROM leave_adjustments WHERE workerId = :workerId ORDER BY createdAt DESC")
    fun forWorker(workerId: Long): Flow<List<LeaveAdjustment>>

    @Query("SELECT * FROM leave_adjustments WHERE workerId = :workerId")
    suspend fun forWorkerOnce(workerId: Long): List<LeaveAdjustment>

    @Insert
    suspend fun insert(adjustment: LeaveAdjustment)
}

package com.example.data.repo

import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Notification
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.domain.PayrollCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AttendanceRepository(private val db: AppDatabase) {

    fun recordsOn(date: LocalDate) = db.attendanceDao().byDate(date)
    fun presentCountOn(date: LocalDate) = db.attendanceDao().presentCountOn(date)
    fun recentOn(date: LocalDate, limit: Int = 10) = db.attendanceDao().recentOn(date, limit)
    fun recentForWorker(workerId: Long, limit: Int = 30) = db.attendanceDao().recentForWorker(workerId, limit)

    suspend fun recordFor(workerId: Long, date: LocalDate) = db.attendanceDao().byWorkerAndDate(workerId, date)

    /** Timestamp used when marking on a historical date (noon anchor to avoid TZ edge cases). */
    private fun attendanceTimestamp(date: LocalDate): LocalDateTime =
        if (date == LocalDate.now()) LocalDateTime.now() else LocalDateTime.of(date, LocalTime.NOON)

    sealed interface MarkResult {
        data class Ok(val message: String) : MarkResult
        data class Denied(val message: String) : MarkResult
    }

    /**
     * Mark a single worker. Tapping again on an open shift checks the worker out.
     * Enforces the same permission rules as the Flask backend: attendance users
     * may only check workers in/out, not record absences/leaves or edit closed records.
     */
    suspend fun mark(
        actor: User,
        worker: Worker,
        date: LocalDate,
        status: String,
        leaveType: String? = null,
        markedVia: String = "manual",
    ): MarkResult {
        if (status !in AttendanceStatus.ALL) return MarkResult.Denied("Invalid attendance status.")
        if (!actor.isAdmin && status !in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) {
            return MarkResult.Denied("Please contact the Administrator to mark absences or leaves.")
        }

        val closures = db.closureDao().onDate(date)
        val assignments = db.assignmentDao().forWorkerOnce(worker.id)
        val closure = PayrollCalculator.closureForWorkerOnDate(closures, assignments, date)
        if (closure != null && !closure.allowAttendance) {
            return MarkResult.Denied("Attendance is locked for ${worker.fullName} on this closure day (${closure.reason}).")
        }

        val existing = db.attendanceDao().byWorkerAndDate(worker.id, date)
        if (existing != null && !actor.isAdmin) {
            val isSimpleCheckout = existing.status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE) &&
                status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE) &&
                existing.checkInTime != null && existing.checkOutTime == null
            if (!isSimpleCheckout) {
                return MarkResult.Denied("Please contact the Administrator to make changes.")
            }
        }

        val message: String
        val record: AttendanceRecord
        val now = attendanceTimestamp(date)
        val activeSiteId = assignments.firstOrNull { it.activeOn(date) }?.siteId

        if (existing != null) {
            val previousStatus = existing.status
            var updated = existing.copy(
                status = status,
                markedBy = actor.id,
                markedVia = markedVia,
                leaveType = if (status == AttendanceStatus.LEAVE) leaveType else null,
            )
            if (status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) {
                val sameActiveState = previousStatus in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)
                if (existing.checkInTime == null) {
                    updated = updated.copy(checkInTime = now, checkOutTime = null, overtimeMinutes = 0)
                    if (status == AttendanceStatus.PRESENT && PayrollCalculator.isLateCheckIn(worker, date, now)) {
                        updated = updated.copy(status = AttendanceStatus.LATE)
                    }
                    updated = updated.copy(lateMinutes = PayrollCalculator.lateMinutesForRecord(worker, updated))
                    message = if (updated.status == AttendanceStatus.LATE && updated.lateMinutes > 0)
                        "Check-in captured — ${updated.lateMinutes} min late." else "Check-in captured successfully."
                } else if (existing.checkOutTime == null && sameActiveState) {
                    updated = updated.copy(status = previousStatus, checkOutTime = now)
                    updated = updated.copy(overtimeMinutes = PayrollCalculator.overtimeMinutesForRecord(worker, updated))
                    val otPay = PayrollCalculator.overtimePay(worker, updated.overtimeMinutes)
                    var base = if (updated.overtimeMinutes > 0)
                        "Shift closed. Overtime: ${updated.overtimeMinutes} minutes (₹${PayrollCalculator.round2(otPay)})."
                    else "Shift closed successfully."
                    val (_, mode) = PayrollCalculator.dailyWageForRecord(worker, updated)
                    base = when (mode) {
                        PayrollCalculator.PayMode.HALF_DAY -> "$base Half-day rate will apply (includes ${PayrollCalculator.halfDayGraceMinutes(worker)} min grace window)."
                        PayrollCalculator.PayMode.PRO_RATA -> "$base Minute-based pro-rata daily wage will apply."
                        else -> base
                    }
                    message = base
                } else {
                    updated = updated.copy(checkOutTime = null, overtimeMinutes = 0)
                    updated = updated.copy(lateMinutes = PayrollCalculator.lateMinutesForRecord(worker, updated))
                    message = "Attendance status updated successfully."
                }
            } else {
                updated = updated.copy(checkInTime = null, checkOutTime = null, overtimeMinutes = 0, lateMinutes = 0)
                message = "Attendance marked successfully."
            }
            record = updated
        } else {
            var fresh = AttendanceRecord(
                workerId = worker.id,
                date = date,
                status = status,
                checkInTime = if (status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)) now else null,
                leaveType = if (status == AttendanceStatus.LEAVE) leaveType else null,
                markedBy = actor.id,
                markedVia = markedVia,
                siteId = activeSiteId,
            )
            if (status == AttendanceStatus.PRESENT && PayrollCalculator.isLateCheckIn(worker, date, fresh.checkInTime)) {
                fresh = fresh.copy(status = AttendanceStatus.LATE)
            }
            fresh = fresh.copy(lateMinutes = PayrollCalculator.lateMinutesForRecord(worker, fresh))
            record = fresh
            message = when {
                fresh.status == AttendanceStatus.LATE && fresh.lateMinutes > 0 -> "Check-in captured — ${fresh.lateMinutes} min late."
                status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE) -> "Check-in captured successfully."
                else -> "Attendance marked successfully."
            }
        }

        db.attendanceDao().upsert(record)

        if (!actor.isAdmin) {
            db.notificationDao().insert(
                Notification(
                    title = "Attendance: ${worker.fullName} — $status",
                    body = "${actor.fullName} marked ${worker.fullName} (${worker.workerCode}) as $status on $date",
                )
            )
        }
        return MarkResult.Ok(message)
    }

    /** Bulk mark all active workers (admin only). Skips locked closure workers. */
    suspend fun bulkMark(actor: User, date: LocalDate, status: String): MarkResult {
        if (!actor.isAdmin) return MarkResult.Denied("Please contact the Administrator to make changes.")
        if (status !in AttendanceStatus.ALL) return MarkResult.Denied("Invalid attendance status.")
        val closures = db.closureDao().onDate(date)
        val companyLock = closures.firstOrNull { (it.scope.isBlank() || it.scope == "company") && !it.allowAttendance }
        if (companyLock != null) return MarkResult.Denied("Attendance is locked on this closure day (${companyLock.reason}).")

        val all = db.workerDao().activeOnce()
        var count = 0
        val now = attendanceTimestamp(date)
        for (worker in all) {
            val assignments = db.assignmentDao().forWorkerOnce(worker.id)
            val workerClosure = PayrollCalculator.closureForWorkerOnDate(closures, assignments, date)
            if (workerClosure != null && !workerClosure.allowAttendance) continue
            val existing = db.attendanceDao().byWorkerAndDate(worker.id, date)
            val activeSiteId = assignments.firstOrNull { it.activeOn(date) }?.siteId
            val isActive = status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE)
            val record = (existing ?: AttendanceRecord(workerId = worker.id, date = date, status = status)).copy(
                status = status,
                markedBy = actor.id,
                markedVia = "bulk",
                checkInTime = if (isActive) now else null,
                checkOutTime = null,
                overtimeMinutes = 0,
                lateMinutes = 0,
                siteId = existing?.siteId ?: activeSiteId,
            )
            db.attendanceDao().upsert(record)
            count++
        }
        return MarkResult.Ok("Bulk attendance marked as $status for $count worker(s).")
    }
}

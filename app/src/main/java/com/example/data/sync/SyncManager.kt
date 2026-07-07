package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Cloud replication for the offline-first database.
 *
 * The local Room database is always the source of truth — the app is fully
 * functional with no network and no configuration. When the APK is built with
 * SUPABASE_URL and SUPABASE_ANON_KEY in `.env`, every sync run replicates the
 * core tables to the Supabase project via its PostgREST API (upsert on the
 * primary key), giving automatic off-device backup and a live server-side copy
 * that dashboards or the Flask app can read. Without those keys every call is
 * a silent no-op, so unconfigured builds never fail.
 */
class SyncManager(private val db: AppDatabase, context: Context) {

    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.startsWith("http") &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY != "UNSET"

    private val _lastResult = MutableStateFlow<String?>(prefs.getString(KEY_LAST_RESULT, null))
    val lastResult: StateFlow<String?> = _lastResult

    sealed interface SyncResult {
        data class Success(val rows: Int) : SyncResult
        data class Skipped(val reason: String) : SyncResult
        data class Failed(val message: String) : SyncResult
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext SyncResult.Skipped(
                "Cloud sync is not configured. Build the APK with SUPABASE_URL and SUPABASE_ANON_KEY in .env to enable it."
            )
        }
        try {
            var total = 0
            total += push("sw_workers", db.workerDao().all().first().map { w ->
                obj(
                    "id" to w.id, "worker_code" to w.workerCode, "full_name" to w.fullName,
                    "phone" to w.phone, "email" to w.email, "address" to w.address,
                    "position" to w.position, "department" to w.department,
                    "employee_type" to w.employeeType, "join_date" to w.joinDate.iso(),
                    "pay_type" to w.payType, "daily_rate" to w.dailyRate,
                    "monthly_salary" to w.monthlySalary, "hourly_rate" to w.hourlyRate,
                    "project_rate" to w.projectRate, "start_time" to w.startTime?.iso(),
                    "end_time" to w.endTime?.iso(), "status" to w.status,
                )
            })
            total += push("sw_attendance", db.attendanceDao().betweenDates(LocalDate.now().minusDays(90), LocalDate.now()).map { r ->
                obj(
                    "id" to r.id, "worker_id" to r.workerId, "date" to r.date.iso(),
                    "check_in" to r.checkInTime?.iso(), "check_out" to r.checkOutTime?.iso(),
                    "status" to r.status, "overtime_minutes" to r.overtimeMinutes,
                    "late_minutes" to r.lateMinutes, "leave_type" to r.leaveType,
                    "site_id" to r.siteId, "marked_via" to r.markedVia,
                )
            })
            total += push("sw_transactions", db.transactionDao().betweenDates(LocalDate.now().minusDays(365), LocalDate.now()).first().map { t ->
                obj(
                    "id" to t.id, "worker_id" to t.workerId, "txn_type" to t.txnType,
                    "amount" to t.amount, "date" to t.date.iso(),
                    "description" to t.description, "status" to t.status,
                )
            })
            total += push("sw_sites", db.siteDao().all().first().map { s ->
                obj("id" to s.id, "name" to s.name, "address" to s.address, "status" to s.status)
            })
            total += push("sw_projects", db.projectDao().allOnce().map { p ->
                obj(
                    "id" to p.id, "name" to p.name, "site_id" to p.siteId,
                    "deadline" to p.deadline?.iso(), "completion_date" to p.completionDate?.iso(),
                    "penalty_type" to p.penaltyType, "penalty_value" to p.penaltyValue,
                    "status" to p.status,
                )
            })
            total += push("sw_assignments", db.assignmentDao().allOnce().map { a ->
                obj(
                    "id" to a.id, "worker_id" to a.workerId, "project_id" to a.projectId,
                    "site_id" to a.siteId, "task_id" to a.taskId,
                    "start_date" to a.startDate.iso(), "end_date" to a.endDate?.iso(),
                    "status" to a.status,
                )
            })

            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
            val summary = "Synced $total row(s) at $stamp"
            prefs.edit().putString(KEY_LAST_RESULT, summary).apply()
            _lastResult.value = summary
            SyncResult.Success(total)
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed", e)
            val message = "Sync failed: ${e.message ?: e.javaClass.simpleName}"
            _lastResult.value = message
            SyncResult.Failed(message)
        }
    }

    /** Upsert [rows] into a Supabase table; returns the row count pushed. */
    private fun push(table: String, rows: List<JSONObject>): Int {
        if (rows.isEmpty()) return 0
        val body = JSONArray(rows).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .header("Prefer", "resolution=merge-duplicates")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("$table → HTTP ${response.code}")
            }
        }
        return rows.size
    }

    private fun obj(vararg pairs: Pair<String, Any?>): JSONObject {
        val o = JSONObject()
        for ((k, v) in pairs) o.put(k, v ?: JSONObject.NULL)
        return o
    }

    private fun LocalDate.iso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)
    private fun LocalDateTime.iso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    private fun LocalTime.iso(): String = format(DateTimeFormatter.ISO_LOCAL_TIME)

    companion object {
        private const val TAG = "SmartWorkerSync"
        private const val KEY_LAST_RESULT = "last_result"
    }
}

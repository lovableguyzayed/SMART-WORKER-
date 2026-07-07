package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.SmartWorkerApp
import java.util.concurrent.TimeUnit

/**
 * Background replication: WorkManager runs this every 6 hours whenever the
 * device has connectivity. A no-op when cloud sync isn't configured, so the
 * schedule is safe to register unconditionally.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SmartWorkerApp ?: return Result.failure()
        return when (app.container.syncManager.syncNow()) {
            is SyncManager.SyncResult.Success -> Result.success()
            is SyncManager.SyncResult.Skipped -> Result.success()
            is SyncManager.SyncResult.Failed -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "smartworker-cloud-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }
    }
}

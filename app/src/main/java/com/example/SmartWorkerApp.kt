package com.example

import android.app.Application
import com.example.data.AppContainer
import com.example.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartWorkerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            container.seedIfEmpty()
            container.authRepository.restoreSession()
        }
        // Background cloud replication (no-op until Supabase keys are configured).
        SyncWorker.schedule(this)
    }
}

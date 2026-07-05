package com.example

import android.app.Application
import com.example.data.AppContainer
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
    }
}

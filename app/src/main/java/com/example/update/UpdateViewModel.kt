package com.example.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Drives the OTA flow: check → (dialog) → download → install. Exposes plain
 * StateFlows the UI collects with collectAsStateWithLifecycle.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    // ─────────────────────────────────────────────────────────────────────────
    //  EDIT ME: point this at your hosted update.json (GitHub Releases raw URL,
    //  your Flask backend, S3, etc.). Must be reachable over HTTPS.
    // ─────────────────────────────────────────────────────────────────────────
    private val manifestUrl =
        "https://raw.githubusercontent.com/lovableguyzayed/SMART-WORKER-/main/update.json"

    private val repository = UpdateRepository(app, manifestUrl)
    private val manager = UpdateManager(app)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /** Silent on failure: a dead update server must never block app usage. */
    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = repository.check()
        }
    }

    /** Download the APK, then auto-install (routing through the permission gate). */
    fun downloadAndInstall(manifest: UpdateManifest) {
        viewModelScope.launch {
            manager.downloadApk(manifest.apkUrl).collect { state ->
                _downloadState.value = state
                if (state is DownloadState.Completed) {
                    tryInstall(File(state.apkPath))
                }
            }
        }
    }

    /**
     * Installs [apk] if we're allowed, otherwise emits [DownloadState.NeedsPermission]
     * so the Activity can open the system settings screen and resume afterwards.
     */
    fun tryInstall(apk: File) {
        if (manager.needsInstallPermission()) {
            _downloadState.value = DownloadState.NeedsPermission(apk.absolutePath)
        } else {
            manager.installApk(apk)
        }
    }

    /** Intent for the "allow install unknown apps" settings screen. */
    fun installPermissionIntent() = manager.requestInstallPermissionIntent()

    /** Called after the user returns from granting the permission. */
    fun onInstallPermissionResult() {
        val state = _downloadState.value
        if (state is DownloadState.NeedsPermission) {
            tryInstall(File(state.apkPath))
        }
    }

    /** User tapped "Later" on a non-forced update. */
    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
        _downloadState.value = DownloadState.Idle
    }
}

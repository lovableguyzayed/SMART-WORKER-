package com.example.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The hosted version manifest (update.json). Only [latestVersionCode] and
 * [apkUrl] are strictly required; the rest have safe defaults so an older app
 * can parse a newer manifest and vice-versa.
 */
@Serializable
data class UpdateManifest(
    @SerialName("latestVersionCode") val latestVersionCode: Long,
    @SerialName("latestVersionName") val latestVersionName: String = "",
    @SerialName("apkUrl") val apkUrl: String,
    @SerialName("releaseNotes") val releaseNotes: String = "",
    @SerialName("forceUpdate") val forceUpdate: Boolean = false,
    /** Builds older than this are forced to update (e.g. a breaking server change). */
    @SerialName("minSupportedVersionCode") val minSupportedVersionCode: Long = 0,
)

/** Result of checking the manifest against the installed build. */
sealed interface UpdateState {
    /** No check has run yet, or the dialog was dismissed. */
    data object Idle : UpdateState

    /** Installed build is current — nothing to do. */
    data object UpToDate : UpdateState

    /** A newer build exists. [forced] hides "Later" and blocks dismissal. */
    data class UpdateAvailable(val manifest: UpdateManifest, val forced: Boolean) : UpdateState

    /** The check failed (offline, bad JSON, 404…). Non-fatal — the app keeps running. */
    data class Error(val message: String) : UpdateState
}

/** Progress of the APK download + install hand-off. */
sealed interface DownloadState {
    data object Idle : DownloadState

    /** [progress] is 0f..1f, or -1f when the server didn't send a Content-Length. */
    data class Downloading(val progress: Float) : DownloadState

    /** APK is on disk and ready to install. */
    data class Completed(val apkPath: String) : DownloadState

    /** The device must grant "install unknown apps" before we can continue. */
    data class NeedsPermission(val apkPath: String) : DownloadState

    data class Failed(val message: String) : DownloadState
}

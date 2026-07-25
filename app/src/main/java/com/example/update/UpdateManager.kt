package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads the new APK into app-specific external storage and hands it to the
 * system package installer. No storage permission is required because the file
 * lives under getExternalFilesDir(); it is shared with the installer through a
 * FileProvider content:// URI.
 */
class UpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    private val authority = "${context.packageName}.fileprovider"

    private fun apkDir(): File = File(context.getExternalFilesDir(null), "apk").apply { mkdirs() }

    /**
     * Streams [apkUrl] to disk, emitting [DownloadState.Downloading] as bytes
     * arrive and finally [DownloadState.Completed]. Old APKs are deleted first
     * so storage doesn't accumulate stale installers.
     */
    fun downloadApk(apkUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        val dir = apkDir()
        dir.listFiles()?.forEach { it.delete() } // clean previous downloads

        val outFile = File(dir, "update-${System.currentTimeMillis()}.apk")
        val request = Request.Builder().url(apkUrl).get().build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(DownloadState.Failed("Download failed: HTTP ${response.code}"))
                return@flow
            }
            val body = response.body ?: run {
                emit(DownloadState.Failed("Empty download response"))
                return@flow
            }
            val total = body.contentLength() // -1 when the server omits Content-Length
            var readSoFar = 0L

            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        readSoFar += read
                        val progress = if (total > 0) (readSoFar.toFloat() / total) else -1f
                        emit(DownloadState.Downloading(progress))
                    }
                    output.flush()
                }
            }
        }
        emit(DownloadState.Completed(outFile.absolutePath))
    }.flowOn(Dispatchers.IO)

    /** API 26+: the user must allow this app to install unknown apps. */
    fun needsInstallPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()

    /**
     * Intent that opens the system "allow install from this source" screen for
     * THIS package. Launch it with an ActivityResult launcher and resume the
     * install when the user returns.
     */
    fun requestInstallPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /**
     * Hands the downloaded APK to the package installer. On API 24+ we must use a
     * FileProvider content:// URI (a raw file:// URI throws FileUriExposedException)
     * and grant the installer temporary read access.
     */
    fun installApk(apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Required because we start the installer from a non-Activity context.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

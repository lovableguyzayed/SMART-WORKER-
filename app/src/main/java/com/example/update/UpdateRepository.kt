package com.example.update

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches and parses the hosted [UpdateManifest], then compares it to the
 * installed build. All network + parsing work runs on [Dispatchers.IO].
 */
class UpdateRepository(
    private val context: Context,
    private val manifestUrl: String,
    private val client: OkHttpClient = defaultClient(),
) {
    // Tolerant parser: unknown/extra fields in the manifest never crash an old app.
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val body = fetchManifest()
            val manifest = json.decodeFromString<UpdateManifest>(body)
            val current = currentVersionCode()

            when {
                current < manifest.minSupportedVersionCode ->
                    UpdateState.UpdateAvailable(manifest, forced = true)
                manifest.latestVersionCode > current ->
                    UpdateState.UpdateAvailable(manifest, forced = manifest.forceUpdate)
                else -> UpdateState.UpToDate
            }
        } catch (e: Exception) {
            UpdateState.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun fetchManifest(): String {
        val request = Request.Builder().url(manifestUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Manifest HTTP ${response.code}")
            return response.body?.string() ?: error("Empty manifest response")
        }
    }

    /** Installed versionCode, read the correct way per API level. */
    private fun currentVersionCode(): Long {
        val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

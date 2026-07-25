package com.example.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * Update prompt. Shows version + release notes; during download swaps in a
 * progress bar and disables the buttons; on failure turns the primary button
 * into "Retry". A forced update cannot be dismissed and has no "Later".
 */
@Composable
fun UpdateDialog(
    update: UpdateState.UpdateAvailable,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val manifest = update.manifest
    val downloading = downloadState is DownloadState.Downloading
    val failed = downloadState is DownloadState.Failed
    val forced = update.forced

    AlertDialog(
        onDismissRequest = { if (!forced && !downloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !forced,
            dismissOnClickOutside = !forced,
        ),
        title = {
            Text(
                if (manifest.latestVersionName.isNotBlank())
                    "Update available — v${manifest.latestVersionName}"
                else "Update available",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (manifest.releaseNotes.isNotBlank()) {
                    Text(manifest.releaseNotes, fontSize = 14.sp)
                } else {
                    Text("A new version of the app is ready to install.", fontSize = 14.sp)
                }
                if (forced) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This is a required update.",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    )
                }

                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Spacer(Modifier.height(16.dp))
                        val p = downloadState.progress
                        if (p >= 0f) {
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("${(p * 100).roundToInt()}%", fontSize = 12.sp)
                        } else {
                            // Server sent no Content-Length — show an indeterminate bar.
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("Downloading…", fontSize = 12.sp)
                        }
                    }
                    is DownloadState.Failed -> {
                        Spacer(Modifier.height(12.dp))
                        Text(downloadState.message, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !downloading) {
                Text(if (failed) "Retry" else "Download & Update")
            }
        },
        dismissButton = {
            // A forced update offers no way out; a normal one shows "Later".
            if (!forced) {
                TextButton(onClick = onDismiss, enabled = !downloading) { Text("Later") }
            }
        },
    )
}

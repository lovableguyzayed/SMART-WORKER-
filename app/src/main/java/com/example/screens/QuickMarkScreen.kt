package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.User
import com.example.data.repo.AttendanceRepository
import com.example.ui.LocalAppContainer
import com.example.ui.SwCard
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.contentColorsFor
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface2
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.QuickMarkViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Quick Mark — check a worker in or out by typed code or scanned QR. The day's
 * running totals sit on top so the scanner doubles as a live tally.
 */
@Composable
fun QuickMarkScreen(vm: QuickMarkViewModel, user: User, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val container = LocalAppContainer.current
    val todayRecords by container.attendanceRepository.recordsOn(LocalDate.now())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val state by vm.state.collectAsStateLifecycle()
    val busy by vm.busy.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        containerColor = Ink,
        topBar = {
            SwTopBar(
                title = "Quick Mark",
                leading = {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Surface3)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnDark, modifier = Modifier.size(19.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
        ) {
            // ── Today's running tally ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanStat("Present", todayRecords.count { it.status == AttendanceStatus.PRESENT }, Lime, Modifier.weight(1f))
                ScanStat("Late", todayRecords.count { it.status == AttendanceStatus.LATE }, Sun, Modifier.weight(1f))
                ScanStat("Absent", todayRecords.count { it.status == AttendanceStatus.ABSENT }, Coral, Modifier.weight(1f))
                ScanStat("Open", todayRecords.count { it.checkInTime != null && it.checkOutTime == null }, Volt, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            // ── Entry ──
            SwCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Flame),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null, tint = White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Check in or out", style = SwType.Title.copy(fontSize = 16.sp), color = OnDark)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Type the worker's ID (e.g. MA001) or scan their card.",
                        style = SwType.Caption, color = OnDarkMuted, textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Surface3)
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Badge, null, tint = OnDarkMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(11.dp))
                        Box(Modifier.weight(1f)) {
                            if (code.isEmpty()) {
                                Text("Employee ID", style = SwType.Body, color = OnDarkFaint)
                            }
                            BasicTextField(
                                value = code,
                                onValueChange = { code = it.uppercase() },
                                singleLine = true,
                                textStyle = SwType.Body.copy(color = OnDark),
                                cursorBrush = SolidColor(Flame),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ScanButton(
                            "Find", Icons.Filled.Search, Flame, White,
                            enabled = code.isNotBlank() && !busy,
                            modifier = Modifier.weight(1f),
                        ) { vm.lookup(user, code) }
                        ScanButton(
                            if (scanning) "Stop" else "Scan",
                            Icons.Filled.QrCodeScanner,
                            if (scanning) Coral else Surface3,
                            if (scanning) White else OnDark,
                            enabled = true,
                            modifier = Modifier.weight(1f),
                        ) { scanning = !scanning }
                    }

                    if (scanning) {
                        Spacer(Modifier.height(14.dp))
                        com.example.ui.QrScannerPanel { scanned ->
                            scanning = false
                            code = scanned.removePrefix("SMARTWORKER:").trim().uppercase()
                            vm.lookup(user, scanned)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (val s = state) {
                null -> if (busy) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Flame)
                    }
                }

                is AttendanceRepository.Lookup.NotFound -> {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Coral.copy(alpha = 0.14f)).padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.message, style = SwType.Body.copy(fontSize = 13.5.sp), color = Coral)
                    }
                }

                is AttendanceRepository.Lookup.Found -> {
                    val r = s.result
                    val accent = accentFor(r.worker.workerCode.ifBlank { r.worker.fullName })
                    SwCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        r.worker.fullName.take(1).uppercase(),
                                        color = contentColorsFor(accent).first,
                                        fontSize = 20.sp,
                                        style = SwType.Title,
                                    )
                                }
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        r.worker.fullName, style = SwType.Title.copy(fontSize = 16.sp), color = OnDark,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${r.worker.workerCode}  ·  ${r.worker.position}",
                                        style = SwType.Caption, color = OnDarkMuted,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TodayStat(
                                    "Status",
                                    r.record?.status?.replaceFirstChar { it.uppercase() } ?: "Not marked",
                                    r.record?.let { statusAccent(it.status) } ?: OnDarkFaint,
                                    Modifier.weight(1f),
                                )
                                TodayStat("Check-in", r.record?.checkInTime?.format(timeFmt) ?: "—", OnDark, Modifier.weight(1f))
                                TodayStat("Check-out", r.record?.checkOutTime?.format(timeFmt) ?: "—", OnDark, Modifier.weight(1f))
                            }

                            if (r.closureReason != null) {
                                Spacer(Modifier.height(14.dp))
                                val tone = if (r.closureLocked) Coral else Sun
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                                        .background(tone.copy(alpha = 0.14f)).padding(12.dp),
                                ) {
                                    Text(
                                        if (r.closureLocked) {
                                            "Attendance locked — closure day: ${r.closureReason}"
                                        } else {
                                            "Closure day (attendance allowed): ${r.closureReason}"
                                        },
                                        style = SwType.Caption.copy(fontSize = 12.sp), color = tone,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            when {
                                r.closureLocked -> Unit
                                r.canCheckIn -> BigAction(
                                    "Check in", Icons.AutoMirrored.Filled.Login, Lime, !busy,
                                ) { vm.markPresent(user, r.worker) }
                                r.canCheckOut -> BigAction(
                                    "Check out", Icons.AutoMirrored.Filled.Logout, Sun, !busy,
                                ) { vm.markPresent(user, r.worker) }
                                else -> Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .background(Surface3).padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "Shift already closed for today",
                                        style = SwType.Caption, color = OnDarkMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearMessage() },
            containerColor = Surface2,
            textContentColor = OnDark,
            confirmButton = { TextButton(onClick = { vm.clearMessage() }) { Text("OK", color = Flame) } },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun ScanStat(label: String, value: Int, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = SwType.StatSmall.copy(fontSize = 19.sp), color = tint)
        Text(label, fontSize = 10.sp, color = tint, maxLines = 1)
    }
}

@Composable
private fun TodayStat(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(Surface3).padding(11.dp),
    ) {
        Text(label, style = SwType.Label, color = OnDarkMuted)
        Spacer(Modifier.height(3.dp))
        Text(
            value, style = SwType.Title.copy(fontSize = 13.sp), color = tint,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScanButton(
    label: String,
    icon: ImageVector,
    container: Color,
    foreground: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) container else Surface3)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon, null,
            tint = if (enabled) foreground else OnDarkFaint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = SwType.Title.copy(fontSize = 14.sp),
            color = if (enabled) foreground else OnDarkFaint,
        )
    }
}

@Composable
private fun BigAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val fg = contentColorsFor(accent).first
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accent else Surface3)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (enabled) fg else OnDarkFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            style = SwType.Title.copy(fontSize = 15.sp),
            color = if (enabled) fg else OnDarkFaint,
        )
    }
}

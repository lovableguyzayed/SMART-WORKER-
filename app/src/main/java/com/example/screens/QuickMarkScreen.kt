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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repo.AttendanceRepository
import com.example.ui.CardBorder
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.theme.WarningBg
import com.example.ui.vm.QuickMarkViewModel
import java.time.format.DateTimeFormatter

/**
 * Quick Mark — check workers in/out by employee ID (the native port of the
 * Flask QR-scan flow; camera scanning can layer on top of the same lookup).
 */
@Composable
fun QuickMarkScreen(vm: QuickMarkViewModel, user: User, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val state by vm.state.collectAsStateLifecycle()
    val busy by vm.busy.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Quick Mark",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCodeScanner, null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Check in / out by Employee ID", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                    Text(
                        "Type the worker's ID (e.g. MA001) or scan their ID card QR.",
                        fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Employee ID") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Badge, null, tint = TextSecondary) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerColor,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.lookup(user, code) },
                            enabled = code.isNotBlank() && !busy,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        ) {
                            Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Find", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { scanning = !scanning },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (scanning) "Stop" else "Scan QR", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (scanning) {
                        Spacer(Modifier.height(12.dp))
                        com.example.ui.QrScannerPanel { scanned ->
                            scanning = false
                            code = scanned.removePrefix("SMARTWORKER:").trim().uppercase()
                            vm.lookup(user, scanned)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                null -> if (busy) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                is AttendanceRepository.Lookup.NotFound -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerBg),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Text(s.message, fontSize = 13.sp, color = Danger, modifier = Modifier.padding(14.dp))
                    }
                }
                is AttendanceRepository.Lookup.Found -> {
                    val r = s.result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(52.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.worker.fullName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                                    Text("${r.worker.workerCode} • ${r.worker.position} • ${r.worker.department}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Today's state
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TodayStat("Status", r.record?.status?.replaceFirstChar { it.uppercase() } ?: "Not marked", Modifier.weight(1f))
                                TodayStat("Check-in", r.record?.checkInTime?.format(timeFmt) ?: "—", Modifier.weight(1f))
                                TodayStat("Check-out", r.record?.checkOutTime?.format(timeFmt) ?: "—", Modifier.weight(1f))
                            }

                            if (r.closureReason != null) {
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (r.closureLocked) DangerBg else WarningBg),
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                ) {
                                    Text(
                                        if (r.closureLocked) "Attendance locked — closure day: ${r.closureReason}"
                                        else "Closure day (attendance allowed): ${r.closureReason}",
                                        fontSize = 12.sp,
                                        color = if (r.closureLocked) Danger else Warning,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            when {
                                r.closureLocked -> {}
                                r.canCheckIn -> ActionButton("Check In", Icons.AutoMirrored.Filled.Login, Success, !busy) {
                                    vm.markPresent(user, r.worker)
                                }
                                r.canCheckOut -> ActionButton("Check Out", Icons.AutoMirrored.Filled.Logout, Warning, !busy) {
                                    vm.markPresent(user, r.worker)
                                }
                                else -> StatusPill("Shift already closed for today", TextSecondary)
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
            confirmButton = { TextButton(onClick = { vm.clearMessage() }) { Text("OK") } },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun TodayStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 1)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

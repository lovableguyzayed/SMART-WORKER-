package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.ui.CardBorder
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.vm.AttendanceViewModel
import java.time.format.DateTimeFormatter

@Composable
fun SmartWorkerAttendanceScreen(vm: AttendanceViewModel, user: User, onOpenQuickMark: () -> Unit = {}) {
    val date by vm.date.collectAsStateLifecycle()
    val rows by vm.rows.collectAsStateLifecycle()
    val totals by vm.totals.collectAsStateLifecycle()
    var pendingWorker by remember { mutableStateOf<Worker?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showBulk by remember { mutableStateOf(false) }

    val fmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE") }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SwTopBar(title = "Attendance") },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Date selector
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Filled.ChevronLeft, "Previous day") { vm.shiftDate(-1) }
                Text(
                    date.format(fmt), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                CircleIconButton(Icons.Filled.ChevronRight, "Next day") { vm.shiftDate(1) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.height(32.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp))
                        .clickable { vm.today() }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Today", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium) }
            }

            // KPI row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniKpi("Present", totals.present, Success, Modifier.weight(1f))
                MiniKpi("Absent", totals.absent, Danger, Modifier.weight(1f))
                MiniKpi("Late", totals.late, Warning, Modifier.weight(1f))
                MiniKpi("Leave", totals.leave, Purple, Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user.isAdmin) {
                    BulkButton("Mark all present", Success, Modifier.weight(1f)) { showBulk = true }
                }
                BulkButton("Quick Mark (ID)", PrimaryBlue, Modifier.weight(1f), onOpenQuickMark)
            }

            HorizontalDivider(color = DividerColor)

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(rows, key = { it.worker.id }) { row ->
                    AttendanceRow(row.worker, row.record?.status) { pendingWorker = row.worker }
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    // Mark dialog
    pendingWorker?.let { worker ->
        MarkDialog(
            worker = worker,
            isAdmin = user.isAdmin,
            onDismiss = { pendingWorker = null },
            onPick = { status ->
                vm.mark(user, worker, status) { toast = it }
                pendingWorker = null
            },
        )
    }

    if (showBulk) {
        AlertDialog(
            onDismissRequest = { showBulk = false },
            title = { Text("Mark all present?") },
            text = { Text("This sets every active worker to Present for ${date.format(fmt)}. Existing records are updated.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.bulkMark(user, AttendanceStatus.PRESENT) { toast = it }
                    showBulk = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showBulk = false }) { Text("Cancel") } },
        )
    }

    toast?.let { message ->
        AlertDialog(
            onDismissRequest = { toast = null },
            confirmButton = { TextButton(onClick = { toast = null }) { Text("OK") } },
            text = { Text(message) },
        )
    }
}

@Composable
private fun MarkDialog(worker: Worker, isAdmin: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(worker.fullName, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Tap a status. Tapping Present again checks the worker out.", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                StatusChoice("Present / Check-out", Success) { onPick(AttendanceStatus.PRESENT) }
                StatusChoice("Late", Warning) { onPick(AttendanceStatus.LATE) }
                if (isAdmin) {
                    StatusChoice("Absent", Danger) { onPick(AttendanceStatus.ABSENT) }
                    StatusChoice("On Leave", Purple) { onPick(AttendanceStatus.LEAVE) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StatusChoice(label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy)
    }
}

@Composable
private fun MiniKpi(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BulkButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun AttendanceRow(worker: Worker, status: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp).background(CardBackground).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
            Text(worker.fullName.take(1), color = PrimaryBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(worker.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text("${worker.workerCode} • ${worker.position}", fontSize = 12.sp, color = TextSecondary)
        }
        if (status != null) {
            val (label, color) = statusLabelColor(status)
            StatusPill(label, color)
        } else {
            StatusPill("Not marked", TextSecondary)
        }
    }
}

private fun statusLabelColor(status: String): Pair<String, Color> = when (status) {
    AttendanceStatus.PRESENT -> "Present" to Success
    AttendanceStatus.LATE -> "Late" to Warning
    AttendanceStatus.ABSENT -> "Absent" to Danger
    else -> "On Leave" to Purple
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(CardBackground).border(1.dp, DividerColor, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Navy, modifier = Modifier.size(20.dp)) }
}

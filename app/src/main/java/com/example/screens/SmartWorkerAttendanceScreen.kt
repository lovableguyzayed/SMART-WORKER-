package com.example.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DividerColor
import com.example.ui.theme.IconBlueBg
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.PurpleBg
import com.example.ui.theme.Success
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.theme.WarningBg
import com.example.ui.vm.AttendanceViewModel
import com.example.util.CsvExporter
import com.example.util.LocalImage
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Daily attendance workspace. Adapts the high-fidelity construction-site design
 * (company header, KPI cards, mark-attendance methods, worker table, summary
 * strip) onto the app's real data: every figure is derived from Room via
 * [AttendanceViewModel], and every control performs a real action.
 */
@Composable
fun SmartWorkerAttendanceScreen(vm: AttendanceViewModel, user: User, onOpenQuickMark: () -> Unit = {}) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val date by vm.date.collectAsStateLifecycle()
    val rows by vm.rows.collectAsStateLifecycle()
    val totals by vm.totals.collectAsStateLifecycle()
    val search by vm.search.collectAsStateLifecycle()
    val company by container.catalogRepository.company.collectAsStateWithLifecycle(initialValue = null)

    var pendingWorker by remember { mutableStateOf<Worker?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showBulk by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) } // 0 Worker List, 1 Summary

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE") }

    val overtimeCount = rows.count { (it.record?.overtimeMinutes ?: 0) > 0 }
    val expected = totals.expected
    val marked = totals.present + totals.absent + totals.late + totals.leave

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SwTopBar(title = "Attendance") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CompanyHeader(company?.name ?: "SmartWorker", company?.logo, marked, expected) }
            item { DateSelector(date.format(dateFmt), onPrev = { vm.shiftDate(-1) }, onNext = { vm.shiftDate(1) }, onToday = { vm.today() }) }
            item { KpiCards(totals.present, totals.absent, totals.late, totals.leave, overtimeCount, expected) }
            item {
                MarkAttendanceCard(
                    isAdmin = user.isAdmin,
                    onQuickMark = onOpenQuickMark,
                    onBulkPresent = { showBulk = true },
                )
            }
            item { AttendanceTabs(tab) { tab = it } }

            if (tab == 0) {
                item {
                    SearchExportRow(
                        query = search,
                        onQueryChange = vm::setSearch,
                        onExport = {
                            val records = rows.mapNotNull { it.record }
                            if (records.isEmpty()) {
                                toast = "Nothing to export — no attendance is marked for this day yet."
                            } else {
                                scope.launch {
                                    val byId = rows.associate { it.worker.id to it.worker }
                                    val csv = CsvExporter.attendanceCsv(records, byId)
                                    CsvExporter.share(context, "attendance_$date.csv", csv)
                                }
                            }
                        },
                    )
                }
                item { WorkerListHeaderRow() }
                if (rows.isEmpty()) {
                    item {
                        Text(
                            "No active workers to show.",
                            fontSize = 13.sp, color = TextSecondary,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(rows, key = { it.worker.id }) { row ->
                        WorkerRow(
                            worker = row.worker,
                            record = row.record,
                            onCall = { dial(context, row.worker.phone) },
                            onMark = { pendingWorker = row.worker },
                        )
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    }
                }
            } else {
                item { SummaryStrip(expected, totals.present, totals.absent, totals.late, totals.leave, overtimeCount) }
            }
        }
    }

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
            text = { Text("This sets every active worker to Present for ${date.format(dateFmt)}. Existing records are updated.") },
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

// ── Company / summary header ──────────────────────────────────────────────────
@Composable
private fun CompanyHeader(name: String, logoPath: String?, marked: Int, expected: Int) {
    val rate = if (expected > 0) marked * 100 / expected else 0
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(IconBlueBg),
                contentAlignment = Alignment.Center,
            ) {
                LocalImage(
                    path = logoPath,
                    contentDescription = "Company logo",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    fallback = { Icon(Icons.Outlined.Apartment, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp)) },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("$marked of $expected marked today", fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$rate%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Text("attendance", fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}

// ── Date selector ─────────────────────────────────────────────────────────────
@Composable
private fun DateSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SquareIconButton(Icons.Filled.ChevronLeft, "Previous day", onPrev)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SquareIconButton(Icons.Filled.ChevronRight, "Next day", onNext)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, PrimaryBlue),
            modifier = Modifier.clickable(onClick = onToday),
        ) {
            Box(Modifier.height(40.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Today", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SquareIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = Navy)
        }
    }
}

// ── KPI cards ─────────────────────────────────────────────────────────────────
@Composable
private fun KpiCards(present: Int, absent: Int, late: Int, leave: Int, overtime: Int, expected: Int) {
    data class Kpi(val title: String, val count: Int, val color: Color, val bg: Color, val icon: ImageVector)
    val cards = listOf(
        Kpi("Present", present, Success, SuccessBg, Icons.Outlined.HowToReg),
        Kpi("Absent", absent, Danger, DangerBg, Icons.Outlined.PersonOff),
        Kpi("Late", late, Warning, WarningBg, Icons.Outlined.Schedule),
        Kpi("On Leave", leave, Purple, PurpleBg, Icons.Outlined.PersonOutline),
        Kpi("Overtime", overtime, PrimaryBlue, IconBlueBg, Icons.Outlined.MoreTime),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cards) { kpi ->
            val pct = if (expected > 0) "%.1f%%".format(kpi.count * 100.0 / expected) else "0%"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
                modifier = Modifier.width(104.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(kpi.bg),
                            contentAlignment = Alignment.Center,
                        ) { Icon(kpi.icon, null, tint = kpi.color, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(6.dp))
                        Text(kpi.title, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(kpi.count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(pct, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = kpi.color)
                }
            }
        }
    }
}

// ── Mark attendance methods ───────────────────────────────────────────────────
@Composable
private fun MarkAttendanceCard(isAdmin: Boolean, onQuickMark: () -> Unit, onBulkPresent: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Mark Attendance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text("Scan a code, look up by worker ID, or tap a worker below.", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MethodCard("QR / ID Scan", Icons.Outlined.QrCodeScanner, PrimaryBlue, Modifier.weight(1f), onQuickMark)
                MethodCard("By Worker ID", Icons.Outlined.HowToReg, Success, Modifier.weight(1f), onQuickMark)
                if (isAdmin) {
                    MethodCard("All Present", Icons.Outlined.DoneAll, Warning, Modifier.weight(1f), onBulkPresent)
                }
            }
        }
    }
}

@Composable
private fun MethodCard(title: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, DividerColor),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy,
                maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── Tabs ──────────────────────────────────────────────────────────────────────
@Composable
private fun AttendanceTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Worker List", "Summary")
    Row(Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, title ->
            val active = index == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title, fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) PrimaryBlue else TextSecondary,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                HorizontalDivider(
                    color = if (active) PrimaryBlue else DividerColor,
                    thickness = if (active) 3.dp else 1.dp,
                )
            }
        }
    }
}

// ── Search + export ───────────────────────────────────────────────────────────
@Composable
private fun SearchExportRow(query: String, onQueryChange: (String) -> Unit, onExport: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by name or ID", fontSize = 13.sp, color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DividerColor,
            ),
            modifier = Modifier.weight(1f).height(52.dp),
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier.clickable(onClick = onExport),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.FileDownload, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy)
            }
        }
    }
}

// ── Worker table ──────────────────────────────────────────────────────────────
@Composable
private fun WorkerListHeaderRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Worker", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1.7f))
        Text("Status", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.9f))
        Text("In", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.8f))
        Text("Out", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.8f))
        Text("Hrs", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.9f))
        Text("", modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun WorkerRow(worker: Worker, record: AttendanceRecord?, onCall: () -> Unit, onMark: () -> Unit) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val status = record?.status
    val (label, color, bg) = statusVisual(status)

    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onMark),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1.7f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(AvatarBlueBg), contentAlignment = Alignment.Center) {
                LocalImage(
                    path = worker.profileImage,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    fallback = { Text(worker.fullName.take(1), fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(worker.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${worker.workerCode} • ${worker.position}", fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.weight(0.9f)) { StatusChip(label, color, bg) }
        Text(
            record?.checkInTime?.format(timeFmt) ?: "--", fontSize = 11.sp,
            color = if (status == AttendanceStatus.LATE) Warning else Navy,
            modifier = Modifier.weight(0.8f),
        )
        Text(record?.checkOutTime?.format(timeFmt) ?: "--", fontSize = 11.sp, color = Navy, modifier = Modifier.weight(0.8f))
        Column(Modifier.weight(0.9f)) {
            Text(hoursLabel(record), fontSize = 11.sp, color = Navy)
            val ot = record?.overtimeMinutes ?: 0
            if (ot > 0) Text("+${hm(ot)}", fontSize = 10.sp, color = Success)
        }
        Row(Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
            if (worker.phone.isNotBlank()) {
                Icon(
                    Icons.Outlined.Call, "Call ${worker.fullName}",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp).clickable(onClick = onCall),
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Filled.MoreVert, "Mark ${worker.fullName}",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp).clickable(onClick = onMark),
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ── Summary strip ─────────────────────────────────────────────────────────────
@Composable
private fun SummaryStrip(total: Int, present: Int, absent: Int, late: Int, leave: Int, overtime: Int) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                SummaryItem("Total Workers", total, null, Icons.Outlined.CalendarMonth, PrimaryBlue, total, Modifier.weight(1f))
                SummaryItem("Present", present, present, Icons.Outlined.HowToReg, Success, total, Modifier.weight(1f))
                SummaryItem("Absent", absent, absent, Icons.Outlined.PersonOff, Danger, total, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryItem("Late", late, late, Icons.Outlined.Schedule, Warning, total, Modifier.weight(1f))
                SummaryItem("On Leave", leave, leave, Icons.Outlined.PersonOutline, Purple, total, Modifier.weight(1f))
                SummaryItem("Overtime", overtime, overtime, Icons.Outlined.MoreTime, PrimaryBlue, total, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: Int, pctOf: Int?, icon: ImageVector, tint: Color, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, DividerColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                if (pctOf != null && total > 0) {
                    Spacer(Modifier.width(3.dp))
                    Text("(%.0f%%)".format(pctOf * 100.0 / total), fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ── Mark dialog (worker tap / more) ───────────────────────────────────────────
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

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun statusVisual(status: String?): Triple<String, Color, Color> = when (status) {
    AttendanceStatus.PRESENT -> Triple("Present", Success, SuccessBg)
    AttendanceStatus.LATE -> Triple("Late", Warning, WarningBg)
    AttendanceStatus.ABSENT -> Triple("Absent", Danger, DangerBg)
    AttendanceStatus.LEAVE -> Triple("Leave", Purple, PurpleBg)
    else -> Triple("—", TextSecondary, DividerColor)
}

private fun hoursLabel(record: AttendanceRecord?): String {
    val minutes = record?.workedMinutes ?: 0
    return if (minutes <= 0) "--" else hm(minutes)
}

private fun hm(minutes: Int): String = "%02dh %02dm".format(minutes / 60, minutes % 60)

private fun dial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }
}

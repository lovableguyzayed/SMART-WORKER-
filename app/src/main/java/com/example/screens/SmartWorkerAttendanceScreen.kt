package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.ui.EmptyState
import com.example.ui.LocalAppContainer
import com.example.ui.MeterRow
import com.example.ui.StatTile
import com.example.ui.SwCard
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Forest
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.OnBright
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.AttendanceViewModel
import com.example.util.CsvExporter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Attendance — a scrollable day strip across the top (the reference decks' date
 * selector), the day's marking progress as a meter, colour-blocked totals, then
 * one row per worker tinted by status.
 */
@Composable
fun SmartWorkerAttendanceScreen(
    vm: AttendanceViewModel,
    user: User,
    onOpenQuickMark: () -> Unit = {},
) {
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

    AttendanceContent(
        date = date,
        rows = rows,
        totals = totals,
        search = search,
        companyName = company?.name,
        isAdmin = user.isAdmin,
        onSetDate = vm::setDate,
        onSearch = vm::setSearch,
        onQuickMark = onOpenQuickMark,
        onBulkPresent = { showBulk = true },
        onMarkWorker = { pendingWorker = it },
        onCall = { dial(context, it.phone) },
        onExport = {
            val records = rows.mapNotNull { it.record }
            if (records.isEmpty()) {
                toast = "Nothing to export — no attendance is marked for this day yet."
            } else {
                scope.launch {
                    val byId = rows.associate { it.worker.id to it.worker }
                    CsvExporter.share(context, "attendance_$date.csv", CsvExporter.attendanceCsv(records, byId))
                }
            }
        },
    )

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
            containerColor = Surface2,
            titleContentColor = OnDark,
            textContentColor = OnDarkMuted,
            title = { Text("Mark all present?") },
            text = {
                Text(
                    "This sets every active worker to Present for " +
                        "${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}. " +
                        "Existing records are updated.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.bulkMark(user, AttendanceStatus.PRESENT) { toast = it }
                    showBulk = false
                }) { Text("Confirm", color = Lime) }
            },
            dismissButton = { TextButton(onClick = { showBulk = false }) { Text("Cancel", color = OnDarkMuted) } },
        )
    }

    toast?.let { message ->
        AlertDialog(
            onDismissRequest = { toast = null },
            containerColor = Surface2,
            textContentColor = OnDark,
            confirmButton = { TextButton(onClick = { toast = null }) { Text("OK", color = Flame) } },
            text = { Text(message) },
        )
    }
}

/** Presentation half — renders from plain state so it can be screenshotted. */
@Composable
internal fun AttendanceContent(
    date: LocalDate,
    rows: List<AttendanceViewModel.Row>,
    totals: AttendanceViewModel.Totals,
    search: String,
    companyName: String?,
    isAdmin: Boolean,
    onSetDate: (LocalDate) -> Unit,
    onSearch: (String) -> Unit,
    onQuickMark: () -> Unit,
    onBulkPresent: () -> Unit,
    onMarkWorker: (Worker) -> Unit,
    onCall: (Worker) -> Unit,
    onExport: () -> Unit,
) {
    val marked = totals.present + totals.absent + totals.late + totals.leave
    val expected = totals.expected
    val pct = if (expected > 0) marked * 100 / expected else 0

    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            companyName ?: "SmartWorker",
                            style = SwType.Caption, color = OnDarkMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("Attendance", style = SwType.Headline.copy(fontSize = 26.sp), color = OnDark)
                    }
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Surface3)
                            .clickable(onClick = onExport),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Download, "Export CSV", tint = OnDark, modifier = Modifier.size(19.dp))
                    }
                }
            }

            // ── Day strip ──
            item { DayStrip(selected = date, onSelect = onSetDate) }

            // ── Marking progress ──
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        MeterRow(
                            label = if (expected == 0) "No active workers" else "$marked of $expected marked",
                            percent = pct,
                            accent = if (pct >= 100) Lime else Flame,
                        )
                    }
                }
            }

            // ── Totals ──
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(totals.present.toString(), "Present", Modifier.weight(1f).height(100.dp), container = Lime)
                        StatTile(totals.absent.toString(), "Absent", Modifier.weight(1f).height(100.dp), container = Coral)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(totals.late.toString(), "Late", Modifier.weight(1f).height(100.dp), container = Sun)
                        StatTile(totals.leave.toString(), "On leave", Modifier.weight(1f).height(100.dp), container = Volt)
                    }
                }
            }

            // ── Marking actions ──
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MarkAction(
                        "Scan QR", "Mark by worker ID", Icons.Filled.QrCodeScanner,
                        Flame, White, Modifier.weight(1f), onQuickMark,
                    )
                    if (isAdmin) {
                        MarkAction(
                            "All present", "One tap for everyone", Icons.Filled.DoneAll,
                            Forest, White, Modifier.weight(1f), onBulkPresent,
                        )
                    }
                }
            }

            // ── Search ──
            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Surface3)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = OnDarkMuted, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text("Search worker", style = SwType.Body, color = OnDarkFaint)
                        }
                        BasicTextField(
                            value = search,
                            onValueChange = onSearch,
                            singleLine = true,
                            textStyle = SwType.Body.copy(color = OnDark),
                            cursorBrush = SolidColor(Flame),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (search.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Close, "Clear search", tint = OnDarkMuted,
                            modifier = Modifier.size(18.dp).clickable { onSearch("") },
                        )
                    }
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Groups,
                        if (search.isBlank()) "No active workers" else "No match",
                        if (search.isBlank()) {
                            "Add workers to start marking attendance."
                        } else {
                            "Nothing matches “$search”."
                        },
                    )
                }
            } else {
                items(rows, key = { it.worker.id }) { row ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        AttendanceRow(
                            worker = row.worker,
                            record = row.record,
                            onCall = { onCall(row.worker) },
                            onMark = { onMarkWorker(row.worker) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal day selector. Shows a fortnight ending a few days after the
 * selected date so the current day sits roughly centre, with today called out.
 */
@Composable
private fun DayStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val today = remember { LocalDate.now() }
    val days = remember(selected) { (-5L..3L).map { selected.plusDays(it) } }
    // The window re-centres on each selection, so the selected pill always sits
    // at the same index — scroll it into view or it hides at the right edge.
    val scroll = rememberScrollState()
    val stepPx = with(LocalDensity.current) { (58.dp + 8.dp).roundToPx() }
    val selectedIndex = days.indexOf(selected)
    LaunchedEffect(selected) {
        if (selectedIndex >= 0) {
            scroll.animateScrollTo(((selectedIndex - 2) * stepPx).coerceAtLeast(0))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            val isToday = day == today
            val bg = when {
                isSelected -> Flame
                isToday -> Surface3
                else -> Surface1
            }
            val fg = if (isSelected) White else OnDark
            Column(
                Modifier
                    .width(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .clickable { onSelect(day) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.format(DateTimeFormatter.ofPattern("EEE")).uppercase(),
                    style = SwType.Label,
                    color = if (isSelected) White.copy(alpha = 0.85f) else OnDarkMuted,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    day.dayOfMonth.toString().padStart(2, '0'),
                    style = SwType.Title.copy(fontSize = 17.sp),
                    color = fg,
                )
                if (isToday) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Today",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) White else Flame,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkAction(
    label: String,
    sub: String,
    icon: ImageVector,
    container: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SwCard(modifier.height(104.dp), container = container, onClick = onClick) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(23.dp))
            Column {
                Text(label, style = SwType.Title.copy(fontSize = 15.sp), color = foreground, maxLines = 1)
                Text(
                    sub, style = SwType.Caption.copy(fontSize = 11.sp),
                    color = foreground.copy(alpha = 0.72f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AttendanceRow(
    worker: Worker,
    record: AttendanceRecord?,
    onCall: () -> Unit,
    onMark: () -> Unit,
) {
    val accent = record?.let { statusAccent(it.status) } ?: OnDarkFaint
    SwCard(Modifier.fillMaxWidth(), onClick = onMark) {
        Row(
            Modifier.fillMaxWidth().padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(5.dp).height(66.dp).background(accent))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f).padding(vertical = 13.dp)) {
                Text(
                    worker.fullName, style = SwType.Title.copy(fontSize = 15.sp), color = OnDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        worker.workerCode.takeIf { it.isNotBlank() },
                        // An absent or on-leave worker has no meaningful arrival
                        // time, even if a stale one is still on the record.
                        record?.checkInTime
                            ?.takeIf { record.status == AttendanceStatus.PRESENT || record.status == AttendanceStatus.LATE }
                            ?.format(DateTimeFormatter.ofPattern("hh:mm a")),
                        record?.lateMinutes?.takeIf { it > 0 }?.let { "$it min late" },
                        record?.overtimeMinutes?.takeIf { it > 0 }?.let { "OT ${it / 60}h ${it % 60}m" },
                    ).joinToString("  ·  ").ifBlank { worker.position },
                    style = SwType.Caption, color = OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Surface3)
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Phone, "Call ${worker.fullName}", tint = OnDarkMuted, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier.clip(CircleShape)
                    .background(if (record == null) Surface3 else accent.copy(alpha = 0.18f))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    record?.status?.replaceFirstChar { it.uppercase() } ?: "Mark",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record == null) OnDarkMuted else accent,
                )
            }
        }
    }
}

@Composable
private fun MarkDialog(
    worker: Worker,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = OnDark,
        title = {
            Column {
                Text(worker.fullName, style = SwType.Title.copy(fontSize = 17.sp), color = OnDark)
                Text(worker.workerCode, style = SwType.Caption, color = OnDarkMuted)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                StatusChoice("Present", Lime) { onPick(AttendanceStatus.PRESENT) }
                StatusChoice("Late", Sun) { onPick(AttendanceStatus.LATE) }
                StatusChoice("Absent", Coral) { onPick(AttendanceStatus.ABSENT) }
                if (isAdmin) StatusChoice("On leave", Volt) { onPick(AttendanceStatus.LEAVE) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnDarkMuted) } },
    )
}

@Composable
private fun StatusChoice(label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Checklist, null, tint = OnBright, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = SwType.Title.copy(fontSize = 15.sp), color = OnBright)
    }
}

private fun dial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:$phone"),
            ),
        )
    }
}

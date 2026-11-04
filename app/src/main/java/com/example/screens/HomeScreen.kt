package com.example.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClosureDay
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.ui.LocalAppContainer
import com.example.ui.SectionHeader
import com.example.ui.StatTile
import com.example.ui.SwCard
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Coral
import com.example.ui.theme.Cream
import com.example.ui.theme.Danger
import com.example.ui.theme.Flame
import com.example.ui.theme.Forest
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.Mint
import com.example.ui.theme.OnBright
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.DashboardViewModel
import com.example.util.LocalImage
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Dashboard — a bento grid in the house style: one hero figure, a tile grid of
 * supporting stats, a conversational prompt when something needs doing, then
 * colour-coded action tiles and today's attendance ledger.
 */
@Composable
fun HomeScreen(
    vm: DashboardViewModel,
    user: User,
    onOpenNotifications: () -> Unit,
    onQuickAction: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val state by vm.state.collectAsStateLifecycle()
    val upcomingClosures by vm.upcomingClosures.collectAsStateLifecycle()
    val company by container.catalogRepository.company.collectAsStateWithLifecycle(initialValue = null)
    val unread by container.catalogRepository.unreadCount.collectAsStateWithLifecycle(initialValue = 0)

    val unmarked = (state.totalWorkers - state.marked).coerceAtLeast(0)

    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DashboardHeader(
                    companyName = company?.name ?: "SmartWorker",
                    logo = company?.logo,
                    firstName = user.fullName.trim().split(" ").firstOrNull().orEmpty(),
                    unread = unread,
                    onNotifications = onOpenNotifications,
                    onProfile = { onQuickAction("more") },
                )
            }

            // ── Hero: the one figure the day is judged on ──
            item {
                HeroTile(
                    label = "Present today",
                    value = state.present.toString(),
                    caption = buildString {
                        append("of ${state.totalWorkers} workers")
                        if (state.totalWorkers > 0) append("  ·  ${state.attendancePct.toInt()}% attendance")
                    },
                    onClick = { onQuickAction("attendance") },
                )
            }

            // ── Supporting stats ──
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = state.absent.toString(),
                            label = "Absent",
                            container = Coral,
                            modifier = Modifier.weight(1f).height(108.dp),
                            onClick = { onQuickAction("attendance") },
                        )
                        StatTile(
                            value = state.late.toString(),
                            label = "Late",
                            container = Sun,
                            modifier = Modifier.weight(1f).height(108.dp),
                            onClick = { onQuickAction("attendance") },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = state.totalWorkers.toString(),
                            label = "Total workers",
                            container = Volt,
                            modifier = Modifier.weight(1f).height(108.dp),
                            onClick = { onQuickAction("workers") },
                        )
                        StatTile(
                            value = state.leave.toString(),
                            label = "On leave",
                            container = Surface1,
                            modifier = Modifier.weight(1f).height(108.dp),
                            onClick = { onQuickAction("attendance") },
                        )
                    }
                }
            }

            // ── Conversational prompt ──
            item {
                InsightTile(
                    message = when {
                        state.totalWorkers == 0 ->
                            "No workers yet. Add your first worker to start tracking attendance."
                        unmarked > 0 ->
                            "$unmarked ${if (unmarked == 1) "worker is" else "workers are"} still unmarked today. " +
                                "You're not starting from scratch — you're just finishing."
                        else ->
                            "All ${state.totalWorkers} workers marked today. Nothing left on the attendance list."
                    },
                    actionLabel = if (state.totalWorkers == 0) "Add worker" else if (unmarked > 0) "Mark now" else null,
                    onAction = {
                        onQuickAction(if (state.totalWorkers == 0) "worker_form" else "attendance")
                    },
                )
            }

            // ── Action tiles ──
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader("Quick actions")
                    val actions = buildList {
                        add(ActionSpec("Scan QR", "Mark by worker ID", Icons.Filled.QrCodeScanner, Flame, White, "quick_mark"))
                        add(ActionSpec("Attendance", "Mark or review", Icons.Filled.HowToReg, Forest, White, "attendance"))
                        if (user.isAdmin) {
                            add(ActionSpec("Payroll", "Wages & payslips", Icons.Filled.ReceiptLong, Cream, OnBright, "payroll"))
                            add(ActionSpec("Add worker", "New record", Icons.Filled.PersonAdd, Surface1, OnDark, "worker_form"))
                            add(ActionSpec("Transactions", "Advances & bonuses", Icons.Filled.Payments, Surface1, OnDark, "transactions"))
                            add(ActionSpec("Reports", "Export & analyse", Icons.Filled.Workspaces, Surface1, OnDark, "reports"))
                            add(ActionSpec("Closures", "Holidays & shutdowns", Icons.Filled.EventBusy, Surface1, OnDark, "closures"))
                        }
                    }
                    actions.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { spec ->
                                ActionTile(spec, Modifier.weight(1f)) { onQuickAction(spec.destination) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Today's ledger ──
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        "Today's attendance",
                        action = if (state.recent.isNotEmpty()) "See all" else null,
                        onAction = { onQuickAction("attendance") },
                    )
                }
            }
            if (state.recent.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SwCard(Modifier.fillMaxWidth()) {
                            Text(
                                "Nothing marked yet today.",
                                style = SwType.Body,
                                color = OnDarkMuted,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                }
            } else {
                items(state.recent.size) { i ->
                    val (record, worker) = state.recent[i]
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        LedgerRow(worker, record) { onQuickAction("attendance") }
                    }
                }
            }

            // ── Upcoming closures ──
            if (upcomingClosures.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SectionHeader("Upcoming closures")
                    }
                }
                items(upcomingClosures.size) { i ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        ClosureRow(upcomingClosures[i])
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private data class ActionSpec(
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val container: Color,
    val foreground: Color,
    val destination: String,
)

@Composable
private fun DashboardHeader(
    companyName: String,
    logo: String?,
    firstName: String,
    unread: Int,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
) {
    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Surface3)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            LocalImage(logo, "Company logo", Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))) {
                Icon(Icons.Filled.Business, null, tint = Flame, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (firstName.isBlank()) greeting else "$greeting, $firstName",
                style = SwType.Caption,
                color = OnDarkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                companyName,
                style = SwType.Headline.copy(fontSize = 21.sp),
                color = OnDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.semantics {
                contentDescription = if (unread > 0) "Notifications, $unread unread" else "Notifications"
            },
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Surface3).clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Notifications, null, tint = OnDark, modifier = Modifier.size(20.dp))
            }
            if (unread > 0) {
                Box(
                    Modifier.size(18.dp).background(Danger, CircleShape).align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (unread > 9) "9+" else "$unread",
                        fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Full-width hero figure — the trakmac "calories today" tile, our numbers. */
@Composable
private fun HeroTile(label: String, value: String, caption: String, onClick: () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) {
        SwCard(Modifier.fillMaxWidth(), container = Flame, onClick = onClick) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label.uppercase(),
                        style = SwType.Label,
                        color = White.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(value, style = SwType.StatHuge, color = White)
                Spacer(Modifier.height(2.dp))
                Text(caption, style = SwType.Caption, color = White.copy(alpha = 0.88f))
            }
        }
    }
}

/** The calm mint tile that tells you what still needs doing. */
@Composable
private fun InsightTile(message: String, actionLabel: String?, onAction: () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) {
        SwCard(Modifier.fillMaxWidth(), container = Mint) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("»", style = SwType.Title, color = OnBright.copy(alpha = 0.55f))
                Spacer(Modifier.height(4.dp))
                Text(message, style = SwType.Body.copy(fontSize = 14.5.sp), color = OnBright)
                if (actionLabel != null) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(OnBright)
                            .clickable(onClick = onAction)
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        Text(actionLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Mint)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(spec: ActionSpec, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SwCard(modifier.height(112.dp), container = spec.container, onClick = onClick) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(spec.icon, null, tint = spec.foreground, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    spec.label, style = SwType.Title.copy(fontSize = 15.sp), color = spec.foreground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    spec.sub, style = SwType.Caption.copy(fontSize = 11.5.sp),
                    color = spec.foreground.copy(alpha = 0.7f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Attendance row, colour-blocked by status like the task-board reference. */
@Composable
private fun LedgerRow(worker: Worker, record: AttendanceRecord, onClick: () -> Unit) {
    val accent = statusAccent(record.status)
    SwCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status spine — reads at a glance down the list.
            Box(Modifier.width(5.dp).height(62.dp).background(accent))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    worker.fullName, style = SwType.Title.copy(fontSize = 15.sp), color = OnDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        worker.workerCode.takeIf { it.isNotBlank() },
                        record.checkInTime?.format(DateTimeFormatter.ofPattern("hh:mm a")),
                        record.lateMinutes.takeIf { it > 0 }?.let { "$it min late" },
                    ).joinToString("  ·  "),
                    style = SwType.Caption, color = OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.clip(CircleShape).background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    record.status.replaceFirstChar { it.uppercase() },
                    fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = accent,
                )
            }
        }
    }
}

@Composable
private fun ClosureRow(closure: ClosureDay) {
    SwCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Sun),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    closure.date.dayOfMonth.toString(),
                    style = SwType.Title, color = OnBright,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    closure.reason, style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${closure.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))}  ·  " +
                        closure.type.replaceFirstChar { it.uppercase() },
                    style = SwType.Caption, color = OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (!closure.allowAttendance) {
                Box(
                    Modifier.clip(CircleShape).background(Coral.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("No marking", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Coral)
                }
            }
        }
    }
}

internal fun statusAccent(status: String): Color = when (status) {
    AttendanceStatus.PRESENT -> Lime
    AttendanceStatus.LATE -> Sun
    AttendanceStatus.ABSENT -> Coral
    AttendanceStatus.LEAVE -> Volt
    else -> OnDarkMuted
}

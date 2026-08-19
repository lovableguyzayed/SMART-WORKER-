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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.theme.White
import com.example.ui.vm.DashboardViewModel
import com.example.util.LocalImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Stat-card gradients, matching the web dashboard's Tailwind pairs.
private val Blue500 = Color(0xFF3B82F6)
private val Blue600 = Color(0xFF2563EB)
private val Green500 = Color(0xFF22C55E)
private val Green600 = Color(0xFF16A34A)
private val Purple500 = Color(0xFFA855F7)
private val Purple600 = Color(0xFF9333EA)
private val Orange500 = Color(0xFFF97316)
private val Orange600 = Color(0xFFEA580C)
private val Indigo500 = Color(0xFF6366F1)
private val Teal500 = Color(0xFF14B8A6)

/**
 * Dashboard — mirrors the web app's `dashboard.html`: gradient header with a
 * welcome card, 2×2 quick-stat tiles, a payroll shortcut, the quick-action
 * grid, today's attendance ledger and upcoming closures.
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

    Scaffold(containerColor = BackgroundColor) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                DashboardHeader(
                    companyName = company?.name ?: "SmartWorker",
                    logo = company?.logo,
                    firstName = user.fullName.trim().split(" ").firstOrNull().orEmpty(),
                    unread = unread,
                    isAdmin = user.isAdmin,
                    onNotifications = onOpenNotifications,
                    onProfile = { onQuickAction("more") },
                )
            }

            // ── Quick stats (2 × 2) ──
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total Workers", state.totalWorkers.toString(), Icons.Filled.Groups, Blue500, Blue600, Modifier.weight(1f)) { onQuickAction("workers") }
                        StatCard("Present Today", state.present.toString(), Icons.Filled.CheckCircle, Green500, Green600, Modifier.weight(1f)) { onQuickAction("attendance") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Attendance Rate", "${state.attendancePct.toInt()}%", Icons.Filled.PieChart, Purple500, Purple600, Modifier.weight(1f)) { onQuickAction("attendance") }
                        StatCard("Absent Today", state.absent.toString(), Icons.Filled.PersonOff, Orange500, Orange600, Modifier.weight(1f)) { onQuickAction("attendance") }
                    }
                }
            }

            // ── Payroll shortcut (admin) ──
            if (user.isAdmin) {
                item {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(PrimaryBlue, DarkBlue)))
                            .clickable { onQuickAction("payroll") }
                            .padding(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.ReceiptLong, null, tint = White, modifier = Modifier.size(24.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Payroll", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text("Calculate & track monthly wages", color = White.copy(alpha = 0.9f), fontSize = 12.5.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Quick actions ──
            item {
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = CardBorder,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Actions", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                        Spacer(Modifier.height(14.dp))
                        val actions = buildList {
                            if (user.isAdmin) add(Triple("Add Worker", Icons.Filled.PersonAdd, PrimaryBlue) to "worker_form")
                            add(Triple("Mark Attendance", Icons.Filled.HowToReg, Green500) to "attendance")
                            add(Triple("QR Scanner", Icons.Filled.QrCodeScanner, Indigo500) to "quick_mark")
                            if (user.isAdmin) {
                                add(Triple("Transactions", Icons.Filled.Payments, Teal500) to "transactions")
                                add(Triple("Reports", Icons.Filled.Workspaces, Purple500) to "reports")
                                add(Triple("Closures", Icons.Filled.EventBusy, Orange500) to "closures")
                            }
                        }
                        actions.chunked(2).forEach { pair ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                pair.forEach { (spec, dest) ->
                                    val (label, icon, color) = spec
                                    ActionButton(label, icon, color, Modifier.weight(1f)) { onQuickAction(dest) }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Today's attendance ledger ──
            item {
                Card(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = CardBorder,
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Today's Attendance", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy, modifier = Modifier.weight(1f))
                            Text(
                                "VIEW ALL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onQuickAction("attendance") }.padding(4.dp),
                            )
                        }
                        // Column headers
                        Row(
                            Modifier.fillMaxWidth().background(BackgroundColor).padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            LedgerHead("WORKER", Modifier.weight(1f), TextAlign.Start)
                            LedgerHead("IN", Modifier.width(52.dp), TextAlign.Center)
                            LedgerHead("OUT", Modifier.width(52.dp), TextAlign.Center)
                            LedgerHead("STATUS", Modifier.width(64.dp), TextAlign.End)
                        }
                        if (state.recent.isEmpty()) {
                            Text(
                                "No attendance marked today yet.",
                                fontSize = 13.sp, color = TextSecondary,
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            state.recent.forEach { (record, worker) ->
                                LedgerRow(worker, record) { onQuickAction("attendance") }
                                androidx.compose.material3.HorizontalDivider(color = SubtleDivider)
                            }
                        }
                    }
                }
            }

            // ── Upcoming closures ──
            if (upcomingClosures.isNotEmpty()) {
                item {
                    Card(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Upcoming Closures", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                            Spacer(Modifier.height(10.dp))
                            upcomingClosures.take(5).forEach { c ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Warning.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Filled.CalendarMonth, null, tint = Warning, modifier = Modifier.size(18.dp)) }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.reason, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            c.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                                            fontSize = 11.5.sp, color = TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    companyName: String,
    logo: String?,
    firstName: String,
    unread: Int,
    isAdmin: Boolean,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1E3A8A), DarkBlue)))
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalImage(logo, "Logo", Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))) {
                        Icon(Icons.Filled.Business, null, tint = White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(companyName, color = White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Dashboard Overview", color = White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
                if (isAdmin) {
                    Box {
                        HeaderIconButton(Icons.Filled.Notifications, "Notifications", onNotifications)
                        if (unread > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                    .size(17.dp).clip(CircleShape).background(Danger),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (unread > 9) "9+" else unread.toString(),
                                    color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }
                HeaderIconButton(Icons.Filled.Person, "Profile", onProfile)
            }

            Spacer(Modifier.height(16.dp))
            // Glass welcome card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(White.copy(alpha = 0.15f))
                    .padding(16.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (firstName.isBlank()) "Welcome back!" else "Welcome back, $firstName!",
                        color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                        color = White.copy(alpha = 0.9f), fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(White.copy(alpha = 0.15f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = White, modifier = Modifier.size(20.dp)) }
}

// ── Pieces ────────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(from, to)))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(value, color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(label, color = White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = White, modifier = Modifier.size(21.dp)) }
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label, color = White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LedgerHead(text: String, modifier: Modifier = Modifier, align: TextAlign) {
    Text(
        text, modifier = modifier, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        color = TextSecondary, textAlign = align,
    )
}

@Composable
private fun LedgerRow(worker: Worker, record: AttendanceRecord, onClick: () -> Unit) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.US) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PrimaryBlue, DarkBlue))),
                contentAlignment = Alignment.Center,
            ) {
                LocalImage(worker.profileImage, null, Modifier.size(32.dp).clip(CircleShape)) {
                    Text(worker.fullName.take(1).uppercase(), color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(worker.fullName, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(worker.workerCode, fontSize = 10.5.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            record.checkInTime?.format(timeFmt) ?: "–",
            modifier = Modifier.width(52.dp), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (record.checkInTime != null) Success else TextSecondary, textAlign = TextAlign.Center,
        )
        Text(
            record.checkOutTime?.format(timeFmt) ?: if (record.checkInTime != null) "shift" else "–",
            modifier = Modifier.width(52.dp), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (record.checkOutTime != null) Navy else PrimaryBlue, textAlign = TextAlign.Center,
        )
        Row(Modifier.width(64.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            val (label, color) = when (record.status) {
                AttendanceStatus.PRESENT -> "Present" to Success
                AttendanceStatus.LATE -> "Late" to Warning
                AttendanceStatus.ABSENT -> "Absent" to Danger
                else -> "Leave" to Purple
            }
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = color, maxLines = 1)
        }
    }
}

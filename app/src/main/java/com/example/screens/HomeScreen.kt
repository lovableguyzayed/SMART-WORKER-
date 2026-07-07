package com.example.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.User
import com.example.ui.CardBorder
import com.example.ui.SectionHeader
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.IconBlueBg
import com.example.ui.theme.IconGreenBg
import com.example.ui.theme.IconPurpleBg
import com.example.ui.theme.IconRedBg
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.vm.DashboardViewModel

@Composable
fun HomeScreen(
    vm: DashboardViewModel,
    user: User,
    onOpenNotifications: () -> Unit,
    onQuickAction: (String) -> Unit,
) {
    val state by vm.state.collectAsStateLifecycle()
    val upcomingClosures by vm.upcomingClosures.collectAsStateLifecycle()

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Smart Worker",
                onSearch = { onQuickAction("workers") },
                onNotifications = onOpenNotifications,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GreetingCard(user.fullName, Modifier.padding(horizontal = 16.dp)) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Today's Overview")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(Icons.Filled.Groups, PrimaryBlue, IconBlueBg, state.totalWorkers.toString(), "All Workers", TextSecondary, Modifier.weight(1f))
                        KpiCard(Icons.Filled.CheckCircle, Success, IconGreenBg, state.present.toString(), "Present", Success, Modifier.weight(1f), "Present")
                        KpiCard(Icons.Filled.PersonOff, Danger, IconRedBg, state.absent.toString(), "Absent", Danger, Modifier.weight(1f), "Absent")
                        KpiCard(Icons.Filled.EventBusy, Purple, IconPurpleBg, state.leave.toString(), "On Leave", Purple, Modifier.weight(1f), "Leave")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Quick Actions")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        QuickAction(Icons.Filled.PersonAdd, "Add\nWorker", PrimaryBlue) { onQuickAction("workers") }
                        QuickAction(Icons.Filled.HowToReg, "Mark\nAttendance", Success) { onQuickAction("attendance") }
                        QuickAction(Icons.Filled.TableChart, "View\nAttendance", Warning) { onQuickAction("attendance") }
                        QuickAction(Icons.Filled.Payments, "Payroll", Purple) { onQuickAction("payroll") }
                        QuickAction(Icons.Filled.Assessment, "More", Danger) { onQuickAction("more") }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Attendance Summary")
                    Spacer(Modifier.height(10.dp))
                    AttendanceSummaryCard(state)
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader("Recent Activity")
                    Spacer(Modifier.height(10.dp))
                    if (state.recent.isEmpty()) {
                        Text("No attendance marked today yet.", fontSize = 13.sp, color = TextSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.recent.forEach { (record, worker) ->
                                val (tint, bg) = statusColors(record.status)
                                RecentRow(
                                    icon = statusIcon(record.status),
                                    tint = tint,
                                    bg = bg,
                                    title = "${worker.fullName} — ${record.status.replaceFirstChar { it.uppercase() }}",
                                    time = record.checkInTime?.toLocalTime()?.toString() ?: worker.workerCode,
                                )
                            }
                        }
                    }
                }
            }
            if (upcomingClosures.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("Upcoming Closures")
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingClosures.take(5).forEach { c ->
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    border = CardBorder,
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                c.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy,
                                            )
                                            Text(c.reason, fontSize = 12.sp, color = TextSecondary)
                                        }
                                        com.example.ui.StatusPill(
                                            if (c.allowAttendance) "Open" else "Locked",
                                            if (c.allowAttendance) Success else Danger,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GreetingCard(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.take(1), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Good day, $name 👋", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text("Here's what's happening at your site today.", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun KpiCard(
    icon: ImageVector, iconTint: Color, iconBg: Color, value: String, label: String,
    labelColor: Color, modifier: Modifier = Modifier, topLabel: String? = null,
) {
    Card(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(Modifier.size(30.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            if (topLabel != null) Text(topLabel, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(label, fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp),
            border = CardBorder,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}

@Composable
private fun AttendanceSummaryCard(state: DashboardViewModel.DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val total = state.marked.coerceAtLeast(1)
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                DonutChart(
                    listOf(
                        state.present.toFloat() / total to Success,
                        state.late.toFloat() / total to Warning,
                        state.absent.toFloat() / total to Danger,
                        state.leave.toFloat() / total to Purple,
                    ),
                    Modifier.size(110.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.marked.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Marked", fontSize = 10.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                LegendRow(Success, "Present", state.present, state.marked)
                LegendRow(Warning, "Late", state.late, state.marked)
                LegendRow(Danger, "Absent", state.absent, state.marked)
                LegendRow(Purple, "On Leave", state.leave, state.marked)
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Int, total: Int) {
    val pct = if (total > 0) count * 100.0 / total else 0.0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, fontSize = 13.sp, color = Navy, modifier = Modifier.weight(1f))
        Text(count.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy)
        Text("%.0f%%".format(pct), fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun DonutChart(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val strokeW = size.minDimension * 0.15f
        val radius = (size.minDimension - strokeW) / 2f
        val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
        var startAngle = -90f
        // Track ring so an all-zero day still renders.
        drawArc(Color(0xFFEFF2F7), 0f, 360f, false, style = Stroke(strokeW), topLeft = topLeft, size = arcSize)
        segments.forEach { (fraction, color) ->
            val sweep = fraction * 360f
            if (sweep > 0f) {
                drawArc(color, startAngle, sweep, false, style = Stroke(strokeW, cap = StrokeCap.Butt), topLeft = topLeft, size = arcSize)
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun RecentRow(icon: ImageVector, tint: Color, bg: Color, title: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy, maxLines = 1)
                Text(time, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

private fun statusIcon(status: String): ImageVector = when (status) {
    AttendanceStatus.PRESENT -> Icons.Filled.CheckCircle
    AttendanceStatus.LATE -> Icons.Filled.Schedule
    AttendanceStatus.ABSENT -> Icons.Filled.PersonOff
    else -> Icons.Filled.EventBusy
}

private fun statusColors(status: String): Pair<Color, Color> = when (status) {
    AttendanceStatus.PRESENT -> Success to IconGreenBg
    AttendanceStatus.LATE -> Warning to Color(0xFFFFF7ED)
    AttendanceStatus.ABSENT -> Danger to IconRedBg
    else -> Purple to IconPurpleBg
}

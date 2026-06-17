package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────
//  COLOUR TOKENS  (from design spec)
// ─────────────────────────────────────────────────────────
val SW_PrimaryBlue  = Color(0xFF0D5BFF)
val SW_SuccessGreen = Color(0xFF22C55E)
val SW_WarnOrange   = Color(0xFFF59E0B)
val SW_DangerRed    = Color(0xFFEF4444)
val SW_Purple       = Color(0xFF8B5CF6)
val SW_DarkNavy     = Color(0xFF001B36)
val SW_TextSec      = Color(0xFF64748B)
val SW_Border       = Color(0xFFE2E8F0)
val SW_Background   = Color(0xFFF8FAFC)
val SW_Card         = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────
private data class HomeQuickAction(
    val icon: ImageVector,
    val label: String,
    val tint: Color
)

private data class DonutSegment(val fraction: Float, val color: Color)

private data class RecentActivity(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val time: String
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(onLogout: () -> Unit = {}) {
    Scaffold(
        containerColor = SW_Background,
        topBar    = { SmartWorkerTopBar(onLogout) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SiteSelectorCard(Modifier.padding(horizontal = 16.dp)) }
            item { GreetingCard(name = "Ramesh", modifier = Modifier.padding(horizontal = 16.dp)) }
            item { TodaysOverviewSection(Modifier.padding(horizontal = 16.dp)) }
            item { QuickActionsSection(Modifier.padding(horizontal = 16.dp)) }
            item { AttendanceSummarySection(Modifier.padding(horizontal = 16.dp)) }
            item { RecentActivitiesSection(Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerTopBar(onMenuClick: () -> Unit = {}) {
    Surface(color = SW_Card, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Menu,
                contentDescription = "Menu",
                tint               = SW_DarkNavy,
                modifier           = Modifier.size(24.dp).clickable { onMenuClick() }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text       = "Smart Worker",
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = SW_DarkNavy,
                modifier   = Modifier.weight(1f)
            )

            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = "Search",
                tint               = SW_DarkNavy,
                modifier           = Modifier.size(24.dp).clickable {}
            )

            Spacer(Modifier.width(16.dp))

            // Notification bell with badge
            Box {
                Icon(
                    imageVector        = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint               = SW_DarkNavy,
                    modifier           = Modifier.size(24.dp).clickable {}
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(SW_DangerRed, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "3",
                        fontSize   = 10.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SITE SELECTOR CARD  (height 72dp, corner 16dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SiteSelectorCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SW_Card),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, SW_Border)
    ) {
        Row(
            modifier         = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Site image placeholder  56×56dp
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFD8E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Apartment,
                    contentDescription = null,
                    tint               = SW_PrimaryBlue,
                    modifier           = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Green Valley Tower",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = SW_DarkNavy
                )
                Text(
                    text     = "Site ID: GVT-2024-001",
                    fontSize = 12.sp,
                    color    = SW_TextSec
                )
            }

            Icon(
                imageVector        = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select site",
                tint               = SW_TextSec,
                modifier           = Modifier.size(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  GREETING CARD  (height 64dp, corner 16dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GreetingCard(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(64.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SW_Card),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, SW_Border)
    ) {
        Row(
            modifier         = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFD8E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = SW_PrimaryBlue, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Good Morning, $name 👋",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = SW_DarkNavy
                )
                Text(
                    text     = "Here's what's happening at your site today.",
                    fontSize = 12.sp,
                    color    = SW_TextSec,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))
            Text("☀️", fontSize = 22.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TODAY'S OVERVIEW  (4 KPI cards)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun TodaysOverviewSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SWSectionHeader(title = "Today's Overview", onViewAll = {})
        Spacer(Modifier.height(10.dp))
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                icon       = Icons.Default.Groups,
                iconTint   = SW_PrimaryBlue,
                iconBg     = Color(0xFFEEF4FF),
                value      = "146",
                label      = "All Workers",
                labelColor = SW_TextSec,
                modifier   = Modifier.weight(1f)
            )
            KpiCard(
                icon       = Icons.Default.CheckCircle,
                iconTint   = SW_SuccessGreen,
                iconBg     = Color(0xFFECFDF5),
                topLabel   = "Present",
                value      = "128",
                label      = "87.7%",
                labelColor = SW_SuccessGreen,
                modifier   = Modifier.weight(1f)
            )
            KpiCard(
                icon       = Icons.Default.PersonOff,
                iconTint   = SW_DangerRed,
                iconBg     = Color(0xFFFEF2F2),
                topLabel   = "Absent",
                value      = "12",
                label      = "8.2%",
                labelColor = SW_DangerRed,
                modifier   = Modifier.weight(1f)
            )
            KpiCard(
                icon       = Icons.Default.EventBusy,
                iconTint   = SW_Purple,
                iconBg     = Color(0xFFF5F3FF),
                topLabel   = "On Leave",
                value      = "6",
                label      = "4.1%",
                labelColor = SW_Purple,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

// KPI Card  (spec: 72×84dp, corner 16dp)
@Composable
private fun KpiCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    labelColor: Color,
    topLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.height(84.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SW_Card),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, SW_Border)
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceEvenly
        ) {
            // Icon with coloured circle background
            Box(
                modifier = Modifier.size(30.dp).background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }

            // Optional category label above value
            if (topLabel != null) {
                Text(
                    text      = topLabel,
                    fontSize  = 9.sp,
                    color     = SW_TextSec,
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )
            }

            // Main KPI value
            Text(
                text       = value,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = SW_DarkNavy,
                textAlign  = TextAlign.Center
            )

            // Bottom label / percentage
            Text(
                text       = label,
                fontSize   = 10.sp,
                color      = labelColor,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUICK ACTIONS  (5 items, 64×64dp cards, corner 16dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun QuickActionsSection(modifier: Modifier = Modifier) {
    val actions = listOf(
        HomeQuickAction(Icons.Default.PersonAdd,   "Add\nWorker",      SW_PrimaryBlue),
        HomeQuickAction(Icons.Default.HowToReg,    "Mark\nAttendance", SW_SuccessGreen),
        HomeQuickAction(Icons.Default.TableChart,  "View\nAttendance", SW_WarnOrange),
        HomeQuickAction(Icons.Default.Payments,    "Payroll",          SW_Purple),
        HomeQuickAction(Icons.Default.Assessment,  "Site\nReport",     SW_DangerRed)
    )

    Column(modifier = modifier) {
        Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SW_DarkNavy)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            actions.forEach { QuickActionItem(it) }
        }
    }
}

@Composable
private fun QuickActionItem(action: HomeQuickAction) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick   = {},
            modifier  = Modifier.size(64.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = SW_Card),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = BorderStroke(1.dp, SW_Border)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(action.icon, action.label, tint = action.tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text      = action.label,
            fontSize  = 10.sp,
            color     = SW_TextSec,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ATTENDANCE SUMMARY  (donut chart + legend)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AttendanceSummarySection(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SWSectionHeader(title = "Attendance Summary", onViewAll = {})
        Spacer(Modifier.height(10.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = SW_Card),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = BorderStroke(1.dp, SW_Border)
        ) {
            Row(
                modifier         = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart with centre label
                Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    DonutChart(
                        segments = listOf(
                            DonutSegment(0.877f, SW_SuccessGreen),
                            DonutSegment(0.068f, SW_WarnOrange),
                            DonutSegment(0.082f, SW_DangerRed),
                            DonutSegment(0.041f, SW_Purple)
                        ),
                        modifier = Modifier.size(110.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("146", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SW_DarkNavy)
                        Text("Total", fontSize = 10.sp, color = SW_TextSec)
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendRow(SW_SuccessGreen, "Present",  "128", "87.7%")
                    LegendRow(SW_WarnOrange,   "Late",     "10",  "6.8%")
                    LegendRow(SW_DangerRed,    "Absent",   "12",  "8.2%")
                    LegendRow(SW_Purple,       "On Leave", "6",   "4.1%")
                }
            }
        }
    }
}

@Composable
private fun DonutChart(segments: List<DonutSegment>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val strokeW = size.minDimension * 0.15f
        val radius  = (size.minDimension - strokeW) / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        var startAngle = -90f

        segments.forEach { seg ->
            val sweep = seg.fraction * 360f
            drawArc(
                color      = seg.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter  = false,
                style      = Stroke(width = strokeW, cap = StrokeCap.Butt),
                topLeft    = topLeft,
                size       = arcSize
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: String, pct: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, fontSize = 13.sp, color = SW_DarkNavy, modifier = Modifier.weight(1f))
        Text(count, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SW_DarkNavy)
        Text(pct,   fontSize = 11.sp, color = SW_TextSec)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  RECENT ACTIVITIES
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun RecentActivitiesSection(modifier: Modifier = Modifier) {
    val activities = listOf(
        RecentActivity(Icons.Default.CheckCircle, SW_SuccessGreen, "Attendance marked for 128 workers", "Today, 09:15 AM"),
        RecentActivity(Icons.Default.PersonAdd, SW_PrimaryBlue, "New worker Arjun Patel added", "Today, 08:45 AM"),
        RecentActivity(Icons.Default.Payments, SW_WarnOrange, "Payroll April 2024 generated", "Yesterday, 06:30 PM"),
        RecentActivity(Icons.Default.Assessment, SW_DangerRed, "Site report uploaded", "Yesterday, 05:20 PM")
    )
    Column(modifier = modifier) {
        Text("Recent Activities", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SW_DarkNavy)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { RecentActivityItem(it) }
        }
    }
}

@Composable
private fun RecentActivityItem(activity: RecentActivity) {
    Card(
        modifier  = Modifier.fillMaxWidth().height(64.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SW_Card),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, SW_Border)
    ) {
        Row(
            modifier         = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(activity.tint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(activity.icon, null, tint = activity.tint, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = activity.title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = SW_DarkNavy
                )
                Text(
                    text     = activity.time,
                    fontSize = 11.sp,
                    color    = SW_TextSec
                )
            }

            Icon(Icons.Default.ChevronRight, "View", tint = SW_TextSec)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  COMMON COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SWSectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SW_DarkNavy)
        Text(
            text     = "View All",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color    = SW_PrimaryBlue,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        color = SW_Card,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, "Home", selected == 0) { onSelect(0) }
            BottomNavItem(Icons.Default.People, "Workers", selected == 1) { onSelect(1) }
            Spacer(modifier = Modifier.width(48.dp)) // Space for FAB
            BottomNavItem(Icons.Default.FactCheck, "Attendance", selected == 2) { onSelect(2) }
            BottomNavItem(Icons.Default.GridView, "More", selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) SW_PrimaryBlue else SW_TextSec
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal, color = color)
    }
}

package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  COLOUR TOKENS  (spec §1)
// ─────────────────────────────────────────────────────────────────────────────
private val AT_Primary   = Color(0xFF0D5BFF)
private val AT_Green     = Color(0xFF22C55E)
private val AT_Orange    = Color(0xFFF59E0B)
private val AT_Red       = Color(0xFFEF4444)
private val AT_Purple    = Color(0xFF8B5CF6)
private val AT_Navy      = Color(0xFF0F172A)
private val AT_TextGray  = Color(0xFF64748B)
private val AT_Border    = Color(0xFFE2E8F0)
private val AT_BG        = Color(0xFFF8FAFC)
private val AT_White     = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
private enum class AttStatus { PRESENT, ABSENT, LATE, ON_LEAVE, OVERTIME }

private data class AttRecord(
    val name: String,
    val id: String,
    val role: String,
    val status: AttStatus,
    val inTime: String,
    val outTime: String,
    val totalHrs: String,
    val overtime: String? = null,
    val avatarBg: Color = Color(0xFFD8E8FF)
)

private data class KpiStat(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val label: String,
    val count: String,
    val pct: String
)

// ─────────────────────────────────────────────────────────────────────────────
//  STATUS HELPERS
// ─────────────────────────────────────────────────────────────────────────────
private fun attStatusLabel(s: AttStatus) = when (s) {
    AttStatus.PRESENT  -> "Present"
    AttStatus.ABSENT   -> "Absent"
    AttStatus.LATE     -> "Late"
    AttStatus.ON_LEAVE -> "On Leave"
    AttStatus.OVERTIME -> "Overtime"
}

private fun attStatusColor(s: AttStatus) = when (s) {
    AttStatus.PRESENT  -> AT_Green
    AttStatus.ABSENT   -> AT_Red
    AttStatus.LATE     -> AT_Orange
    AttStatus.ON_LEAVE -> AT_Purple
    AttStatus.OVERTIME -> AT_Primary
}

private fun attStatusBg(s: AttStatus) = when (s) {
    AttStatus.PRESENT  -> Color(0xFFDCFCE7)
    AttStatus.ABSENT   -> Color(0xFFFEE2E2)
    AttStatus.LATE     -> Color(0xFFFFF7ED)
    AttStatus.ON_LEAVE -> Color(0xFFF3E8FF)
    AttStatus.OVERTIME -> Color(0xFFEEF4FF)
}

// ─────────────────────────────────────────────────────────────────────────────
//  SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────
private val kpiStats = listOf(
    KpiStat(Icons.Default.CheckCircle, AT_Green,   Color(0xFFECFDF5), "Present",  "128", "86.5%"),
    KpiStat(Icons.Default.PersonOff,   AT_Red,     Color(0xFFFEF2F2), "Absent",   "14",  "9.5%"),
    KpiStat(Icons.Default.Schedule,    AT_Orange,  Color(0xFFFFF7ED), "Late",     "6",   "4.1%"),
    KpiStat(Icons.Default.EventBusy,   AT_Purple,  Color(0xFFF5F3FF), "On Leave", "8",   "5.4%"),
    KpiStat(Icons.Default.Timer,       AT_Primary, Color(0xFFEEF4FF), "Overtime", "18",  "12.2%")
)

private val attendanceData = listOf(
    AttRecord("Ramesh Kumar",  "W001", "Mason",       AttStatus.PRESENT,  "08:45 AM", "05:45 PM", "09h 00m", "+01h 00m", Color(0xFFBFDBFE)),
    AttRecord("Suresh Yadav",  "W002", "Helper",      AttStatus.PRESENT,  "08:50 AM", "06:05 PM", "09h 15m", "+01h 15m", Color(0xFFFEF3C7)),
    AttRecord("Arun Kumar",    "W003", "Electrician", AttStatus.LATE,     "09:20 AM", "--",        "--",      null,       Color(0xFFFED7AA)),
    AttRecord("Vijay Singh",   "W004", "Carpenter",   AttStatus.PRESENT,  "08:40 AM", "05:30 PM", "08h 50m", null,       Color(0xFFD1FAE5)),
    AttRecord("Deepak Verma",  "W005", "Plumber",     AttStatus.ABSENT,   "--",       "--",        "--",      null,       Color(0xFFFFE4E6))
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerAttendanceScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = AT_BG,
        topBar         = { AttendanceTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // ── Scrollable content ────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier.weight(1f),
                contentPadding      = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { SiteCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { DateSelectorRow(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                item { KpiCardsRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                item { MarkAttendanceCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                item { AttendanceTabBar(selectedTab) { selectedTab = it } }
                item { SearchActionsRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { WorkerTableHeader() }
                items(attendanceData) { record ->
                    AttendanceWorkerRow(record)
                    HorizontalDivider(color = AT_Border.copy(alpha = 0.6f), thickness = 0.5.dp)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Summary strip (fixed above BottomNavBar) ──────────────────
            AttendanceSummaryStrip()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AttendanceTopBar() {
    Surface(color = AT_White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = AT_Navy, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                "Attendance",
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = AT_Navy,
                modifier   = Modifier.weight(1f)
            )
            Icon(Icons.Default.Search, "Search",
                tint = AT_Navy, modifier = Modifier.size(24.dp).clickable {})
            Spacer(Modifier.width(16.dp))
            Box {
                Icon(Icons.Default.Notifications, "Notifications",
                    tint = AT_Navy, modifier = Modifier.size(24.dp).clickable {})
                Box(
                    modifier = Modifier.size(16.dp).background(AT_Red, CircleShape).align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontSize = 10.sp, color = AT_White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SITE SELECTOR CARD  (spec §4: Height 72dp, Radius 16dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SiteCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AT_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, AT_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFD8E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apartment, null, tint = AT_Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Green Valley Tower", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AT_Navy)
                Text("Site ID: GVT-2024-001", fontSize = 12.sp, color = AT_TextGray)
            }
            Icon(Icons.Default.KeyboardArrowDown, "Expand", tint = AT_TextGray, modifier = Modifier.size(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DATE SELECTOR  (spec §4: Height 48dp)
//  < [Calendar] 14 May 2024, Tuesday >    [Today]  [Filter]
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DateSelectorRow(modifier: Modifier = Modifier) {
    Row(
        modifier          = modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ← arrow
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).clickable {},
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, "Prev", tint = AT_Navy, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.CalendarToday, null, tint = AT_Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "14 May 2024, Tuesday",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = AT_Navy,
            modifier   = Modifier.weight(1f)
        )

        // → arrow
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).clickable {},
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronRight, "Next", tint = AT_Navy, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        // "Today" outlined button
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AT_Primary, RoundedCornerShape(8.dp))
                .clickable {}
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Today", fontSize = 12.sp, color = AT_Primary, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.width(6.dp))

        // Filter button
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AT_Border, RoundedCornerShape(8.dp))
                .clickable {}
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, null, tint = AT_Navy, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("Filter", fontSize = 12.sp, color = AT_Navy)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  KPI SUMMARY CARDS  (spec §4: Height 88dp, Radius 16dp — 5 cards, 1 row)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun KpiCardsRow(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        kpiStats.forEach { stat ->
            KpiStatCard(stat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun KpiStatCard(stat: KpiStat, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.height(88.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AT_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, AT_Border)
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement   = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(26.dp).background(stat.iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(stat.icon, null, tint = stat.iconTint, modifier = Modifier.size(15.dp))
            }
            Text(stat.label, fontSize = 9.sp,  color = AT_TextGray, maxLines = 1)
            Text(stat.count, fontSize = 18.sp, fontWeight = FontWeight.Bold,    color = AT_Navy)
            Text(stat.pct,   fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = stat.iconTint)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MARK ATTENDANCE CARD  (spec §4: Padding 16dp, Radius 16dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MarkAttendanceCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AT_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, AT_Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: title + "Mark Now →" button
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark Attendance",  fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AT_Navy)
                    Text(
                        "Quickly mark attendance using any of the methods",
                        fontSize = 11.sp,
                        color    = AT_TextGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick        = {},
                    shape          = RoundedCornerShape(8.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = AT_Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Mark Now", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AT_White)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
                }
            }

            HorizontalDivider(color = AT_Border, modifier = Modifier.padding(vertical = 12.dp))

            // 3 attendance methods
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttMethodBtn(Icons.Default.QrCodeScanner,         AT_Primary, "QR Attendance")
                AttMethodBtn(Icons.Default.Face,                  AT_Green,   "Face Attendance")
                AttMethodBtn(Icons.Default.TouchApp,              AT_Orange,  "Manual Attendance")
            }
        }
    }
}

@Composable
private fun AttMethodBtn(icon: ImageVector, tint: Color, label: String) {
    Column(
        modifier            = Modifier.clickable {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(tint.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize   = 11.sp,
            color      = AT_Navy,
            textAlign  = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines   = 2
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ATTENDANCE TAB BAR  (spec §4: Height 48dp, Indicator 3dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AttendanceTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Worker List", "Attendance Sheet", "Summary", "Reports")
    Surface(color = AT_White) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedTab
                    // Box with weight gives bounded width → fillMaxWidth() on indicator works
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = tab,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color    = if (isSelected) AT_Primary else AT_TextGray,
                            textAlign = TextAlign.Center
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(AT_Primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = AT_Border, thickness = 1.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH & ACTIONS ROW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SearchActionsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AT_White)
                .border(1.dp, AT_Border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, "Search", tint = AT_TextGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search worker...", fontSize = 13.sp, color = AT_TextGray)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Sort/Filter
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AT_Border, RoundedCornerShape(8.dp))
                .background(AT_White)
                .clickable {},
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FilterList, "Filter", tint = AT_Navy, modifier = Modifier.size(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TABLE HEADER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkerTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AT_BG)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Worker Details", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AT_TextGray, modifier = Modifier.weight(1.2f))
        Text("Status",         fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AT_TextGray, modifier = Modifier.weight(0.8f))
        Text("In - Out",       fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AT_TextGray, modifier = Modifier.weight(1f))
        Text("Total Hrs",      fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AT_TextGray, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
    }
    HorizontalDivider(color = AT_Border, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════════════════
//  WORKER ROW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AttendanceWorkerRow(record: AttRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AT_White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Details
        Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(record.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(record.name.take(1), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AT_Navy)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(record.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AT_Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${record.id} • ${record.role}", fontSize = 10.sp, color = AT_TextGray)
            }
        }

        // Status
        Box(modifier = Modifier.weight(0.8f)) {
            Box(
                modifier = Modifier
                    .background(attStatusBg(record.status), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(attStatusLabel(record.status), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = attStatusColor(record.status))
            }
        }

        // In - Out
        Column(modifier = Modifier.weight(1f)) {
            Text(record.inTime, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AT_Navy)
            Text(record.outTime, fontSize = 10.sp, color = AT_TextGray)
        }

        // Total Hrs
        Column(modifier = Modifier.weight(0.7f), horizontalAlignment = Alignment.End) {
            Text(record.totalHrs, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AT_Navy)
            if (record.overtime != null) {
                Text(record.overtime, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AT_Primary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SUMMARY STRIP (Bottom)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AttendanceSummaryStrip() {
    Surface(
        color = AT_White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Expected: 156", fontSize = 11.sp, color = AT_TextGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("128 Present", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AT_Green)
                    Text(" • ", fontSize = 14.sp, color = AT_TextGray)
                    Text("14 Absent", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AT_Red)
                }
            }
            Button(
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AT_Primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Export PDF", fontSize = 12.sp, color = AT_White)
            }
        }
    }
}

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  COLOUR TOKENS  (spec §1)
// ─────────────────────────────────────────────────────────────────────────────
private val WK_Primary   = Color(0xFF0D5BFF)
private val WK_Green     = Color(0xFF22C55E)
private val WK_Orange    = Color(0xFFF59E0B)
private val WK_Red       = Color(0xFFEF4444)
private val WK_Purple    = Color(0xFF8B5CF6)
private val WK_Navy      = Color(0xFF0F172A)
private val WK_TextGray  = Color(0xFF64748B)
private val WK_Border    = Color(0xFFE2E8F0)
private val WK_BG        = Color(0xFFF8FAFC)
private val WK_White     = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
//  STATUS ENUM
// ─────────────────────────────────────────────────────────────────────────────
private enum class WorkerStatus { PRESENT, LATE, ABSENT, ON_LEAVE }

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
private data class Worker(
    val id: String,
    val name: String,
    val role: String,
    val status: WorkerStatus,
    val avatarBg: Color = Color(0xFFD8E8FF)
)

private data class WkSummaryCard(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val label: String,
    val count: String,
    val pct: String? = null,
    val pctColor: Color = Color.Transparent
)

// ─────────────────────────────────────────────────────────────────────────────
//  SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────
private val sampleWorkers = listOf(
    Worker("W001", "Ramesh Kumar",  "Mason",       WorkerStatus.PRESENT,  Color(0xFFBFDBFE)),
    Worker("W002", "Suresh Yadav",  "Helper",      WorkerStatus.PRESENT,  Color(0xFFFEF3C7)),
    Worker("W003", "Arun Kumar",    "Electrician", WorkerStatus.LATE,     Color(0xFFFED7AA)),
    Worker("W004", "Vijay Singh",   "Carpenter",   WorkerStatus.PRESENT,  Color(0xFFD1FAE5)),
    Worker("W005", "Deepak Verma",  "Plumber",     WorkerStatus.ABSENT,   Color(0xFFFFE4E6)),
    Worker("W006", "Mohammad Ali",  "Mason",       WorkerStatus.PRESENT,  Color(0xFFF3E8FF)),
    Worker("W007", "Rohit Sharma",  "Steel Fixer", WorkerStatus.PRESENT,  Color(0xFFFEF9C3)),
    Worker("W008", "Sanjay Patel",  "Painter",     WorkerStatus.LATE,     Color(0xFFE0F2FE))
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerWorkersScreen(
    onNavigateToWorkerDetails: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = WK_BG,
        topBar    = { WorkersTopBar(onMenuClick) }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { WorkersSiteCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { WorkersTabBar(selectedTab) { selectedTab = it } }
            item { SearchAndFilterRow(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            item { WorkersSummarySection(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            item { WorkerListHeader(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            items(sampleWorkers) { worker ->
                WorkerListItem(worker, onClick = onNavigateToWorkerDetails)
                HorizontalDivider(
                    color     = WK_Border,
                    thickness = 0.5.dp,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkersTopBar(onMenuClick: () -> Unit = {}) {
    Surface(color = WK_White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = WK_Navy, modifier = Modifier.size(24.dp).clickable { onMenuClick() })
            Spacer(Modifier.width(16.dp))
            Text(
                "Workers",
                fontSize   = 20.sp,           // spec §2: Screen Title 20sp
                fontWeight = FontWeight.SemiBold,
                color      = WK_Navy,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Search, "Search",
                tint     = WK_Navy,
                modifier = Modifier.size(24.dp).clickable {}
            )
            Spacer(Modifier.width(16.dp))
            // Notification bell  (spec §4: badge 16dp, #EF4444)
            Box {
                Icon(
                    Icons.Default.Notifications, "Notifications",
                    tint     = WK_Navy,
                    modifier = Modifier.size(24.dp).clickable {}
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(WK_Red, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontSize = 10.sp, color = WK_White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SITE SELECTOR CARD
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkersSiteCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = WK_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, WK_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFD8E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apartment, null, tint = WK_Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Green Valley Tower",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WK_Navy
                )
                Text("Site ID: GVT-2024-001", fontSize = 12.sp, color = WK_TextGray)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown, "Expand",
                    tint     = WK_Navy,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TAB BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkersTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("All (146)", "Active (128)", "Inactive (10)", "On Leave (8)")
    Surface(color = WK_White) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = tab,
                            fontSize  = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color     = if (isSelected) WK_Primary else WK_TextGray,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 3.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(if (isSelected) WK_Primary else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = WK_Border, thickness = 1.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH BAR + FILTER BUTTON
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SearchAndFilterRow(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(WK_White, RoundedCornerShape(12.dp))
                .border(1.dp, WK_Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = WK_TextGray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search by name, ID or mobile number",
                    fontSize = 13.sp,
                    color    = WK_TextGray,
                    maxLines = 1
                )
            }
        }
        OutlinedButton(
            onClick        = {},
            shape          = RoundedCornerShape(12.dp),
            colors         = ButtonDefaults.outlinedButtonColors(contentColor = WK_Navy),
            border         = BorderStroke(1.dp, WK_Border),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier       = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.FilterList, null, tint = WK_Navy, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Filter", fontSize = 13.sp, color = WK_Navy, fontWeight = FontWeight.Medium)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  WORKERS SUMMARY CARDS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkersSummarySection(modifier: Modifier = Modifier) {
    val cards = listOf(
        WkSummaryCard(Icons.Default.Groups,       WK_Primary, Color(0xFFEEF4FF), "Total Workers", "146"),
        WkSummaryCard(Icons.Default.CheckCircle,  WK_Green,   Color(0xFFECFDF5), "Active",        "128", "87.7%", WK_Green),
        WkSummaryCard(Icons.Default.PersonOff,    WK_Red,     Color(0xFFFEF2F2), "Inactive",      "10",  "6.8%",  WK_Red),
        WkSummaryCard(Icons.Default.EventBusy,    WK_Purple,  Color(0xFFF5F3FF), "On Leave",      "8",   "5.5%",  WK_Purple)
    )

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cards.forEach { card ->
            WkSummaryCardView(card, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WkSummaryCardView(card: WkSummaryCard, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.height(90.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = WK_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, WK_Border)
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(card.iconBg, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(card.icon, null, tint = card.iconTint, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    card.label,
                    fontSize   = 9.sp,
                    color      = WK_TextGray,
                    lineHeight = 12.sp,
                    maxLines   = 2
                )
            }
            Text(
                card.count,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = WK_Navy
            )
            if (card.pct != null) {
                Text(
                    card.pct,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = card.pctColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  WORKER LIST HEADER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkerListHeader(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("Worker List", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WK_Navy)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, WK_Border, RoundedCornerShape(8.dp))
                .clickable {}
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort by: Name (A-Z)", fontSize = 12.sp, color = WK_TextGray)
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint     = WK_TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  WORKER LIST ITEM
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkerListItem(worker: Worker, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(WK_White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(worker.avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = WK_Primary, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                worker.name,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = WK_Navy
            )
            Text(
                text     = "ID: ${worker.id}  •  ${worker.role}",
                fontSize = 12.sp,
                color    = WK_TextGray
            )
        }

        WorkerStatusPill(worker.status)

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEEF4FF), CircleShape)
                .clickable {},
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Phone, "Call", tint = WK_Primary, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(6.dp))

        Icon(
            Icons.Default.ChevronRight, null,
            tint     = WK_TextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun WorkerStatusPill(status: WorkerStatus) {
    val (label, bg) = when (status) {
        WorkerStatus.PRESENT  -> "Present"  to WK_Green
        WorkerStatus.LATE     -> "Late"     to WK_Orange
        WorkerStatus.ABSENT   -> "Absent"   to WK_Red
        WorkerStatus.ON_LEAVE -> "On Leave" to WK_Purple
    }
    Box(
        modifier = Modifier
            .background(bg.copy(alpha=0.15f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            color      = bg
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkersBottomSection(onNavigateToHome: () -> Unit = {}, onNavigateToPayroll: () -> Unit = {}, onNavigateToMore: () -> Unit = {}) {
    Surface(
        color          = WK_White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            WorkersNavTab(Icons.Default.Home, "Home", false, onClick = onNavigateToHome)
            WorkersNavTab(Icons.Default.Groups, "Workers", true)
            Spacer(Modifier.width(56.dp))
            WorkersNavTab(Icons.Default.CalendarToday, "Attendance", false)
            WorkersNavTab(Icons.Default.Payments, "Payroll", false, onClick = onNavigateToPayroll)
            WorkersNavTab(Icons.Default.MoreHoriz, "More", false, onClick = onNavigateToMore)
        }
    }
}

@Composable
private fun WorkersNavTab(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit = {}) {
    val color = if (active) WK_Primary else WK_TextGray
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Medium else FontWeight.Normal, color = color)
    }
}

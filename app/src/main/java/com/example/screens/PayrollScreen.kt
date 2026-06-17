package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
//  COLOUR TOKENS  (spec §2 – Navy is #0F172A on Payroll screen)
// ─────────────────────────────────────────────────────────────────────────────
private val PY_Primary   = Color(0xFF0D5BFF)
private val PY_Green     = Color(0xFF22C55E)
private val PY_Orange    = Color(0xFFF59E0B)
private val PY_Red       = Color(0xFFEF4444)
private val PY_Purple    = Color(0xFF8B5CF6)
private val PY_Navy      = Color(0xFF0F172A)
private val PY_TextGray  = Color(0xFF64748B)
private val PY_Border    = Color(0xFFE2E8F0)
private val PY_BG        = Color(0xFFF8FAFC)
private val PY_White     = Color(0xFFFFFFFF)
private val PY_LightBlue = Color(0xFFEFF6FF)   // Upcoming payroll card bg  (spec §6)
private val PY_DateBG    = Color(0xFFF1F5F9)   // Date box bg  (spec §7)

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
private data class SummaryItem(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val label: String,
    val value: String,
    val valueColor: Color
)

private data class BatchItem(
    val monthAbbr: String,
    val day: String,
    val title: String,
    val workers: String,
    val amount: String,
    val status: String,
    val statusColor: Color,
    val statusBg: Color
)

private data class SalaryCategory(
    val icon: ImageVector,
    val iconTint: Color,
    val label: String,
    val count: String,
    val amount: String
)

private data class PayrollQuickAction(
    val icon: ImageVector,
    val iconTint: Color,
    val label: String
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerPayrollScreen(
    onMenuClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = PY_BG,
        topBar    = { PayrollTopBar(onMenuClick) }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { PayrollSiteCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { PayrollTabBar(selectedTab = selectedTab) { selectedTab = it } }
            item { PayrollSummarySection(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { UpcomingPayrollCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { RecentBatchesSection(Modifier.padding(horizontal = 16.dp)) }
            item { SalaryDistSection(Modifier.padding(horizontal = 16.dp)) }
            item { PayrollQuickActionsSection(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollTopBar(onMenuClick: () -> Unit = {}) {
    Surface(color = PY_White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = PY_Navy, modifier = Modifier.size(24.dp).clickable { onMenuClick() })
            Spacer(Modifier.width(16.dp))
            Text(
                "Payroll",
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PY_Navy,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Search, "Search",
                tint     = PY_Navy,
                modifier = Modifier.size(24.dp).clickable {}
            )
            Spacer(Modifier.width(16.dp))
            // Bell + badge  (spec §11: badge 16dp, #EF4444)
            Box {
                Icon(
                    Icons.Default.Notifications, "Notifications",
                    tint     = PY_Navy,
                    modifier = Modifier.size(24.dp).clickable {}
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(PY_Red, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontSize = 10.sp, color = PY_White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SITE SELECTOR CARD  (spec §10: height 72dp, corner 16dp, Switch Site btn)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollSiteCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = PY_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, PY_Border)
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
                Icon(Icons.Default.Apartment, null, tint = PY_Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Green Valley Tower", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
                Text("Site ID: GVT-2024-001", fontSize = 12.sp, color = PY_TextGray)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick        = {},
                shape          = RoundedCornerShape(8.dp),
                colors         = ButtonDefaults.outlinedButtonColors(contentColor = PY_Navy),
                border         = BorderStroke(1.dp, PY_Border),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier       = Modifier.height(34.dp)
            ) {
                Text("Switch Site", fontSize = 12.sp, color = PY_Navy, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = PY_Navy, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TAB BAR  (spec §5: height 48dp, active #0D5BFF, indicator 3dp, scrollable)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Overview", "Salary", "Advances", "Deductions", "Payments")
    Surface(color = PY_White) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.width(8.dp))
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedTab
                    // IntrinsicSize.Max lets the inner fillMaxWidth indicator
                    // match the Column width (= text + horizontal padding)
                    Box(
                        modifier = Modifier
                            .clickable { onTabSelected(index) }
                            .width(IntrinsicSize.Max)
                            .height(48.dp)
                    ) {
                        Text(
                            text       = tab,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (isSelected) PY_Primary else PY_TextGray,
                            modifier   = Modifier
                                .align(Alignment.Center)
                                .padding(start = 16.dp, end = 16.dp, bottom = 3.dp)
                        )
                        // Active underline  (spec §5: indicator 3dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(if (isSelected) PY_Primary else Color.Transparent)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            HorizontalDivider(color = PY_Border, thickness = 1.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PAYROLL SUMMARY  (6 KPI cards in a 3-column × 2-row grid)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollSummarySection(modifier: Modifier = Modifier) {
    val items = listOf(
        SummaryItem(Icons.Default.Groups,               PY_Primary, Color(0xFFEEF4FF), "Total Workers",    "146",        PY_Navy),
        SummaryItem(Icons.Default.CurrencyRupee,        PY_Green,   Color(0xFFECFDF5), "Total Payroll",    "₹28,45,600", PY_Green),
        SummaryItem(Icons.Default.AccountBalanceWallet, PY_Green,   Color(0xFFECFDF5), "Paid Amount",      "₹18,75,400", PY_Green),
        SummaryItem(Icons.Default.Schedule,             PY_Orange,  Color(0xFFFFF7ED), "Pending Amount",   "₹9,70,200",  PY_Orange),
        SummaryItem(Icons.Default.PersonAdd,            PY_Purple,  Color(0xFFF5F3FF), "Advances Given",   "₹3,20,000",  PY_Purple),
        SummaryItem(Icons.Default.RemoveCircle,         PY_Red,     Color(0xFFFEF2F2), "Total Deductions", "₹1,80,400",  PY_Red)
    )

    Column(modifier = modifier) {
        // Section header  +  "This Month ▼" dropdown
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Payroll Summary",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PY_Navy
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, PY_Border, RoundedCornerShape(8.dp))
                    .clickable {}
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This Month", fontSize = 12.sp, color = PY_TextGray)
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = PY_TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3 × 2 grid
        val rows = items.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        SummaryCard(item, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Single KPI card  (spec §4: Full Width × 84dp, corner 16dp, padding 16dp)
@Composable
private fun SummaryCard(item: SummaryItem, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.height(84.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = PY_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, PY_Border)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon (rounded-square bg, spec §11: 40dp container, 24dp icon, 12dp radius, 10% tint)
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(item.iconBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = item.iconTint, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    item.label,
                    fontSize   = 10.sp,
                    color      = PY_TextGray,
                    lineHeight = 13.sp,
                    maxLines   = 2
                )
            }
            // KPI value  (spec §13: 20sp SemiBold)
            Text(
                item.value,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = item.valueColor,
                maxLines   = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  UPCOMING PAYROLL CARD  (spec §6: corner 16dp, bg #EFF6FF, btn 40dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun UpcomingPayrollCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = PY_LightBlue),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // "Next Salary Date"  |  "5 Days Left" pill
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Next Salary Date",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = PY_TextGray
                )
                Box(
                    modifier = Modifier
                        .background(PY_Primary, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "5 Days Left",
                        fontSize   = 11.sp,
                        color      = PY_White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Calendar icon + date  (spec §6: icon 32dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFBFDBFE), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = PY_Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "15 May 2024",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PY_Navy
                    )
                    Text("For 146 Workers", fontSize = 12.sp, color = PY_TextGray)
                }
            }

            Spacer(Modifier.height(16.dp))

            // "Generate Payroll" CTA  (spec §6: button height 40dp)
            Button(
                onClick  = {},
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PY_Primary)
            ) {
                Text("Generate Payroll", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PY_White)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  RECENT PAYROLL BATCHES
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun RecentBatchesSection(modifier: Modifier = Modifier) {
    val batches = listOf(
        BatchItem("APR", "30", "April 2024 Payroll",    "146 Workers", "₹28,45,600", "Paid",       PY_Green,  Color(0xFFDCFCE7)),
        BatchItem("MAR", "31", "March 2024 Payroll",    "146 Workers", "₹27,80,450", "Paid",       PY_Green,  Color(0xFFDCFCE7)),
        BatchItem("FEB", "29", "February 2024 Payroll", "145 Workers", "₹26,15,300", "Paid",       PY_Green,  Color(0xFFDCFCE7)),
        BatchItem("JAN", "31", "January 2024 Payroll",  "142 Workers", "₹24,60,800", "Processing", PY_Orange, Color(0xFFFFF7ED))
    )

    Column(modifier = modifier) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Recent Payroll Batches", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
            Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PY_Primary, modifier = Modifier.clickable {})
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            batches.forEach { batch ->
                BatchCard(batch)
            }
        }
    }
}

@Composable
private fun BatchCard(batch: BatchItem) {
    Card(
        modifier  = Modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = PY_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, PY_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date box
            Column(
                modifier = Modifier
                    .size(48.dp)
                    .background(PY_DateBG, RoundedCornerShape(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(batch.monthAbbr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PY_TextGray)
                Text(batch.day, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PY_Navy)
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Title & workers
            Column(modifier = Modifier.weight(1f)) {
                Text(batch.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PY_Navy)
                Text(batch.workers, fontSize = 12.sp, color = PY_TextGray)
            }
            
            // Amount & status
            Column(horizontalAlignment = Alignment.End) {
                Text(batch.amount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
                Box(
                    modifier = Modifier
                        .background(batch.statusBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(batch.status, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = batch.statusColor)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SALARY DISTRIBUTION
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SalaryDistSection(modifier: Modifier = Modifier) {
    val categories = listOf(
        SalaryCategory(Icons.Default.Engineering, PY_Primary, "Engineers & Supervisors", "12", "₹8,40,000"),
        SalaryCategory(Icons.Default.Construction, PY_Orange, "Skilled Labours", "48", "₹11,52,000"),
        SalaryCategory(Icons.Default.PanTool, PY_TextGray, "Unskilled Labours", "86", "₹8,53,600")
    )
    Column(modifier = modifier) {
        Spacer(Modifier.height(20.dp))
        Text("Salary Distribution", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
        Spacer(Modifier.height(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(containerColor = PY_White),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = BorderStroke(1.dp, PY_Border)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                categories.forEachIndexed { index, cat ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(cat.iconTint.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(cat.icon, null, tint = cat.iconTint, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PY_Navy)
                            Text("${cat.count} Workers", fontSize = 12.sp, color = PY_TextGray)
                        }
                        Text(cat.amount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
                    }
                    if (index < categories.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = PY_Border
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUICK ACTIONS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollQuickActionsSection(modifier: Modifier = Modifier) {
    val actions = listOf(
        PayrollQuickAction(Icons.Default.Receipt, PY_Primary, "Run Payroll"),
        PayrollQuickAction(Icons.Default.MoneyOff, PY_Red, "Add Deduction"),
        PayrollQuickAction(Icons.Default.AccountBalanceWallet, PY_Purple, "Give Advance")
    )
    Column(modifier = modifier) {
        Spacer(Modifier.height(20.dp))
        Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PY_Navy)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { action ->
                Card(
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PY_White),
                    border = BorderStroke(1.dp, PY_Border),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(action.icon, null, tint = action.iconTint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(action.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PY_Navy, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PayrollBottomNav(onNavigateToHome: () -> Unit = {}, onNavigateToMore: () -> Unit = {}, onNavigateToWorkers: () -> Unit = {}) {
    Surface(
        color          = PY_White,
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
            PayrollNavTab(Icons.Default.Home, "Home", false, onClick = onNavigateToHome)
            PayrollNavTab(Icons.Default.Groups, "Workers", false, onClick = onNavigateToWorkers)
            Spacer(Modifier.width(56.dp))
            PayrollNavTab(Icons.Default.CalendarToday, "Attendance", false)
            PayrollNavTab(Icons.Default.Payments, "Payroll", true)
            PayrollNavTab(Icons.Default.MoreHoriz, "More", false, onClick = onNavigateToMore)
        }
    }
}

@Composable
private fun PayrollNavTab(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit = {}) {
    val color = if (active) PY_Primary else PY_TextGray
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Medium else FontWeight.Normal, color = color)
    }
}

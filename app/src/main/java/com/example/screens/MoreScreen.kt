package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
//  COLOUR TOKENS  (spec §2)
// ─────────────────────────────────────────────────────────────────────────────
private val MS_Primary   = Color(0xFF0D5BFF)
private val MS_Green     = Color(0xFF22C55E)
private val MS_Orange    = Color(0xFFF59E0B)
private val MS_Red       = Color(0xFFEF4444)
private val MS_Purple    = Color(0xFF8B5CF6)
private val MS_Navy      = Color(0xFF001B36)
private val MS_TextGray  = Color(0xFF64748B)
private val MS_Border    = Color(0xFFE2E8F0)
private val MS_BG        = Color(0xFFF8FAFC)
private val MS_White     = Color(0xFFFFFFFF)
private val MS_AvatarBG  = Color(0xFFD8E8FF)
private val MS_ChevronTint = Color(0xFFCBD5E1)

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────
private data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val subtitle: String? = null   // e.g. "English" or "v1.2.0"
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerMoreScreen(
    onLogout: () -> Unit = {}
) {
    Scaffold(
        containerColor = MS_BG,
        topBar         = { MoreTopBar(onLogout) }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item { MoreSiteSelectorCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { MoreProfileCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            item { ManagementSection(Modifier.padding(horizontal = 16.dp)) }
            item { ReportsAnalyticsSection(Modifier.padding(horizontal = 16.dp)) }
            item { SettingsSupportSection(Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MoreTopBar(onMenuClick: () -> Unit = {}) {
    Surface(color = MS_White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Menu, "Menu",
                tint = MS_Navy, modifier = Modifier.size(24.dp).clickable { onMenuClick() }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text       = "More",
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MS_Navy,
                modifier   = Modifier.weight(1f)
            )

            Icon(
                Icons.Default.Search, "Search",
                tint     = MS_Navy,
                modifier = Modifier.size(24.dp).clickable {}
            )

            Spacer(Modifier.width(16.dp))

            // Notification bell with badge (spec §11: badge size 16dp, color #EF4444)
            Box {
                Icon(
                    Icons.Default.Notifications, "Notifications",
                    tint     = MS_Navy,
                    modifier = Modifier.size(24.dp).clickable {}
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MS_Red, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "2",
                        fontSize   = 10.sp,
                        color      = MS_White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SITE SELECTOR CARD  (spec §8 – height 72dp, corner 16dp, "Switch Site ▼")
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MoreSiteSelectorCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().height(72.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MS_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, MS_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Site thumbnail  56 × 56dp
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MS_AvatarBG),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apartment, null, tint = MS_Primary, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Green Valley Tower",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MS_Navy
                )
                Text(
                    "Site ID: GVT-2024-001",
                    fontSize = 12.sp,
                    color    = MS_TextGray
                )
            }

            Spacer(Modifier.width(8.dp))

            // "Switch Site ▼" outlined button
            OutlinedButton(
                onClick        = {},
                shape          = RoundedCornerShape(8.dp),
                colors         = ButtonDefaults.outlinedButtonColors(contentColor = MS_Navy),
                border         = BorderStroke(1.dp, MS_Border),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier       = Modifier.height(34.dp)
            ) {
                Text("Switch Site", fontSize = 12.sp, color = MS_Navy, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = MS_Navy,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PROFILE CARD  (spec §4 – height 88dp, corner 16dp, padding 16dp, avatar 56dp)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MoreProfileCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MS_White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border    = BorderStroke(1.dp, MS_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar  56 × 56dp
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MS_AvatarBG),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MS_Primary, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.width(12.dp))

            // Name · Role · Phone · Email
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Ramesh Kumar",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MS_Navy
                )
                Text(
                    "Supervisor",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MS_Primary
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Phone, null, tint = MS_TextGray, modifier = Modifier.size(12.dp))
                    Text("+91 98765 43210", fontSize = 12.sp, color = MS_TextGray)
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Email, null, tint = MS_TextGray, modifier = Modifier.size(12.dp))
                    Text(
                        "ramesh.kumar@smartworker.in",
                        fontSize = 11.sp,
                        color    = MS_TextGray,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint     = MS_TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MANAGEMENT SECTION  (6 items  ·  3-column grid)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ManagementSection(modifier: Modifier = Modifier) {
    MenuGridSection(
        title    = "Management",
        items    = listOf(
            MenuItem(Icons.Default.ManageAccounts,     "User\nManagement",    MS_Purple),
            MenuItem(Icons.Default.Business,           "Site\nManagement",    MS_Primary),
            MenuItem(Icons.Default.AdminPanelSettings, "Role &\nPermissions", MS_Green),
            MenuItem(Icons.Default.Construction,       "Equipment",           MS_Orange),
            MenuItem(Icons.Default.Inventory2,         "Material",            MS_Purple),
            MenuItem(Icons.Default.Assignment,         "Tasks",               MS_Primary)
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  REPORTS & ANALYTICS SECTION  (3 items  ·  single row)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ReportsAnalyticsSection(modifier: Modifier = Modifier) {
    MenuGridSection(
        title    = "Reports & Analytics",
        items    = listOf(
            MenuItem(Icons.Default.PieChart,     "Reports",     MS_Primary),
            MenuItem(Icons.Default.TrendingUp,   "Analytics",   MS_Green),
            MenuItem(Icons.Default.FileDownload, "Export Data", MS_Orange)
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  SETTINGS & SUPPORT SECTION  (6 items  ·  3-column grid)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SettingsSupportSection(modifier: Modifier = Modifier) {
    MenuGridSection(
        title    = "Settings & Support",
        items    = listOf(
            MenuItem(Icons.Default.Settings,    "App Settings",     MS_Primary),
            MenuItem(Icons.Default.HelpOutline, "Help &\nSupport",  MS_Primary),
            MenuItem(Icons.Default.Feedback,    "Feedback",         MS_Purple),
            MenuItem(Icons.Default.Lock,        "Change\nPassword", MS_Orange),
            MenuItem(Icons.Default.Language,    "Language",         MS_Green,  subtitle = "English"),
            MenuItem(Icons.Default.Info,        "About App",        MS_Purple, subtitle = "v1.2.0")
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  REUSABLE ─ MENU GRID SECTION
//  Renders: section title  +  items laid out in rows of 3
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MenuGridSection(
    title: String,
    items: List<MenuItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section header  (spec §6: 16sp SemiBold, top-margin 24dp, bottom-margin 12dp)
        Spacer(Modifier.height(24.dp))
        Text(
            text       = title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MS_Navy
        )
        Spacer(Modifier.height(12.dp))

        // Chunk items into rows of 3
        val rows = items.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        MenuTile(item = item, modifier = Modifier.weight(1f))
                    }
                    // Pad incomplete last row so alignment is preserved
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MENU TILE  (spec §5 + §12A – tile 112 × 92dp, corner 16dp, padding 16dp)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MenuTile(item: MenuItem, modifier: Modifier = Modifier) {
    Card(
        onClick   = {},
        modifier  = modifier.height(92.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MS_White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        border    = BorderStroke(1.dp, MS_Border)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            // Chevron pinned to top-right  (spec §12E: size 32dp, color #64748B, style Line)
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MS_ChevronTint,
                modifier           = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
            )

            // Icon  +  label  (centred vertically and horizontally)
            Column(
                modifier              = Modifier.fillMaxSize(),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                // Icon  (spec §12D: size 32dp, filled, corner bg 8dp, transparent bg)
                Icon(
                    imageVector        = item.icon,
                    contentDescription = null,
                    tint               = item.tint,
                    modifier           = Modifier.size(32.dp)
                )

                Spacer(Modifier.height(6.dp))

                // Label  (spec §12F: Poppins Medium 14sp #001B36 Center)
                Text(
                    text       = item.label,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MS_Navy,
                    textAlign  = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines   = 2
                )

                // Optional subtitle (e.g. "English", "v1.2.0")
                if (item.subtitle != null) {
                    Text(
                        text      = item.subtitle,
                        fontSize  = 10.sp,
                        color     = MS_TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTTOM NAVIGATION  (spec §7 – height 80dp, "More" tab active)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MoreBottomNav(onNavigateToHome: () -> Unit = {}, onNavigateToPayroll: () -> Unit = {}, onNavigateToWorkers: () -> Unit = {}) {
    Surface(
        color          = MS_White,
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
            // Left side of FAB
            MoreNavTab(icon = Icons.Default.Home,          label = "Home",       active = false, onClick = onNavigateToHome)
            MoreNavTab(icon = Icons.Default.Groups,        label = "Workers",    active = false, onClick = onNavigateToWorkers)

            // Gap reserved for the Scaffold FAB (56dp circle)
            Spacer(Modifier.width(56.dp))

            // Right side of FAB
            MoreNavTab(icon = Icons.Default.CalendarToday, label = "Attendance", active = false)
            MoreNavTab(icon = Icons.Default.Payments,      label = "Payroll",    active = false, onClick = onNavigateToPayroll)
            MoreNavTab(icon = Icons.Default.MoreHoriz,     label = "More",       active = true)
        }
    }
}

@Composable
private fun MoreNavTab(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (active) MS_Primary else MS_TextGray,
            modifier           = Modifier.size(24.dp)   // spec §7: icon 24dp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text       = label,
            fontSize   = 12.sp,                          // spec §7: label 12sp Medium
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (active) MS_Primary else MS_TextGray
        )
    }
}

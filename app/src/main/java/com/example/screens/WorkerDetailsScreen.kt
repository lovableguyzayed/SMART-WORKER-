package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
private val WD_Primary   = Color(0xFF0D5BFF)
private val WD_Green     = Color(0xFF22C55E)
private val WD_Orange    = Color(0xFFF59E0B)
private val WD_Purple    = Color(0xFF8B5CF6)
private val WD_Navy      = Color(0xFF0F172A)
private val WD_Body      = Color(0xFF334155)
private val WD_TextGray  = Color(0xFF64748B)
private val WD_Border    = Color(0xFFE2E8F0)
private val WD_Divider   = Color(0xFFF1F5F9)
private val WD_BG        = Color(0xFFF8FAFC)
private val WD_White     = Color(0xFFFFFFFF)
private val WD_GreenBg   = Color(0xFFDCFCE7)   // Active badge bg

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────
private data class WDQuickAction(
    val icon: ImageVector,
    val iconTint: Color,
    val label: String
)

private data class InfoRow(
    val label: String,
    val value: String,
    val valueSecondary: String? = null   // for Emergency Contact 2nd line
)

// ═══════════════════════════════════════════════════════════════════════════
//  ROOT SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SmartWorkerWorkerDetailsScreen(onBack: () -> Unit = {}) {
    Scaffold(
        containerColor = WD_BG,
        topBar         = { WorkerDetailsTopBar(onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileHeaderCard() }
            item { QuickActionsGrid() }
            item { WorkerInfoCard() }
            item { JobInfoCard() }
            item { Spacer(Modifier.height(8.dp)) }   // breathing room at bottom
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TOP APP BAR  (back arrow | "Worker Details" | ⋮ menu)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkerDetailsTopBar(onBack: () -> Unit) {
    Surface(color = WD_White, shadowElevation = 2.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WD_Navy)
            }

            Text(
                text       = "Worker Details",
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color      = WD_Navy,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.weight(1f)
            )

            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = WD_Navy)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PROFILE HEADER CARD
//  spec §4: height 96dp, corner 16dp, padding 16dp, avatar 64dp, badge 28dp
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ProfileHeaderCard() {
    Card(
        modifier  = Modifier.fillMaxWidth().height(96.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = WD_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, WD_Border)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar  64×64dp circle  (spec §8A)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBFDBFE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, null,
                    tint     = WD_Primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + badge · ID + role · Phone
            Column(
                modifier            = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Name row + "Active" badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Ramesh Kumar",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = WD_Navy
                    )
                    Spacer(Modifier.width(8.dp))
                    // Status badge  (spec §8B: height 28dp, radius 999dp, bg #DCFCE7)
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(WD_GreenBg, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Active",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color      = WD_Green
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                // ID + Role
                Text(
                    "ID: W001  •  Mason",
                    fontSize = 12.sp,
                    color    = WD_TextGray
                )

                Spacer(Modifier.height(3.dp))

                // Phone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone, null,
                        tint     = WD_TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("9876543210", fontSize = 12.sp, color = WD_TextGray)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  QUICK ACTIONS GRID
//  spec §5: 3 columns × 2 rows, button height 72dp, corner 12dp, icon 24dp
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun QuickActionsGrid() {
    val actions = listOf(
        WDQuickAction(Icons.Default.HowToReg,           WD_Primary, "Mark\nAttendance"),
        WDQuickAction(Icons.Default.AccountBalanceWallet,WD_Green,   "Add\nPayment"),
        WDQuickAction(Icons.Default.MonetizationOn,     WD_Purple,  "Add\nAdvance"),
        WDQuickAction(Icons.Default.Description,        WD_Orange,  "Documents"),
        WDQuickAction(Icons.Default.ManageAccounts,     WD_Primary, "Edit\nWorker"),
        WDQuickAction(Icons.Default.Apps,               WD_TextGray,"More")
    )

    val rows = actions.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowActions ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(action = action, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Single quick-action card  (spec §8C: 72dp height, corner 12dp, border #E2E8F0)
@Composable
private fun QuickActionCard(action: WDQuickAction, modifier: Modifier = Modifier) {
    Card(
        onClick   = {},
        modifier  = modifier.height(72.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = WD_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, WD_Border)
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center
        ) {
            // Icon  (spec §8F: 24×24dp, line/outline, colour per action)
            Icon(
                imageVector        = action.icon,
                contentDescription = null,
                tint               = action.iconTint,
                modifier           = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            // Label  (spec §5: 12sp Medium)
            Text(
                text       = action.label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = WD_Navy,
                textAlign  = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines   = 2
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  WORKER INFORMATION CARD
//  spec §6: card corner 16dp, padding 16dp, row 44dp, divider #F1F5F9
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WorkerInfoCard() {
    val rows = listOf(
        InfoRow("Father Name",       "Shyam Lal"),
        InfoRow("Date of Birth",     "12 Feb 1990"),
        InfoRow("Gender",            "Male"),
        InfoRow("Marital Status",    "Married"),
        InfoRow("Address",           "Lucknow, Uttar Pradesh"),
        InfoRow("Emergency Contact", "Suresh Kumar", "9876501234")
    )
    InfoCard(title = "Worker Information", rows = rows)
}

// ═══════════════════════════════════════════════════════════════════════════
//  JOB INFORMATION CARD
//  spec §7: same dimensions as Info card
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun JobInfoCard() {
    val rows = listOf(
        InfoRow("Work Type",      "Daily Wage"),
        InfoRow("Skill Category", "Mason"),
        InfoRow("Joining Date",   "15 Jan 2024"),
        InfoRow("Supervisor",     "Ravi Kumar")
    )
    InfoCard(title = "Job Information", rows = rows)
}

// ─────────────────────────────────────────────────────────────────────────────
//  REUSABLE INFO CARD  (shared by Worker Info + Job Info)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoCard(title: String, rows: List<InfoRow>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = WD_White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, WD_Border)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Section title  (spec §3: Card Title SemiBold 16sp)
            Text(
                text       = title,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = WD_Navy
            )
            Spacer(Modifier.height(8.dp))

            rows.forEachIndexed { i, row ->
                InfoRowItem(row = row)
                // Divider between rows (not after the last)
                if (i < rows.lastIndex) {
                    HorizontalDivider(
                        color     = WD_Divider,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

// Single row inside an info card
//  spec §6/§7: Row Height 44dp min, Label 14sp Regular, Value 14sp Medium
@Composable
private fun InfoRowItem(row: InfoRow) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)        // 44dp min; grows if value is multi-line
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Label  (left, Regular 14sp gray)
        Text(
            text     = row.label,
            fontSize = 14.sp,
            color    = WD_TextGray,
            modifier = Modifier.weight(0.42f)
        )

        // Value  (right, Medium 14sp dark)
        if (row.valueSecondary != null) {
            // Multi-line value (e.g. Emergency Contact: name + phone)
            Column(
                modifier            = Modifier.weight(0.58f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text       = row.value,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = WD_Navy,
                    textAlign  = TextAlign.End
                )
                Text(
                    text     = row.valueSecondary,
                    fontSize = 12.sp,
                    color    = WD_TextGray,
                    textAlign = TextAlign.End
                )
            }
        } else {
            Text(
                text       = row.value,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = WD_Navy,
                textAlign  = TextAlign.End,
                modifier   = Modifier.weight(0.58f)
            )
        }
    }
}

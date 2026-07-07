package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CardBorder
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.vm.AppViewModel

private data class MoreItem(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val adminOnly: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * More hub — every entry navigates to a working module. Admin-only management
 * entries are hidden from attendance users (role & permission model).
 */
@Composable
fun SmartWorkerMoreScreen(
    appVm: AppViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val user by appVm.currentUser.collectAsStateLifecycle()
    val company by appVm.company.collectAsStateLifecycle()
    val unread by appVm.unreadNotifications.collectAsStateLifecycle()
    val isAdmin = user?.isAdmin == true

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "More",
                unreadCount = unread,
                onNotifications = { onNavigate("notifications") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = CardBorder,
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user?.fullName ?: "—", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                            Text(
                                (user?.role ?: "").replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue,
                            )
                            company?.let { Text(it.name, fontSize = 12.sp, color = TextSecondary) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
            item {
                SectionTitle("Operations")
                MenuList(
                    isAdmin,
                    listOf(
                        MoreItem(Icons.Filled.Groups, "Workers", Purple) { onNavigate("workers") },
                        MoreItem(Icons.Filled.QrCodeScanner, "Quick Mark / Scan", PrimaryBlue) { onNavigate("quick_mark") },
                        MoreItem(Icons.Filled.AccountBalanceWallet, "Transactions", Success, adminOnly = true) { onNavigate("transactions") },
                        MoreItem(Icons.Filled.Payments, "Payroll", Warning, adminOnly = true) { onNavigate("payroll") },
                    ),
                )
            }

            if (isAdmin) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    SectionTitle("Management")
                    MenuList(
                        isAdmin,
                        listOf(
                            MoreItem(Icons.Filled.Business, "Sites, Projects, Tasks & Departments", PrimaryBlue, adminOnly = true) { onNavigate("manage") },
                            MoreItem(Icons.Filled.EventBusy, "Closure Days", Danger, adminOnly = true) { onNavigate("closures") },
                            MoreItem(Icons.Filled.ManageAccounts, "Attendance Users", Purple, adminOnly = true) { onNavigate("attendance_users") },
                            MoreItem(Icons.Filled.Settings, "Company Settings", Navy, adminOnly = true) { onNavigate("company_settings") },
                            MoreItem(Icons.Filled.CloudSync, "Cloud Backup — Sync Now", Success, adminOnly = true) { appVm.syncNow() },
                        ),
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                SectionTitle("Reports & Alerts")
                MenuList(
                    isAdmin,
                    listOf(
                        MoreItem(Icons.Filled.PieChart, "Reports & Export", PrimaryBlue, adminOnly = true) { onNavigate("reports") },
                        MoreItem(
                            Icons.Filled.Notifications,
                            if (unread > 0) "Notifications ($unread)" else "Notifications",
                            Warning,
                        ) { onNavigate("notifications") },
                    ),
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Danger.copy(alpha = 0.1f))
                        .clickable(onClick = onLogout)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text("Log Out", color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun MenuList(isAdmin: Boolean, items: List<MoreItem>) {
    val visible = items.filter { !it.adminOnly || isAdmin }
    if (visible.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column {
            visible.forEachIndexed { i, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).background(item.tint.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        item.label,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                if (i < visible.lastIndex) HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(start = 64.dp))
            }
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.AttendanceStatus
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
import com.example.ui.vm.AppViewModel
import com.example.util.LocalImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Teal500 = Color(0xFF14B8A6)
private val Indigo500 = Color(0xFF6366F1)

/**
 * Profile / More hub — mirrors the web app's `profile.html`: gradient profile
 * header, quick stats, account information, permissions, quick actions,
 * system information and logout.
 */
@Composable
fun SmartWorkerMoreScreen(
    appVm: AppViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val container = LocalAppContainer.current
    val user by appVm.currentUser.collectAsStateLifecycle()
    val company by appVm.company.collectAsStateLifecycle()
    val unread by appVm.unreadNotifications.collectAsStateLifecycle()
    val workers by container.workerRepository.activeWorkers.collectAsStateWithLifecycle(initialValue = emptyList())
    val todayRecords by container.attendanceRepository.recordsOn(LocalDate.now())
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val u = user ?: return
    val isAdmin = u.isAdmin
    val presentToday = todayRecords.count {
        it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE
    }

    Scaffold(containerColor = BackgroundColor) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ProfileHeaderCard(u.fullName, u.role, company?.name, company?.logo) }

            // ── Quick stats ──
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileStat("Total Workers", workers.size.toString(), Icons.Filled.Groups, PrimaryBlue, Modifier.weight(1f))
                    ProfileStat("Present Today", presentToday.toString(), Icons.Filled.CheckCircle, Success, Modifier.weight(1f))
                }
            }

            // ── Account information ──
            item {
                InfoSection("Account Information") {
                    KeyValue("Full Name", u.fullName)
                    KeyValue("Username", u.username)
                    KeyValue("Email", u.email.ifBlank { "—" })
                    KeyValue("Role", if (isAdmin) "System Administrator" else u.role.replaceFirstChar { it.uppercase() })
                    u.phone?.takeIf { it.isNotBlank() }?.let { KeyValue("Mobile", it) }
                    KeyValue("Member Since", u.createdAt.format(DateTimeFormatter.ofPattern("MMMM yyyy")), last = true)
                }
            }

            // ── Permissions ──
            item {
                InfoSection("Permissions") {
                    if (isAdmin) {
                        listOf(
                            "Manage workers & payroll",
                            "Mark and edit attendance",
                            "Company settings & closures",
                            "Transactions & reports",
                            "Manage attendance users",
                        ).forEach { PermissionRow(it, granted = true) }
                    } else {
                        listOf("Mark attendance", "Scan QR codes", "View workers on assigned sites")
                            .forEach { PermissionRow(it, granted = true) }
                        listOf("Edit attendance", "Modify payroll", "Change worker records", "Admin settings")
                            .forEach { PermissionRow(it, granted = false) }
                    }
                }
            }

            // ── Quick actions ──
            item {
                InfoSection("Quick Actions") {
                    val items = buildList {
                        if (isAdmin) {
                            add(Triple("Company Settings", Icons.Filled.Settings, PrimaryBlue) to "company_settings")
                            add(Triple("Manage Sites & Projects", Icons.Filled.Tune, Indigo500) to "manage")
                            add(Triple("Assignments", Icons.Filled.AccountTree, Success) to "assignments")
                            add(Triple("Attendance Users", Icons.Filled.Person, Purple) to "attendance_users")
                            add(Triple("Closure Days", Icons.Filled.EventBusy, Warning) to "closures")
                            add(Triple("Transactions", Icons.Filled.Payments, Teal500) to "transactions")
                            add(Triple("Reports & Export", Icons.Filled.Description, Navy) to "reports")
                        } else {
                            add(Triple("QR Scanner", Icons.Filled.QrCodeScanner, Indigo500) to "quick_mark")
                            add(Triple("Attendance", Icons.Filled.CalendarMonth, Success) to "attendance")
                        }
                        add(Triple("Notifications", Icons.Filled.Notifications, Warning) to "notifications")
                    }
                    items.forEachIndexed { i, (spec, dest) ->
                        val (label, icon, tint) = spec
                        ActionRow(
                            label = label,
                            icon = icon,
                            tint = tint,
                            badge = if (dest == "notifications" && unread > 0) unread else null,
                            last = i == items.lastIndex,
                        ) { onNavigate(dest) }
                    }
                }
            }

            // ── Cloud backup ──
            item {
                InfoSection("Cloud Backup") {
                    ActionRow("Sync Now", Icons.Filled.CloudSync, PrimaryBlue, last = true) { appVm.syncNow() }
                }
            }

            // ── System information ──
            item {
                InfoSection("System Information") {
                    KeyValue("Application", "SmartWorker v${BuildConfig.VERSION_NAME}")
                    KeyValue("Build", BuildConfig.VERSION_CODE.toString())
                    KeyValue("Database", "Connected · offline-first")
                    KeyValue("Today", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), last = true)
                }
            }

            // ── Logout ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Danger.copy(alpha = 0.1f))
                        .clickable(onClick = onLogout)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Log Out", color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeaderCard(fullName: String, role: String, companyName: String?, logo: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1E3A8A), DarkBlue)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp)) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalImage(logo, "Profile", Modifier.size(88.dp).clip(CircleShape)) {
                        Text(
                            fullName.take(1).uppercase(),
                            color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                        .size(28.dp).clip(CircleShape).background(White),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Business, null, tint = PrimaryBlue, modifier = Modifier.size(15.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text(fullName, color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(White.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Text(
                    if (role == "admin") "System Administrator" else role.replaceFirstChar { it.uppercase() },
                    color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
            }
            companyName?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Building blocks ───────────────────────────────────────────────────────────
@Composable
private fun InfoSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp),
            border = CardBorder,
        ) { Column(content = content) }
    }
}

@Composable
private fun KeyValue(label: String, value: String, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
    if (!last) HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background((if (granted) Success else Danger).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (granted) Icons.Filled.Check else Icons.Filled.Close,
                if (granted) "Allowed" else "Not allowed",
                tint = if (granted) Success else Danger,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label, fontSize = 13.sp,
            color = if (granted) Navy else TextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    icon: ImageVector,
    tint: Color,
    badge: Int? = null,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                Modifier.clip(CircleShape).background(Danger).padding(horizontal = 7.dp, vertical = 2.dp),
            ) { Text(badge.toString(), color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp),
        )
    }
    if (!last) HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(start = 62.dp))
}

@Composable
private fun ProfileStat(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

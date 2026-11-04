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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.AttendanceStatus
import com.example.ui.LocalAppContainer
import com.example.ui.StatTile
import com.example.ui.SwCard
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Lime
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Purple
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.AppViewModel
import com.example.util.LocalImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Profile / More hub — identity, what this account may do, and every admin
 * destination that isn't a tab.
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
    val workers by container.workerRepository.activeWorkers
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val todayRecords by container.attendanceRepository.recordsOn(LocalDate.now())
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val u = user ?: return
    val isAdmin = u.isAdmin
    val presentToday = todayRecords.count {
        it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE
    }

    Scaffold(containerColor = com.example.ui.theme.Ink) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Identity ──
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Flame),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(company?.logo, null, Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))) {
                            Text(
                                u.fullName.take(1).uppercase(),
                                color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            u.fullName, style = SwType.Headline.copy(fontSize = 22.sp), color = OnDark,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.clip(CircleShape)
                                    .background((if (isAdmin) Flame else Lime).copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    if (isAdmin) "Administrator" else "Attendance user",
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) Flame else Lime,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                company?.name ?: "SmartWorker",
                                style = SwType.Caption, color = OnDarkMuted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile(
                        workers.size.toString(), "Active workers",
                        Modifier.weight(1f).height(100.dp), container = Volt,
                    )
                    StatTile(
                        presentToday.toString(), "Present today",
                        Modifier.weight(1f).height(100.dp), container = Lime,
                    )
                }
            }

            // ── Destinations ──
            item {
                val destinations = buildList {
                    if (isAdmin) {
                        add(Dest("Company settings", "Branding on payslips & ID cards", Icons.Filled.Settings, Flame, "company_settings"))
                        add(Dest("Sites & projects", "Sites, projects, tasks, departments", Icons.Filled.Tune, Volt, "manage"))
                        add(Dest("Assignments", "Who works where", Icons.Filled.AccountTree, Lime, "assignments"))
                        add(Dest("Attendance users", "Accounts that may mark attendance", Icons.Filled.Person, Purple, "attendance_users"))
                        add(Dest("Closure days", "Holidays and shutdowns", Icons.Filled.EventBusy, Sun, "closures"))
                        add(Dest("Transactions", "Advances, bonuses, deductions", Icons.Filled.Payments, Coral, "transactions"))
                        add(Dest("Reports & export", "Per-worker reports and CSV", Icons.Filled.Description, Volt, "reports"))
                    } else {
                        add(Dest("QR scanner", "Mark by worker ID", Icons.Filled.QrCodeScanner, Flame, "quick_mark"))
                        add(Dest("Attendance", "Mark or review the day", Icons.Filled.CalendarMonth, Lime, "attendance"))
                    }
                    add(Dest("Notifications", "Alerts and reminders", Icons.Filled.Notifications, Sun, "notifications"))
                }
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Manage", style = SwType.Headline.copy(fontSize = 18.sp), color = OnDark)
                    Spacer(Modifier.height(2.dp))
                    destinations.forEach { d ->
                        DestinationRow(
                            dest = d,
                            badge = if (d.route == "notifications" && unread > 0) unread else null,
                            onClick = { onNavigate(d.route) },
                        )
                    }
                }
            }

            // ── What this account may do ──
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SwCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("PERMISSIONS", style = SwType.Label, color = OnDarkMuted)
                            Spacer(Modifier.height(12.dp))
                            if (isAdmin) {
                                listOf(
                                    "Manage workers & payroll",
                                    "Mark and edit attendance",
                                    "Company settings & closures",
                                    "Transactions & reports",
                                    "Manage attendance users",
                                ).forEach { Permission(it, true) }
                            } else {
                                listOf(
                                    "Mark attendance",
                                    "Scan QR codes",
                                    "View workers on assigned sites",
                                ).forEach { Permission(it, true) }
                                listOf(
                                    "Edit attendance",
                                    "Modify payroll",
                                    "Change worker records",
                                    "Admin settings",
                                ).forEach { Permission(it, false) }
                            }
                        }
                    }
                }
            }

            // ── Backup + build ──
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SwCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .clickable { appVm.syncNow() },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CloudSync, null, tint = Volt, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Sync now", style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, null, tint = OnDarkFaint, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            KeyValue("Application", "SmartWorker v${BuildConfig.VERSION_NAME}")
                            KeyValue("Build", BuildConfig.VERSION_CODE.toString())
                            KeyValue("Database", "Connected · offline-first")
                            KeyValue(
                                "Signed in since",
                                u.createdAt.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Coral.copy(alpha = 0.13f))
                        .clickable(onClick = onLogout)
                        .padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Coral, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Log out", style = SwType.Title.copy(fontSize = 15.sp), color = Coral)
                }
            }
        }
    }
}

private data class Dest(
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String,
)

@Composable
private fun DestinationRow(dest: Dest, badge: Int?, onClick: () -> Unit) {
    SwCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                    .background(dest.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(dest.icon, null, tint = dest.accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dest.label, style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dest.sub, style = SwType.Caption, color = OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (badge != null) {
                Box(
                    Modifier.clip(CircleShape).background(Coral).padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (badge > 9) "9+" else "$badge",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = White,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = OnDarkFaint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Permission(label: String, granted: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(19.dp).clip(CircleShape)
                .background((if (granted) Lime else Surface3).copy(alpha = if (granted) 0.2f else 1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (granted) Icons.Filled.Check else Icons.Filled.Close,
                null,
                tint = if (granted) Lime else OnDarkFaint,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            style = SwType.Body.copy(fontSize = 13.5.sp),
            color = if (granted) OnDark else OnDarkFaint,
        )
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, style = SwType.Caption, color = OnDarkMuted, modifier = Modifier.weight(1f))
        Text(
            value, style = SwType.Caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            color = OnDark, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

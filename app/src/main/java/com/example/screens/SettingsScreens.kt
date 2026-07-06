package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClosureDay
import com.example.data.model.ClosureScope
import com.example.data.model.CompanySetting
import com.example.data.model.User
import com.example.ui.CardBorder
import com.example.ui.EmptyState
import com.example.ui.StatusPill
import com.example.ui.SwDateField
import com.example.ui.SwFormDropdown
import com.example.ui.SwFormField
import com.example.ui.SwSwitchRow
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.theme.White
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ═════════════════════════════════════════════════════════════════════════════
//  COMPANY SETTINGS  (branding shown on payslips / PDF exports)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun CompanySettingsScreen(vm: com.example.ui.vm.SettingsViewModel, onBack: () -> Unit) {
    val company by vm.company.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var reg by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(company) {
        val c = company
        if (c != null && !loaded) {
            name = c.name; address = c.address; phone = c.phone; email = c.email
            website = c.website; gst = c.gstNumber; reg = c.registrationNumber
            loaded = true
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Company Settings",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Shown on payslips, reports and PDF exports.",
                fontSize = 13.sp, color = TextSecondary,
            )
            SwFormField("Company name *", name) { name = it }
            SwFormField("Address", address, singleLine = false) { address = it }
            SwFormField("Phone", phone, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone) { phone = it }
            SwFormField("Email", email, keyboardType = androidx.compose.ui.text.input.KeyboardType.Email) { email = it }
            SwFormField("Website", website) { website = it }
            SwFormField("GST number", gst) { gst = it }
            SwFormField("Registration number", reg) { reg = it }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.saveCompany(
                        CompanySetting(
                            name = name.trim().ifBlank { "SmartWorker" },
                            address = address, phone = phone, email = email,
                            website = website, gstNumber = gst, registrationNumber = reg,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) { Text("Save Company Details", fontWeight = FontWeight.SemiBold) }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } },
            text = { Text(it) },
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  ATTENDANCE USERS  (authorized helpers scoped to sites/projects)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AttendanceUsersScreen(vm: com.example.ui.vm.SettingsViewModel, onBack: () -> Unit) {
    val users by vm.attendanceUsers.collectAsStateLifecycle()
    val sites by vm.sites.collectAsStateLifecycle()
    val projects by vm.projects.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()

    var editUser by remember { mutableStateOf<User?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<User?>(null) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Attendance Users",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = PrimaryBlue, contentColor = White, shape = CircleShape,
            ) { Icon(Icons.Filled.Add, "Add attendance user") }
        },
    ) { padding ->
        if (users.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    Icons.Filled.ManageAccounts, "No attendance users",
                    "Attendance users can mark check-ins/outs for workers on their assigned sites or projects only.",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(users, key = { it.id }) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(u.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                                    Spacer(Modifier.size(8.dp))
                                    StatusPill(
                                        if (u.status == "active") "Active" else "Disabled",
                                        if (u.status == "active") Success else TextSecondary,
                                    )
                                }
                                Text("@${u.username}", fontSize = 12.sp, color = TextSecondary)
                                val scope = buildList {
                                    val sn = sites.filter { it.id in u.siteIdList }.map { it.name }
                                    val pn = projects.filter { it.id in u.projectIdList }.map { it.name }
                                    if (sn.isNotEmpty()) add("Sites: ${sn.joinToString()}")
                                    if (pn.isNotEmpty()) add("Projects: ${pn.joinToString()}")
                                }
                                Text(
                                    if (scope.isEmpty()) "Scope: all sites & projects" else scope.joinToString(" • "),
                                    fontSize = 11.sp, color = TextSecondary,
                                )
                            }
                            IconButton(onClick = { editUser = u }) {
                                Icon(Icons.Filled.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { vm.toggleUser(u.id) }) {
                                Icon(
                                    Icons.Filled.Block,
                                    if (u.status == "active") "Disable" else "Enable",
                                    tint = Warning, modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(onClick = { confirmDelete = u }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAdd || editUser != null) {
        AttendanceUserDialog(
            existing = editUser,
            siteOptions = sites.filter { it.status == "active" }.map { it.id to it.name },
            projectOptions = projects.filter { it.status != "archived" }.map { it.id to it.name },
            onDismiss = { showAdd = false; editUser = null },
            onSave = { id, username, fullName, phone, password, siteIds, projectIds ->
                vm.saveAttendanceUser(id, username, fullName, phone, password, siteIds, projectIds)
                showAdd = false; editUser = null
            },
        )
    }

    confirmDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${u.fullName}?") },
            text = { Text("They will no longer be able to log in. Attendance they already marked is kept.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteUser(u.id); confirmDelete = null }) { Text("Delete", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } },
            text = { Text(it) },
        )
    }
}

@Composable
private fun AttendanceUserDialog(
    existing: User?,
    siteOptions: List<Pair<Long, String>>,
    projectOptions: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, String?, String?, List<Long>, List<Long>) -> Unit,
) {
    var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var password by remember { mutableStateOf("") }
    val selectedSites = remember { (existing?.siteIdList ?: emptyList()).toMutableStateList() }
    val selectedProjects = remember { (existing?.projectIdList ?: emptyList()).toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Attendance User" else "Edit Attendance User") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SwFormField("Full name *", fullName) { fullName = it }
                SwFormField("Username *", username) { username = it }
                SwFormField("Phone", phone, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone) { phone = it }
                SwFormField(
                    if (existing == null) "Password *" else "New password (blank = keep)",
                    password,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                ) { password = it }

                Text("Site access (none selected = all)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
                ChipRow(siteOptions, selectedSites)
                Text("Project access (none selected = all)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
                ChipRow(projectOptions, selectedProjects)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    existing?.id, username, fullName, phone.ifBlank { null },
                    password.ifBlank { null },
                    selectedSites.toList(), selectedProjects.toList(),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChipRow(options: List<Pair<Long, String>>, selected: MutableList<Long>) {
    if (options.isEmpty()) {
        Text("None available yet.", fontSize = 12.sp, color = TextSecondary)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { (id, label) ->
                    FilterChip(
                        selected = id in selected,
                        onClick = { if (id in selected) selected.remove(id) else selected.add(id) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                            selectedLabelColor = PrimaryBlue,
                        ),
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  CLOSURE DAYS  (holidays / site / project closures)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ClosuresScreen(vm: com.example.ui.vm.SettingsViewModel, onBack: () -> Unit) {
    val closures by vm.closures.collectAsStateLifecycle()
    val sites by vm.sites.collectAsStateLifecycle()
    val projects by vm.projects.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Closure Days",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = PrimaryBlue, contentColor = White, shape = CircleShape,
            ) { Icon(Icons.Filled.Add, "Add closure day") }
        },
    ) { padding ->
        if (closures.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    Icons.Filled.EventBusy, "No upcoming closures",
                    "Add holidays or site/project closures. Locked closures block attendance marking.",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(closures, key = { it.id }) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.date.format(fmt), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                                Text(c.reason, fontSize = 13.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(c.type.replaceFirstChar { it.uppercase() }, PrimaryBlue)
                                    Spacer(Modifier.size(6.dp))
                                    val scopeLabel = when (c.scope) {
                                        ClosureScope.SITE -> "Site: ${sites.firstOrNull { it.id == c.siteId }?.name ?: "?"}"
                                        ClosureScope.PROJECT -> "Project: ${projects.firstOrNull { it.id == c.projectId }?.name ?: "?"}"
                                        else -> "Company-wide"
                                    }
                                    StatusPill(scopeLabel, TextSecondary)
                                    Spacer(Modifier.size(6.dp))
                                    StatusPill(
                                        if (c.allowAttendance) "Attendance allowed" else "Attendance locked",
                                        if (c.allowAttendance) Success else Danger,
                                    )
                                }
                            }
                            IconButton(onClick = { vm.deleteClosure(c) }) {
                                Icon(Icons.Filled.Delete, "Delete closure", tint = Danger, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAdd) {
        ClosureDialog(
            sites = sites.filter { it.status == "active" }.map { it.id to it.name },
            projects = projects.filter { it.status != "archived" }.map { it.id to it.name },
            onDismiss = { showAdd = false },
            onSave = { date, reason, type, scope, siteId, projectId, allow ->
                vm.saveClosure(date, reason, type, scope, siteId, projectId, allow)
                showAdd = false
            },
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } },
            text = { Text(it) },
        )
    }
}

@Composable
private fun ClosureDialog(
    sites: List<Pair<Long, String>>,
    projects: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String, String, Long?, Long?, Boolean) -> Unit,
) {
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("holiday") }
    var scope by remember { mutableStateOf(ClosureScope.COMPANY) }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var allow by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Closure Day") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SwDateField("Date", date) { date = it }
                SwFormField("Reason *", reason) { reason = it }
                SwFormDropdown(
                    "Type", type,
                    listOf("holiday", "site", "project", "emergency", "maintenance"),
                    display = { it.replaceFirstChar { c -> c.uppercase() } },
                ) { type = it }
                SwFormDropdown(
                    "Scope", scope,
                    listOf(ClosureScope.COMPANY, ClosureScope.SITE, ClosureScope.PROJECT),
                    display = {
                        when (it) {
                            ClosureScope.SITE -> "Single site"
                            ClosureScope.PROJECT -> "Single project"
                            else -> "Company-wide"
                        }
                    },
                ) { scope = it }
                if (scope == ClosureScope.SITE) {
                    SwFormDropdown(
                        "Site", sites.firstOrNull { it.first == siteId }?.second ?: "Choose…",
                        sites.map { it.second },
                    ) { chosen -> siteId = sites.firstOrNull { it.second == chosen }?.first }
                }
                if (scope == ClosureScope.PROJECT) {
                    SwFormDropdown(
                        "Project", projects.firstOrNull { it.first == projectId }?.second ?: "Choose…",
                        projects.map { it.second },
                    ) { chosen -> projectId = projects.firstOrNull { it.second == chosen }?.first }
                }
                SwSwitchRow(
                    "Allow attendance",
                    "Off = attendance locked for covered workers on this day.",
                    allow,
                ) { allow = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                date?.let { onSave(it, reason, type, scope, siteId, projectId, allow) }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

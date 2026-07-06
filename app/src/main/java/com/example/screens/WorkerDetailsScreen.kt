package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceRecord
import com.example.data.model.PayTypes
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.StatusPill
import com.example.ui.SwDateField
import com.example.ui.SwFormDropdown
import com.example.ui.SwFormField
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
import com.example.ui.vm.WorkerAdminViewModel
import com.example.util.LocalImage
import androidx.compose.material.icons.filled.Badge
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SmartWorkerWorkerDetailsScreen(
    workerId: Long,
    user: User,
    adminVm: WorkerAdminViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenReport: (Long) -> Unit,
    onOpenIdCard: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val worker by remember(workerId) { container.workerRepository.worker(workerId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val recentAttendance by remember(workerId) {
        if (workerId > 0) container.attendanceRepository.recentForWorker(workerId, 10) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentTxns by remember(workerId) {
        if (workerId > 0) container.catalogRepository.recentTransactionsForWorker(workerId, 10) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(workerId) { adminVm.setWorker(workerId) }
    val assignments by adminVm.assignments.collectAsStateLifecycle()
    val modifications by adminVm.modifications.collectAsStateLifecycle()
    val sites by adminVm.sites.collectAsStateLifecycle()
    val projects by adminVm.projects.collectAsStateLifecycle()
    val tasks by adminVm.tasks.collectAsStateLifecycle()

    var dialog by remember { mutableStateOf<String?>(null) } // assign | promote | leave | deactivate
    var toast by remember { mutableStateOf<String?>(null) }

    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Worker Details",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        val w = worker
        if (w == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ProfileHeader(w) }

            // Admin quick actions (Flask worker_profile action bar)
            if (user.isAdmin) {
                item {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile("Edit\nWorker", Icons.Filled.ManageAccounts, PrimaryBlue, Modifier.weight(1f)) { onEdit(w.id) }
                            ActionTile("Assign\nProject", Icons.AutoMirrored.Filled.Assignment, Purple, Modifier.weight(1f)) { dialog = "assign" }
                            ActionTile("Promote /\nRate", Icons.Filled.TrendingUp, Success, Modifier.weight(1f)) { dialog = "promote" }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile("Adjust\nLeave", Icons.Filled.EventAvailable, Warning, Modifier.weight(1f)) { dialog = "leave" }
                            ActionTile("Full\nReport", Icons.Filled.Description, Navy, Modifier.weight(1f)) { onOpenReport(w.id) }
                            ActionTile("ID Card\n+ QR", Icons.Filled.Badge, PrimaryBlue, Modifier.weight(1f)) { onOpenIdCard(w.id) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile(
                                if (w.status == "active") "Deactivate" else "Reactivate",
                                Icons.Filled.PersonOff, Danger, Modifier.weight(1f),
                            ) { dialog = "deactivate" }
                            Spacer(Modifier.weight(2f))
                        }
                    }
                }
            }

            item { InfoCard("Job Information", jobRows(w)) }

            // Assignments (current + history)
            item {
                DetailsSection("Assignments") {
                    if (assignments.isEmpty()) {
                        Text("Not assigned to any site or project yet.", fontSize = 13.sp, color = TextSecondary)
                    } else {
                        assignments.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    val project = projects.firstOrNull { it.id == a.projectId }?.name
                                    val site = sites.firstOrNull { it.id == a.siteId }?.name
                                    val task = tasks.firstOrNull { it.id == a.taskId }?.name
                                    Text(
                                        listOfNotNull(project, site).joinToString(" @ ").ifBlank { "—" },
                                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy,
                                    )
                                    Text(
                                        buildString {
                                            append(a.startDate.format(dateFmt))
                                            append(" → ")
                                            append(a.endDate?.format(dateFmt) ?: "ongoing")
                                            if (task != null) append(" • $task")
                                        },
                                        fontSize = 11.sp, color = TextSecondary,
                                    )
                                }
                                StatusPill(
                                    a.status.replaceFirstChar { it.uppercase() },
                                    when (a.status) { "active" -> Success; "transferred" -> Warning; else -> TextSecondary },
                                )
                                if (user.isAdmin && a.status == "active") {
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        "End",
                                        fontSize = 12.sp, color = Danger, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { adminVm.endAssignment(a.id) { toast = it } }
                                            .padding(6.dp),
                                    )
                                }
                            }
                            HorizontalDivider(color = SubtleDivider)
                        }
                    }
                }
            }

            // Modification / promotion history (permanent audit log)
            if (modifications.isNotEmpty()) {
                item {
                    DetailsSection("Change History") {
                        modifications.take(15).forEach { m ->
                            Column(Modifier.padding(vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        m.modType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                        when (m.modType) {
                                            "promotion" -> Success
                                            "salary_change" -> PrimaryBlue
                                            "leave_grant" -> Warning
                                            else -> TextSecondary
                                        },
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        m.createdAt.toLocalDate().format(dateFmt),
                                        fontSize = 11.sp, color = TextSecondary,
                                    )
                                }
                                if (m.fieldName != null) {
                                    Text(
                                        "${m.fieldName}: ${m.oldValue ?: "—"} → ${m.newValue ?: "—"}",
                                        fontSize = 12.sp, color = Navy,
                                    )
                                }
                                m.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                            HorizontalDivider(color = SubtleDivider)
                        }
                    }
                }
            }

            item {
                DetailsSection("Recent Attendance") {
                    if (recentAttendance.isEmpty()) {
                        Text("No attendance records yet.", fontSize = 13.sp, color = TextSecondary)
                    } else {
                        recentAttendance.forEach { r -> AttendanceMiniRow(r, dateFmt) }
                    }
                }
            }

            if (recentTxns.isNotEmpty()) {
                item {
                    DetailsSection("Recent Transactions") {
                        recentTxns.forEach { t -> TxnMiniRow(t, dateFmt) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        // ── Dialogs ─────────────────────────────────────────────────────────
        when (dialog) {
            "assign" -> AssignDialog(
                sites = sites.map { it.id to it.name },
                projects = projects.map { it.id to it.name },
                tasks = tasks.map { it.id to it.name },
                onDismiss = { dialog = null },
                onSave = { p, s, t, d, n ->
                    adminVm.assign(p, s, t, d, n) { toast = it }
                    dialog = null
                },
            )
            "promote" -> PromoteDialog(
                current = w,
                onDismiss = { dialog = null },
                onSave = { pos, rate, desc ->
                    adminVm.promote(pos, rate, desc) { toast = it }
                    dialog = null
                },
            )
            "leave" -> LeaveAdjustDialog(
                onDismiss = { dialog = null },
                onSave = { days, reason ->
                    adminVm.adjustLeave(days, reason) { toast = it }
                    dialog = null
                },
            )
            "deactivate" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(if (w.status == "active") "Deactivate ${w.fullName}?" else "Reactivate ${w.fullName}?") },
                text = {
                    Text(
                        if (w.status == "active")
                            "They will disappear from attendance and payroll. History is kept and they can be reactivated."
                        else "They will appear again in attendance and payroll runs."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        adminVm.toggleActive { toast = it }
                        dialog = null
                    }) { Text("Confirm", color = if (w.status == "active") Danger else Success) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
            )
        }

        toast?.let {
            AlertDialog(
                onDismissRequest = { toast = null },
                confirmButton = { TextButton(onClick = { toast = null }) { Text("OK") } },
                text = { Text(it) },
            )
        }
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Navy,
                textAlign = TextAlign.Center, lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun DetailsSection(title: String, content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun AssignDialog(
    sites: List<Pair<Long, String>>,
    projects: List<Pair<Long, String>>,
    tasks: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long?, Long?, Long?, LocalDate, String) -> Unit,
) {
    var projectId by remember { mutableStateOf<Long?>(null) }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var taskId by remember { mutableStateOf<Long?>(null) }
    var start by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign / Transfer") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Saving closes any current active assignment (transfer).",
                    fontSize = 12.sp, color = TextSecondary,
                )
                SwFormDropdown(
                    "Project", projects.firstOrNull { it.first == projectId }?.second ?: "None",
                    listOf("None") + projects.map { it.second },
                ) { chosen -> projectId = projects.firstOrNull { it.second == chosen }?.first }
                SwFormDropdown(
                    "Site", sites.firstOrNull { it.first == siteId }?.second ?: "None",
                    listOf("None") + sites.map { it.second },
                ) { chosen -> siteId = sites.firstOrNull { it.second == chosen }?.first }
                SwFormDropdown(
                    "Task", tasks.firstOrNull { it.first == taskId }?.second ?: "None",
                    listOf("None") + tasks.map { it.second },
                ) { chosen -> taskId = tasks.firstOrNull { it.second == chosen }?.first }
                SwDateField("Start date", start) { start = it }
                SwFormField("Notes", notes) { notes = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { start?.let { onSave(projectId, siteId, taskId, it, notes) } }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PromoteDialog(
    current: Worker,
    onDismiss: () -> Unit,
    onSave: (String?, Double?, String?) -> Unit,
) {
    var position by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val rateLabel = when (current.payType) {
        PayTypes.MONTHLY -> "New monthly salary (₹)"
        PayTypes.DAILY -> "New daily rate (₹)"
        PayTypes.HOURLY -> "New hourly rate (₹)"
        else -> "New project rate (₹)"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promotion / Rate Change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Current: ${current.position} — recorded permanently in change history.",
                    fontSize = 12.sp, color = TextSecondary,
                )
                SwFormField("New designation (blank = keep)", position) { position = it }
                SwFormField(
                    "$rateLabel (blank = keep)", rate,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ) { rate = it }
                SwFormField("Reason / notes", desc) { desc = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(position.ifBlank { null }, rate.toDoubleOrNull(), desc.ifBlank { null })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LeaveAdjustDialog(onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var days by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Leave Balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Positive credits extra leave days; negative debits the balance. Applied in the payroll leave ledger.",
                    fontSize = 12.sp, color = TextSecondary,
                )
                SwFormField(
                    "Days (e.g. 2 or -1.5)", days,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ) { days = it }
                SwFormField("Reason", reason) { reason = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { days.toDoubleOrNull()?.let { onSave(it, reason) } }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProfileHeader(w: Worker) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).background(AvatarBlueBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                LocalImage(
                    path = w.profileImage,
                    contentDescription = "Photo of ${'$'}{w.fullName}",
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                ) {
                    Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.fullName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                    Spacer(Modifier.size(8.dp))
                    StatusPill(if (w.status == "active") "Active" else "Inactive", if (w.status == "active") Success else TextSecondary)
                }
                Spacer(Modifier.size(3.dp))
                Text("ID: ${w.workerCode} • ${w.position}", fontSize = 12.sp, color = TextSecondary)
                Text(w.phone, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

private fun jobRows(w: Worker): List<Pair<String, String>> {
    val rate = when (w.payType) {
        PayTypes.DAILY -> "₹${w.dailyRate?.toInt() ?: 0} / day"
        PayTypes.MONTHLY -> "₹${w.monthlySalary?.toInt() ?: 0} / month"
        PayTypes.HOURLY -> "₹${w.hourlyRate?.toInt() ?: 0} / hour"
        else -> "₹${w.projectRate?.toInt() ?: 0} / project"
    }
    return listOf(
        "Department" to w.department,
        "Worker Category" to w.employeeType,
        "Pay Type" to w.payType.replaceFirstChar { it.uppercase() },
        "Pay Rate" to rate,
        "Joining Date" to w.joinDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
        "Overtime" to if (w.overtimeEnabled) "Enabled" else "Disabled",
    )
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(8.dp))
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    Modifier.fillMaxWidth().height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, fontSize = 14.sp, color = TextSecondary)
                    Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy)
                }
                if (i < rows.lastIndex) HorizontalDivider(color = SubtleDivider)
            }
        }
    }
}

@Composable
private fun AttendanceMiniRow(r: AttendanceRecord, fmt: DateTimeFormatter) {
    Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(r.date.format(fmt), fontSize = 13.sp, color = Navy)
        val (label, color) = when (r.status) {
            "present" -> "Present" to Success
            "late" -> "Late" to Warning
            "absent" -> "Absent" to Danger
            else -> "Leave" to Purple
        }
        StatusPill(label, color)
    }
}

@Composable
private fun TxnMiniRow(t: WorkerTransaction, fmt: DateTimeFormatter) {
    Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${com.example.data.model.TxnTypes.LABELS[t.txnType] ?: t.txnType} • ${t.date.format(fmt)}", fontSize = 13.sp, color = Navy)
        Text(
            "${if (t.isEarning) "+" else "-"}₹${"%,.0f".format(t.amount)}",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (t.isEarning) Success else Danger,
        )
    }
}

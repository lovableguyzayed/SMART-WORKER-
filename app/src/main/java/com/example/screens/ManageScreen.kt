package com.example.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.Project
import com.example.data.model.Site
import com.example.data.model.WorkTask
import com.example.ui.CardBorder
import com.example.ui.EmptyState
import com.example.ui.StatusPill
import com.example.ui.SwFormDropdown
import com.example.ui.SwFormField
import com.example.ui.SwDateField
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
import com.example.ui.vm.SettingsViewModel
import java.time.LocalDate

/**
 * Sites / Projects / Tasks / Departments management (Flask settings CRUD).
 * Delete is guarded by reference counts; archive is always available.
 */
@Composable
fun ManageScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sites", "Projects", "Tasks", "Departments")

    val sites by vm.sites.collectAsStateLifecycle()
    val projects by vm.projects.collectAsStateLifecycle()
    val tasks by vm.tasks.collectAsStateLifecycle()
    val departments by vm.departments.collectAsStateLifecycle()
    val message by vm.message.collectAsStateLifecycle()

    var editSite by remember { mutableStateOf<Site?>(null) }
    var editProject by remember { mutableStateOf<Project?>(null) }
    var editTask by remember { mutableStateOf<WorkTask?>(null) }
    var editDept by remember { mutableStateOf<Department?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Manage",
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
                containerColor = PrimaryBlue,
                contentColor = White,
                shape = CircleShape,
            ) { Icon(Icons.Filled.Add, "Add ${tabs[tab].dropLast(1)}") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = CardBackground,
                contentColor = PrimaryBlue,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[tab]), color = PrimaryBlue,
                    )
                },
            ) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                t, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (tab == i) PrimaryBlue else TextSecondary,
                            )
                        },
                    )
                }
            }

            when (tab) {
                0 -> EntityList(
                    items = sites,
                    empty = "No sites yet. Tap + to add your first site.",
                    key = { it.id },
                    title = { it.name },
                    subtitle = { listOf(it.address, it.contactPerson).filter(String::isNotBlank).joinToString(" • ") },
                    status = { it.status },
                    onEdit = { editSite = it },
                    onArchive = { vm.archiveSite(it.id) },
                    onDelete = { vm.deleteSite(it.id) },
                )
                1 -> EntityList(
                    items = projects,
                    empty = "No projects yet. Tap + to add one.",
                    key = { it.id },
                    title = { it.name },
                    subtitle = {
                        buildString {
                            it.deadline?.let { d -> append("Deadline $d") }
                            if (it.penaltyType != "none" && it.penaltyValue > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("Penalty ${if (it.penaltyType == "fixed") "₹" else ""}${it.penaltyValue}${if (it.penaltyType == "percent") "%" else ""}/day")
                            }
                        }
                    },
                    status = { it.status },
                    onEdit = { editProject = it },
                    onArchive = { vm.archiveProject(it.id) },
                    onDelete = { vm.deleteProject(it.id) },
                )
                2 -> EntityList(
                    items = tasks,
                    empty = "No work tasks yet. Tap + to add one.",
                    key = { it.id },
                    title = { it.name },
                    subtitle = { it.category },
                    status = { it.status },
                    onEdit = { editTask = it },
                    onArchive = { vm.saveTask(it.copy(status = if (it.status == "archived") "active" else "archived")) },
                    onDelete = { vm.deleteTask(it) },
                )
                else -> EntityList(
                    items = departments,
                    empty = "No departments yet. Tap + to add one.",
                    key = { it.id },
                    title = { it.name },
                    subtitle = { "" },
                    status = { it.status },
                    onEdit = { editDept = it },
                    onArchive = { vm.saveDepartment(it.copy(status = if (it.status == "archived") "active" else "archived")) },
                    onDelete = { vm.deleteDepartment(it) },
                )
            }
        }
    }

    // ── Add / edit dialogs per tab ──────────────────────────────────────────
    if ((showAdd && tab == 0) || editSite != null) {
        SiteDialog(
            existing = editSite,
            onDismiss = { showAdd = false; editSite = null },
            onSave = { vm.saveSite(it); showAdd = false; editSite = null },
        )
    }
    if ((showAdd && tab == 1) || editProject != null) {
        ProjectDialog(
            existing = editProject,
            sites = sites.filter { it.status == "active" },
            onDismiss = { showAdd = false; editProject = null },
            onSave = { vm.saveProject(it); showAdd = false; editProject = null },
        )
    }
    if ((showAdd && tab == 2) || editTask != null) {
        TaskDialog(
            existing = editTask,
            projects = projects.filter { it.status != "archived" },
            onDismiss = { showAdd = false; editTask = null },
            onSave = { vm.saveTask(it); showAdd = false; editTask = null },
        )
    }
    if ((showAdd && tab == 3) || editDept != null) {
        DepartmentDialog(
            existing = editDept,
            onDismiss = { showAdd = false; editDept = null },
            onSave = { vm.saveDepartment(it); showAdd = false; editDept = null },
        )
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::clearMessage,
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } },
            text = { Text(msg) },
        )
    }
}

// ── Generic entity list ──────────────────────────────────────────────────────

@Composable
private fun <T> EntityList(
    items: List<T>,
    empty: String,
    key: (T) -> Any,
    title: (T) -> String,
    subtitle: (T) -> String,
    status: (T) -> String,
    onEdit: (T) -> Unit,
    onArchive: (T) -> Unit,
    onDelete: (T) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf<T?>(null) }

    if (items.isEmpty()) {
        EmptyState(Icons.Filled.Business, "Nothing here yet", empty)
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = key) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title(item), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                            Spacer(Modifier.size(8.dp))
                            val s = status(item)
                            StatusPill(
                                s.replaceFirstChar { it.uppercase() },
                                when (s) { "active" -> Success; "completed" -> PrimaryBlue; else -> TextSecondary },
                            )
                        }
                        val sub = subtitle(item)
                        if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = TextSecondary)
                    }
                    IconButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Filled.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onArchive(item) }) {
                        Icon(Icons.Filled.Archive, "Archive / restore", tint = Warning, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { confirmDelete = item }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) } // clear the FAB
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${title(item)}?") },
            text = { Text("This is permanent. Items referenced by other records cannot be deleted and should be archived instead.") },
            confirmButton = {
                TextButton(onClick = { onDelete(item); confirmDelete = null }) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun SiteDialog(existing: Site?, onDismiss: () -> Unit, onSave: (Site) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var person by remember { mutableStateOf(existing?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(existing?.contactPhone ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Site" else "Edit Site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwFormField("Site name *", name) { name = it }
                SwFormField("Address", address) { address = it }
                SwFormField("Contact person", person) { person = it }
                SwFormField("Contact phone", phone, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone) { phone = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((existing ?: Site(name = "")).copy(name = name.trim(), address = address, contactPerson = person, contactPhone = phone))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProjectDialog(
    existing: Project?,
    sites: List<Site>,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var siteId by remember { mutableStateOf(existing?.siteId) }
    var deadline by remember { mutableStateOf(existing?.deadline) }
    var completion by remember { mutableStateOf(existing?.completionDate) }
    var penaltyType by remember { mutableStateOf(existing?.penaltyType ?: "none") }
    var penaltyValue by remember { mutableStateOf(existing?.penaltyValue?.takeIf { it > 0 }?.toString() ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Project" else "Edit Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwFormField("Project name *", name) { name = it }
                SwFormField("Description", description) { description = it }
                val siteNames = listOf("None") + sites.map { it.name }
                SwFormDropdown(
                    "Site",
                    sites.firstOrNull { it.id == siteId }?.name ?: "None",
                    siteNames,
                ) { chosen -> siteId = sites.firstOrNull { it.name == chosen }?.id }
                SwDateField("Deadline", deadline) { deadline = it }
                SwDateField("Completion date", completion) { completion = it }
                SwFormDropdown("Delay penalty", penaltyType, listOf("none", "fixed", "percent"), display = {
                    when (it) { "fixed" -> "Fixed ₹ / day"; "percent" -> "% of worker project rate / day"; else -> "None" }
                }) { penaltyType = it }
                if (penaltyType != "none") {
                    SwFormField(
                        if (penaltyType == "fixed") "Penalty ₹ per delayed day" else "Penalty % per delayed day",
                        penaltyValue,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ) { penaltyValue = it }
                }
                SwFormDropdown("Status", status, listOf("active", "completed", "archived"), display = { it.replaceFirstChar { c -> c.uppercase() } }) { status = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    (existing ?: Project(name = "")).copy(
                        name = name.trim(), description = description, siteId = siteId,
                        deadline = deadline, completionDate = completion,
                        penaltyType = penaltyType,
                        penaltyValue = penaltyValue.toDoubleOrNull() ?: 0.0,
                        status = status,
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TaskDialog(
    existing: WorkTask?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (WorkTask) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var projectId by remember { mutableStateOf(existing?.projectId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Task" else "Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwFormField("Task name *", name) { name = it }
                SwFormField("Category", category) { category = it }
                val names = listOf("None") + projects.map { it.name }
                SwFormDropdown(
                    "Project",
                    projects.firstOrNull { it.id == projectId }?.name ?: "None",
                    names,
                ) { chosen -> projectId = projects.firstOrNull { it.name == chosen }?.id }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((existing ?: WorkTask(name = "")).copy(name = name.trim(), category = category, projectId = projectId))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DepartmentDialog(existing: Department?, onDismiss: () -> Unit, onSave: (Department) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Department" else "Edit Department") },
        text = {
            Column {
                SwFormField("Department name *", name) { name = it }
                Spacer(Modifier.height(6.dp))
                Text(
                    "The first two letters become the worker ID prefix (e.g. Masonry → MA001).",
                    fontSize = 12.sp, color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave((existing ?: Department(name = "")).copy(name = name.trim())) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CardBorder
import com.example.ui.SwDateField
import com.example.ui.SwFormDropdown
import com.example.ui.SwFormField
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Danger
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.AssignmentsViewModel
import com.example.util.LocalImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Assignments — mirrors the web app's `assignments.html`: assign and transfer
 * workers to sites, projects and tasks, with site/project/status filters and
 * an assigned/unassigned count.
 */
@Composable
fun AssignmentsScreen(vm: AssignmentsViewModel, onBack: () -> Unit) {
    val rows by vm.visibleRows.collectAsStateLifecycle()
    val allRows by vm.rows.collectAsStateLifecycle()
    val assigned by vm.assignedCount.collectAsStateLifecycle()
    val filter by vm.filter.collectAsStateLifecycle()
    val siteFilter by vm.siteFilter.collectAsStateLifecycle()
    val projectFilter by vm.projectFilter.collectAsStateLifecycle()
    val sites by vm.sites.collectAsStateLifecycle()
    val projects by vm.projects.collectAsStateLifecycle()
    val tasks by vm.tasks.collectAsStateLifecycle()

    var assignFor by remember { mutableStateOf<AssignmentsViewModel.Row?>(null) }
    var endFor by remember { mutableStateOf<AssignmentsViewModel.Row?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Assignments",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Assign & transfer workers to sites, projects and tasks.",
                    fontSize = 13.sp, color = TextSecondary,
                )
            }

            // Assigned / unassigned counts
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CountTile("Assigned", assigned, Success, Modifier.weight(1f))
                    CountTile("Unassigned", allRows.size - assigned, TextSecondary, Modifier.weight(1f))
                }
            }

            // Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwFormDropdown(
                        "Site",
                        sites.firstOrNull { it.id == siteFilter }?.name ?: "All sites",
                        listOf("All sites") + sites.map { it.name },
                    ) { chosen -> vm.setSite(sites.firstOrNull { it.name == chosen }?.id) }
                    SwFormDropdown(
                        "Project",
                        projects.firstOrNull { it.id == projectFilter }?.name ?: "All projects",
                        listOf("All projects") + projects.map { it.name },
                    ) { chosen -> vm.setProject(projects.firstOrNull { it.name == chosen }?.id) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Assigned", "Unassigned").forEachIndexed { i, label ->
                            val selected = filter == i
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) PrimaryBlue else CardBackground)
                                    .border(1.dp, if (selected) PrimaryBlue else DividerColor, RoundedCornerShape(10.dp))
                                    .clickable { vm.setFilter(i) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    color = if (selected) White else TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            if (rows.isEmpty()) {
                item {
                    Text(
                        "No workers match these filters.",
                        fontSize = 13.sp, color = TextSecondary,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(rows, key = { it.worker.id }) { row ->
                    AssignmentRow(
                        row = row,
                        dateFmt = dateFmt,
                        onAssign = { assignFor = row },
                        onEnd = { endFor = row },
                    )
                }
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }

    // ── Assign / transfer dialog ──
    assignFor?.let { row ->
        AssignDialogFull(
            workerName = row.worker.fullName,
            transferring = row.assignment != null,
            sites = sites.map { it.id to it.name },
            projects = projects.map { it.id to it.name },
            tasks = tasks.map { it.id to it.name },
            onDismiss = { assignFor = null },
            onSave = { projectId, siteId, taskId, start, notes ->
                vm.assign(row.worker.id, projectId, siteId, taskId, start, notes) { toast = it }
                assignFor = null
            },
        )
    }

    endFor?.let { row ->
        AlertDialog(
            onDismissRequest = { endFor = null },
            title = { Text("End assignment?") },
            text = { Text("${row.worker.fullName}'s current assignment will be closed today. History is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    row.assignment?.let { a -> vm.endAssignment(a.id) { toast = it } }
                    endFor = null
                }) { Text("End", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { endFor = null }) { Text("Cancel") } },
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

@Composable
private fun CountTile(label: String, value: Int, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = tint)
            Text(label, fontSize = 11.5.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun AssignmentRow(
    row: AssignmentsViewModel.Row,
    dateFmt: DateTimeFormatter,
    onAssign: () -> Unit,
    onEnd: () -> Unit,
) {
    val a = row.assignment
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryBlue, DarkBlue))),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalImage(row.worker.profileImage, null, Modifier.size(42.dp).clip(CircleShape)) {
                        Text(
                            row.worker.fullName.take(1).uppercase(Locale.US),
                            color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        row.worker.fullName, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Navy,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${row.worker.workerCode} • ${row.worker.position}",
                        fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.size(10.dp))
            if (a == null) {
                Text("Not assigned", fontSize = 12.5.sp, color = TextSecondary)
            } else {
                Text(
                    listOfNotNull(row.siteName, row.projectName, row.taskName)
                        .joinToString(" • ").ifBlank { "—" },
                    fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Navy,
                )
                Text("Since ${a.startDate.format(dateFmt)}", fontSize = 11.5.sp, color = TextSecondary)
            }

            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue)
                        .clickable(onClick = onAssign)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (a != null) "Transfer" else "Assign",
                        color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                if (a != null) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Danger.copy(alpha = 0.1f))
                            .clickable(onClick = onEnd)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("End", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun AssignDialogFull(
    workerName: String,
    transferring: Boolean,
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
        title = { Text(if (transferring) "Transfer $workerName" else "Assign $workerName") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (transferring) {
                    Text(
                        "Any active assignment is closed and replaced.",
                        fontSize = 12.sp, color = TextSecondary,
                    )
                }
                SwFormDropdown(
                    "Site", sites.firstOrNull { it.first == siteId }?.second ?: "— No site —",
                    listOf("— No site —") + sites.map { it.second },
                ) { chosen -> siteId = sites.firstOrNull { it.second == chosen }?.first }
                SwFormDropdown(
                    "Project", projects.firstOrNull { it.first == projectId }?.second ?: "— No project —",
                    listOf("— No project —") + projects.map { it.second },
                ) { chosen -> projectId = projects.firstOrNull { it.second == chosen }?.first }
                SwFormDropdown(
                    "Task (optional)", tasks.firstOrNull { it.first == taskId }?.second ?: "— No task —",
                    listOf("— No task —") + tasks.map { it.second },
                ) { chosen -> taskId = tasks.firstOrNull { it.second == chosen }?.first }
                SwDateField("Start date", start) { start = it }
                SwFormField("Notes (optional)", notes) { notes = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { start?.let { onSave(projectId, siteId, taskId, it, notes) } }) {
                Text(if (transferring) "Transfer" else "Assign")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

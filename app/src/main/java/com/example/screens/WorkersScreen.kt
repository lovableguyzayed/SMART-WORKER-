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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PayTypes
import com.example.data.model.Worker
import com.example.ui.CardBorder
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.WorkersViewModel
import com.example.util.LocalImage
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Workers directory — mirrors the web app's `workers.html`: gradient header
 * with a translucent search/filter card, then one rounded card per worker
 * showing photo + status badge, designation, pay rate, phone and join date.
 */
@Composable
fun SmartWorkerWorkersScreen(
    vm: WorkersViewModel,
    onOpenWorker: (Long) -> Unit,
    isAdmin: Boolean = false,
    onAddWorker: () -> Unit = {},
) {
    val workers by vm.workers.collectAsStateLifecycle()
    val search by vm.search.collectAsStateLifecycle()
    val tab by vm.tab.collectAsStateLifecycle()
    val counts by vm.counts.collectAsStateLifecycle()

    Scaffold(containerColor = BackgroundColor) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                WorkersHeader(
                    search = search,
                    onSearch = vm::setSearch,
                    tab = tab,
                    counts = counts,
                    onTab = vm::setTab,
                    isAdmin = isAdmin,
                    onAddWorker = onAddWorker,
                )
            }

            if (workers.isEmpty()) {
                item {
                    Text(
                        if (search.isBlank()) "No workers yet. Tap “Add Worker” to create the first record."
                        else "No workers match “$search”.",
                        fontSize = 13.sp, color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }
            } else {
                items(workers, key = { it.id }) { worker ->
                    WorkerCard(worker, Modifier.padding(horizontal = 16.dp)) { onOpenWorker(worker.id) }
                }
            }
        }
    }
}

@Composable
private fun WorkersHeader(
    search: String,
    onSearch: (String) -> Unit,
    tab: Int,
    counts: WorkersViewModel.Counts,
    onTab: (Int) -> Unit,
    isAdmin: Boolean,
    onAddWorker: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1E3A8A), DarkBlue)))
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Workers", color = White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("Manage Employee Records", color = White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
                if (isAdmin) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(White.copy(alpha = 0.2f))
                            .border(1.dp, White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddWorker)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Worker", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Translucent search + filter card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(White.copy(alpha = 0.15f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(White.copy(alpha = 0.2f))
                        .border(1.dp, White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text("Search by name, ID, or phone…", color = White.copy(alpha = 0.7f), fontSize = 13.5.sp)
                        }
                        BasicTextField(
                            value = search,
                            onValueChange = onSearch,
                            singleLine = true,
                            textStyle = TextStyle(color = White, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(White),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                // Status filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "All (${counts.all})" to 0,
                        "Active (${counts.active})" to 1,
                        "Inactive (${counts.inactive})" to 2,
                    ).forEach { (label, index) ->
                        val selected = tab == index
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (selected) White else White.copy(alpha = 0.16f))
                                .border(1.dp, White.copy(alpha = if (selected) 0f else 0.3f), RoundedCornerShape(11.dp))
                                .clickable { onTab(index) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (selected) PrimaryBlue else White,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerCard(worker: Worker, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val active = worker.status == "active"
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with status badge
                Box(Modifier.size(64.dp)) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(PrimaryBlue, DarkBlue))),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(worker.profileImage, null, Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))) {
                            Text(
                                worker.fullName.take(1).uppercase(Locale.US),
                                color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp)
                            .size(23.dp).clip(CircleShape).background(White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(19.dp).clip(CircleShape)
                                .background(if (active) Success else TextSecondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (active) Icons.Filled.Check else Icons.Filled.Pause,
                                if (active) "Active" else "Inactive",
                                tint = White, modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            worker.fullName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(worker.employeeType, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${worker.position} • ${worker.department}",
                        fontSize = 12.5.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("ID: ${worker.workerCode}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text(payRateLabel(worker), fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Success)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.HorizontalDivider(color = SubtleDivider)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(worker.phone.ifBlank { "—" }, fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Filled.CalendarMonth, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    worker.joinDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    fontSize = 12.sp, color = TextSecondary,
                )
            }
        }
    }
}

private fun payRateLabel(w: Worker): String = when (w.payType) {
    PayTypes.DAILY -> "₹${(w.dailyRate ?: 0.0).toInt()}/day"
    PayTypes.MONTHLY -> "₹${(w.monthlySalary ?: 0.0).toInt()}/month"
    PayTypes.HOURLY -> "₹${(w.hourlyRate ?: 0.0).toInt()}/hour"
    else -> w.projectRate?.let { "₹${it.toInt()}/project" } ?: "Project Based"
}

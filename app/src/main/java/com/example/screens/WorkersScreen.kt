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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PayTypes
import com.example.data.model.Worker
import com.example.ui.EmptyState
import com.example.ui.StatTile
import com.example.ui.SwCard
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.Mint
import com.example.ui.theme.OnBright
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Purple
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.WorkersViewModel
import com.example.util.LocalImage
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Workers — roster headline, search, status filters, then one card per worker.
 * Each card carries a colour drawn from the worker's own code, so the list
 * reads as a colour-blocked board rather than a grey column.
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

    WorkersContent(
        workers = workers,
        search = search,
        tab = tab,
        counts = counts,
        isAdmin = isAdmin,
        onSearch = vm::setSearch,
        onTab = vm::setTab,
        onOpenWorker = onOpenWorker,
        onAddWorker = onAddWorker,
    )
}

/** Presentation half — state in, callbacks out, so it renders from fixtures. */
@Composable
internal fun WorkersContent(
    workers: List<Worker>,
    search: String,
    tab: Int,
    counts: WorkersViewModel.Counts,
    isAdmin: Boolean,
    onSearch: (String) -> Unit,
    onTab: (Int) -> Unit,
    onOpenWorker: (Long) -> Unit,
    onAddWorker: () -> Unit,
) {
    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Roster", style = SwType.Caption, color = OnDarkMuted)
                        Text("Workers", style = SwType.Headline.copy(fontSize = 26.sp), color = OnDark)
                    }
                    if (isAdmin) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(Flame)
                                .clickable(onClick = onAddWorker)
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, tint = White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }
            }

            // ── Roster split ──
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatTile(
                        counts.active.toString(), "Active",
                        Modifier.weight(1f).height(96.dp), container = Lime,
                    )
                    StatTile(
                        counts.inactive.toString(), "Inactive",
                        Modifier.weight(1f).height(96.dp), container = Surface1,
                    )
                    StatTile(
                        counts.all.toString(), "Total",
                        Modifier.weight(1f).height(96.dp), container = Volt,
                    )
                }
            }

            // ── Search ──
            item {
                SearchField(
                    value = search,
                    onValueChange = onSearch,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // ── Filters ──
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill("All", counts.all, tab == 0) { onTab(0) }
                    FilterPill("Active", counts.active, tab == 1) { onTab(1) }
                    FilterPill("Inactive", counts.inactive, tab == 2) { onTab(2) }
                }
            }

            if (workers.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Groups,
                        if (search.isBlank()) "No workers yet" else "No match",
                        if (search.isBlank()) {
                            "Tap Add to create the first record."
                        } else {
                            "Nothing matches “$search”."
                        },
                    )
                }
            } else {
                items(workers, key = { it.id }) { worker ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        WorkerCard(worker) { onOpenWorker(worker.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(Surface3)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = OnDarkMuted, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Search name, ID or phone", style = SwType.Body, color = OnDarkFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SwType.Body.copy(color = OnDark),
                cursorBrush = SolidColor(Flame),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close, "Clear search", tint = OnDarkMuted,
                modifier = Modifier.size(18.dp).clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) OnDark else Surface3)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnBright else OnDarkMuted,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            count.toString(),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) OnBright.copy(alpha = 0.55f) else OnDarkFaint,
        )
    }
}

@Composable
private fun WorkerCard(worker: Worker, onClick: () -> Unit) {
    val accent = accentFor(worker.workerCode.ifBlank { worker.fullName })
    val active = worker.status == "active"
    SwCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(
                            worker.profileImage, null,
                            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)),
                        ) {
                            Text(
                                worker.fullName.take(1).uppercase(Locale.US),
                                color = OnBright, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (!active) {
                        Box(
                            Modifier.align(Alignment.BottomEnd)
                                .size(17.dp).clip(CircleShape).background(Surface1),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(OnDarkFaint))
                        }
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        worker.fullName,
                        style = SwType.Title.copy(fontSize = 16.sp),
                        color = if (active) OnDark else OnDarkMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${worker.position}  ·  ${worker.department}",
                        style = SwType.Caption, color = OnDarkMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(CircleShape).background(Surface3)
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        ) {
                            Text(
                                worker.workerCode, fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold, color = OnDarkMuted,
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            worker.employeeType, style = SwType.Caption.copy(fontSize = 11.sp),
                            color = OnDarkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    payRateLabel(worker),
                    style = SwType.Title.copy(fontSize = 14.sp),
                    color = if (active) Lime else OnDarkFaint,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = OnDarkFaint, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(worker.phone.ifBlank { "—" }, style = SwType.Caption, color = OnDarkMuted)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Filled.CalendarMonth, null, tint = OnDarkFaint, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    worker.joinDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = SwType.Caption, color = OnDarkMuted,
                )
            }
        }
    }
}

/** Stable per-worker accent so a given card keeps its colour across sessions. */
private val AvatarPalette = listOf(Flame, Volt, Lime, Sun, Coral, Mint, Purple)

internal fun accentFor(key: String): Color {
    if (key.isEmpty()) return Flame
    val hash = key.fold(7) { acc, c -> acc * 31 + c.code }
    return AvatarPalette[((hash % AvatarPalette.size) + AvatarPalette.size) % AvatarPalette.size]
}

private fun payRateLabel(w: Worker): String = when (w.payType) {
    PayTypes.DAILY -> "₹${(w.dailyRate ?: 0.0).toInt()}/day"
    PayTypes.MONTHLY -> "₹${(w.monthlySalary ?: 0.0).toInt()}/mo"
    PayTypes.HOURLY -> "₹${(w.hourlyRate ?: 0.0).toInt()}/hr"
    else -> w.projectRate?.let { "₹${it.toInt()}/project" } ?: "Project"
}

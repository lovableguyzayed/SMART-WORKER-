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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.ui.EmptyState
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.vm.WorkersViewModel

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

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SwTopBar(title = "Workers") },
        floatingActionButton = {
            if (isAdmin) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = onAddWorker,
                    containerColor = PrimaryBlue,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    Icon(Icons.Filled.Add, "Add worker")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = search,
                onValueChange = vm::setSearch,
                placeholder = { Text("Search by name, ID or mobile", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue, unfocusedBorderColor = DividerColor,
                ),
            )
            // Tabs
            val tabs = listOf("All (${counts.all})", "Active (${counts.active})", "Inactive (${counts.inactive})")
            Surface(color = CardBackground) {
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, label ->
                            val selected = index == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { vm.setTab(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    color = if (selected) PrimaryBlue else TextSecondary, textAlign = TextAlign.Center,
                                )
                                if (selected) {
                                    Box(
                                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)
                                            .background(PrimaryBlue, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }

            if (workers.isEmpty()) {
                EmptyState(Icons.Filled.PersonSearch, "No workers found", "Try a different search or tab.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(workers, key = { it.id }) { worker ->
                        WorkerRow(worker) { onOpenWorker(worker.id) }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerRow(worker: Worker, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
            com.example.util.LocalImage(
                path = worker.profileImage,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            ) {
                Text(worker.fullName.take(1), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(worker.fullName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text("ID: ${worker.workerCode}  •  ${worker.position}", fontSize = 12.sp, color = TextSecondary)
        }
        StatusPill(
            if (worker.status == "active") "Active" else "Inactive",
            if (worker.status == "active") Success else TextSecondary,
        )
        Spacer(Modifier.size(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

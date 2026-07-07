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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TxnTypes
import com.example.data.model.WorkerTransaction
import com.example.ui.CardBorder
import com.example.ui.EmptyState
import com.example.ui.StatusPill
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.TransactionsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: TransactionsViewModel, onBack: () -> Unit) {
    val txns by vm.currentTransactions.collectAsStateLifecycle()
    val period by vm.period.collectAsStateLifecycle()
    val workers by vm.workers.collectAsStateLifecycle()
    val totals by vm.totals.collectAsStateLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM")

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            com.example.ui.SwTopBar(
                title = "Transactions",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = PrimaryBlue, contentColor = White) {
                Icon(Icons.Filled.Add, "Add transaction")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clickable { vm.shiftMonth(-1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronLeft, "Previous", tint = Navy)
                }
                Text(period.format(fmt), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(Modifier.size(32.dp).clickable { vm.shiftMonth(1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronRight, "Next", tint = Navy)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Earnings: ₹${"%,.0f".format(totals.first)}", fontSize = 13.sp, color = Success, fontWeight = FontWeight.SemiBold)
                Text("Deductions: ₹${"%,.0f".format(totals.second)}", fontSize = 13.sp, color = Danger, fontWeight = FontWeight.SemiBold)
            }

            if (txns.isEmpty()) {
                EmptyState(Icons.Filled.AccountBalanceWallet, "No transactions", "Add an advance, bonus or deduction with the + button.")
            } else {
                val workerName = workers.associate { it.id to it.fullName }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(txns, key = { it.id }) { txn ->
                        TxnRow(txn, workerName[txn.workerId] ?: "Worker #${txn.workerId}", dateFmt, onToggle = { vm.toggle(txn.id) })
                    }
                    item { Spacer(Modifier.height(64.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddTransactionDialog(
            workers = workers.map { it.id to it.fullName },
            onDismiss = { showAdd = false },
            onSave = { workerId, type, amount ->
                vm.save(
                    WorkerTransaction(workerId = workerId, txnType = type, amount = amount, date = LocalDate.now()),
                ) {}
                showAdd = false
            },
        )
    }
}

@Composable
private fun TxnRow(txn: WorkerTransaction, name: String, dateFmt: DateTimeFormatter, onToggle: () -> Unit) {
    val earning = txn.isEarning
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(
                    "${TxnTypes.LABELS[txn.txnType] ?: txn.txnType} • ${txn.date.format(dateFmt)}",
                    fontSize = 12.sp, color = TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (earning) "+" else "-"}₹${"%,.0f".format(txn.amount)}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (earning) Success else Danger,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.clickable(onClick = onToggle)) {
                    StatusPill(if (txn.status == "active") "Active" else "Cancelled", if (txn.status == "active") Success else TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    workers: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long, String, Double) -> Unit,
) {
    var selectedWorker by remember { mutableStateOf(workers.firstOrNull()?.first) }
    var selectedType by remember { mutableStateOf("advance") }
    var amount by remember { mutableStateOf("") }
    val types = TxnTypes.EARNINGS + TxnTypes.DEDUCTIONS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Transaction", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Worker", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                ChipsFlow(workers.map { it.second }, workers.map { it.first }, selectedWorker) { selectedWorker = it }
                Spacer(Modifier.height(12.dp))
                Text("Type", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                ChipsFlow(types.map { TxnTypes.LABELS[it] ?: it }, types, selectedType) { selectedType = it }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedWorker != null && (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = { onSave(selectedWorker!!, selectedType, amount.toDouble()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipsFlow(labels: List<String>, values: List<T>, selected: T?, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val value = values[i]
            val isSel = value == selected
            Box(
                Modifier
                    .padding(vertical = 2.dp)
                    .background(if (isSel) PrimaryBlue else CardBackground, RoundedCornerShape(999.dp))
                    .border(1.dp, if (isSel) PrimaryBlue else DividerColor, RoundedCornerShape(999.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(label, fontSize = 12.sp, color = if (isSel) White else Navy, fontWeight = FontWeight.Medium)
            }
        }
    }
}

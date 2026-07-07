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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.SwTopBar
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.util.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Reports & Export hub (Flask /export_data + /payroll/export.csv + worker
 * report entry point). Every export lands in the system share sheet.
 */
@Composable
fun ReportsScreen(onBack: () -> Unit, onOpenWorkerReport: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pickWorker by remember { mutableStateOf(false) }

    val workers by container.workerRepository.activeWorkers
        .collectAsStateWithLifecycle(initialValue = emptyList())

    fun export(block: suspend () -> Pair<String, String>) {
        scope.launch {
            busy = true
            val (name, csv) = block()
            CsvExporter.share(context, name, csv)
            busy = false
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Reports & Export",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        if (busy) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Generate CSV files and share them by email, WhatsApp, Drive or any installed app.",
                    fontSize = 13.sp, color = TextSecondary,
                )
            }
            item {
                ReportRow("Worker report (6 months)", "Attendance, pay & leave ledger per worker", Icons.Filled.Badge, Purple) {
                    pickWorker = true
                }
            }
            item {
                ReportRow("Payroll — this month", "Full pay breakdown for every worker", Icons.Filled.Payments, Success) {
                    export {
                        val ym = YearMonth.now()
                        val (rows, _) = container.payrollRepository.buildRows(ym.monthValue, ym.year)
                        "payroll_$ym.csv" to CsvExporter.payrollCsv(rows, ym)
                    }
                }
            }
            item {
                ReportRow("Workers master list", "All workers with rates and policies", Icons.Filled.Groups, PrimaryBlue) {
                    export {
                        val all = container.workerRepository.allWorkers.first()
                        "workers.csv" to CsvExporter.workersCsv(all)
                    }
                }
            }
            item {
                ReportRow("Attendance — last 30 days", "Every check-in/out with late & overtime minutes", Icons.Filled.CalendarMonth, Warning) {
                    export {
                        val end = LocalDate.now()
                        val start = end.minusDays(30)
                        val records = container.db.attendanceDao().betweenDates(start, end)
                        val byId = container.workerRepository.allWorkers.first().associateBy { it.id }
                        "attendance_${start}_$end.csv" to CsvExporter.attendanceCsv(records, byId)
                    }
                }
            }
            item {
                ReportRow("Transactions — this month", "Advances, loans, bonuses and recoveries", Icons.Filled.Receipt, Navy) {
                    export {
                        val ym = YearMonth.now()
                        val txns = container.catalogRepository
                            .transactionsBetween(ym.atDay(1), ym.atEndOfMonth()).first()
                        val byId = container.workerRepository.allWorkers.first().associateBy { it.id }
                        "transactions_$ym.csv" to CsvExporter.transactionsCsv(txns, byId)
                    }
                }
            }
        }
    }

    if (pickWorker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickWorker = false },
            title = { Text("Choose worker") },
            text = {
                LazyColumn(Modifier.height(360.dp)) {
                    items(workers, key = { it.id }) { w ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickWorker = false
                                    onOpenWorkerReport(w.id)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(w.fullName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy)
                                Text("${w.workerCode} • ${w.position}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickWorker = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ReportRow(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Filled.Description, "Export", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

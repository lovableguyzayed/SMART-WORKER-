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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.PayTypes
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.domain.PayrollCalculator
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Success
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.flowOf
import java.time.format.DateTimeFormatter

@Composable
fun SmartWorkerWorkerDetailsScreen(workerId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val worker by remember(workerId) { container.workerRepository.worker(workerId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val recentAttendance by remember(workerId) {
        if (workerId > 0) container.attendanceRepository.recentForWorker(workerId, 10) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentTxns by remember(workerId) {
        if (workerId > 0) container.catalogRepository.recentTransactionsForWorker(workerId, 10) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

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
                Text("Loading…", color = TextSecondary)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ProfileHeader(w) }
            item { InfoCard("Job Information", jobRows(w)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = CardBorder,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Recent Attendance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                        Spacer(Modifier.height(8.dp))
                        if (recentAttendance.isEmpty()) {
                            Text("No attendance records yet.", fontSize = 13.sp, color = TextSecondary)
                        } else {
                            recentAttendance.forEach { r -> AttendanceMiniRow(r, dateFmt) }
                        }
                    }
                }
            }
            if (recentTxns.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Recent Transactions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                            Spacer(Modifier.height(8.dp))
                            recentTxns.forEach { t -> TxnMiniRow(t, dateFmt) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
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
            Box(Modifier.size(64.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(38.dp))
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
            "late" -> "Late" to com.example.ui.theme.Warning
            "absent" -> "Absent" to com.example.ui.theme.Danger
            else -> "Leave" to com.example.ui.theme.Purple
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
            color = if (t.isEarning) Success else com.example.ui.theme.Danger,
        )
    }
}

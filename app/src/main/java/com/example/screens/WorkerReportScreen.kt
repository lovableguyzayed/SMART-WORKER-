package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.ui.CardBorder
import com.example.ui.SwTopBar
import com.example.ui.LocalAppContainer
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.Success
import com.example.ui.theme.Warning
import com.example.ui.theme.TextSecondary
import com.example.ui.vm.WorkerAdminViewModel
import com.example.util.CsvExporter

/** 6-month worker performance & pay report (Flask worker_report port). */
@Composable
fun WorkerReportScreen(
    vm: WorkerAdminViewModel,
    workerId: Long,
    onBack: () -> Unit,
) {
    val report by vm.report.collectAsStateLifecycle()
    val context = LocalContext.current

    LaunchedEffect(workerId) {
        vm.setWorker(workerId)
        vm.buildReport()
    }

    // This month's records drive the attendance calendar.
    val container = LocalAppContainer.current
    val calendarRecords by produceState<List<AttendanceRecord>>(emptyList(), workerId) {
        val ym = java.time.YearMonth.now()
        value = runCatching {
            container.db.attendanceDao().forWorkerBetween(workerId, ym.atDay(1), ym.atEndOfMonth())
        }.getOrDefault(emptyList())
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Worker Report",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        when {
            report.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            report.months.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data for this worker yet.", color = TextSecondary)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val first = report.months.first().row
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${first.worker.fullName} (${first.worker.workerCode})",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy,
                                )
                                Text(
                                    "${first.worker.position} • ${first.worker.department} • last 6 months",
                                    fontSize = 12.sp, color = TextSecondary,
                                )
                            }
                            Icon(
                                Icons.Filled.IosShare, "Export report CSV", tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp).clickable {
                                    CsvExporter.share(
                                        context,
                                        "worker_report_${first.worker.workerCode}.csv",
                                        buildReportCsv(report.months),
                                    )
                                },
                            )
                        }
                    }
                }

                // Attendance calendar for the current month (worker_attendance.html)
                item {
                    AttendanceCalendarCard(
                        month = java.time.YearMonth.now(),
                        records = calendarRecords,
                    )
                }

                // Leave ledger (salaried workers)
                first.pay.leaveBalance?.let { lb ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(0.dp),
                            border = CardBorder,
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Leave Ledger (current month)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                                Spacer(Modifier.height(8.dp))
                                LedgerRow("Monthly quota", "${lb.monthlyQuota} day(s)")
                                LedgerRow("Accrued since joining", "${lb.accruedTotal} day(s)")
                                LedgerRow("Manual adjustments", "%+.1f day(s)".format(lb.manualAdjustment))
                                LedgerRow("Used before this month", "${lb.usedBefore} day(s)")
                                LedgerRow("Used this month", "${lb.usedThisMonth} day(s)")
                                LedgerRow("Available this month", "%.1f day(s)".format(lb.availableThisMonth))
                                LedgerRow(
                                    "Chargeable (deducted)", "${lb.chargeableDays} day(s)",
                                    if (lb.chargeableDays > 0) Danger else Success,
                                )
                                LedgerRow(
                                    "Balance after this month", "%.1f day(s)".format(lb.balanceAfter),
                                    if (lb.balanceAfter >= 0) Success else Danger,
                                )
                            }
                        }
                    }
                }

                items(report.months.size) { i ->
                    val m = report.months[i]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardBorder,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(m.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                                Text(
                                    "₹%,.0f".format(m.row.pay.estimatedPay),
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Success,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = SubtleDivider)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                MiniStat("Marked", "${m.row.attendance.totalMarkedDays}")
                                MiniStat("Paid", "${m.row.pay.paidDays}")
                                MiniStat("Absent", "${m.row.attendance.absentDays}")
                                MiniStat("Leave", "${m.row.attendance.leaveDays}")
                                MiniStat("OT min", "${m.row.pay.overtimeMinutes}")
                                MiniStat("Deducted", "₹%,.0f".format(m.row.pay.totalDeductions))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun LedgerRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = Navy) {
    Row(
        Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

private fun buildReportCsv(months: List<WorkerAdminViewModel.MonthRow>): String = buildString {
    appendLine("Month,Days Marked,Paid Days,Absent,Leave,Overtime Min,Gross,Deductions,Net")
    months.forEach { m ->
        appendLine(
            listOf(
                m.label, m.row.attendance.totalMarkedDays, m.row.pay.paidDays,
                m.row.attendance.absentDays, m.row.attendance.leaveDays,
                m.row.pay.overtimeMinutes, m.row.pay.grossPay,
                m.row.pay.totalDeductions, m.row.pay.estimatedPay,
            ).joinToString(",")
        )
    }
}

/**
 * Month grid with colour-coded days — the app-side counterpart of the web
 * app's `worker_attendance.html` calendar.
 */
@Composable
private fun AttendanceCalendarCard(month: java.time.YearMonth, records: List<AttendanceRecord>) {
    val byDay = records.associateBy { it.date.dayOfMonth }
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value % 7 // Sunday-first grid
    val days = month.lengthOfMonth()
    val rows = ((lead + days + 6) / 7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Attendance Calendar — ${month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))}",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy,
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Text(
                        d, modifier = Modifier.weight(1f), fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - lead + 1
                        if (day !in 1..days) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val status = byDay[day]?.status
                            val tint = when (status) {
                                AttendanceStatus.PRESENT -> Success
                                AttendanceStatus.LATE -> Warning
                                AttendanceStatus.ABSENT -> Danger
                                AttendanceStatus.LEAVE -> Purple
                                else -> null
                            }
                            Box(
                                Modifier.weight(1f).padding(1.5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(tint?.copy(alpha = 0.14f) ?: BackgroundColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.toString(), fontSize = 11.sp,
                                        fontWeight = if (tint != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = tint ?: TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot("Present", Success)
                LegendDot("Absent", Danger)
                LegendDot("Late", Warning)
                LegendDot("Leave", Purple)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 10.5.sp, color = TextSecondary)
    }
}

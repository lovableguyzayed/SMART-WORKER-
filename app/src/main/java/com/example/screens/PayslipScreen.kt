package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CardBorder
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.Success
import com.example.ui.theme.TextSecondary
import com.example.ui.vm.PayslipViewModel
import com.example.util.PayslipPdfExporter
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PayslipScreen(
    vm: PayslipViewModel,
    workerId: Long,
    period: YearMonth,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateLifecycle()
    val context = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")

    LaunchedEffect(workerId, period) { vm.load(workerId, period) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "Payslip",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        val row = state.row
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            row == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Worker not found.", color = TextSecondary)
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = CardBorder,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(state.company?.name ?: "Smart Worker", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
                        Text("Payslip — ${period.format(fmt)}", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${row.worker.fullName} (${row.worker.workerCode}) • ${row.worker.position}",
                            fontSize = 13.sp, color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                SlipSection(
                    "Attendance",
                    listOf(
                        "Days marked" to "${row.attendance.totalMarkedDays}",
                        "Paid days" to "${row.pay.paidDays}",
                        "Absent / Leave" to "${row.attendance.absentDays} / ${row.attendance.leaveDays}",
                        "Overtime" to "${row.pay.overtimeMinutes} min",
                    ),
                )
                Spacer(Modifier.height(12.dp))
                SlipSection(
                    "Earnings",
                    buildList {
                        add("Base pay" to money(row.pay.basePay))
                        if (row.pay.overtimePay > 0) add("Overtime pay" to money(row.pay.overtimePay))
                        if (row.pay.closureExtraPay > 0) add("Closure day extra" to money(row.pay.closureExtraPay))
                        if (row.pay.transactionEarnings > 0) add("Bonus / incentives" to money(row.pay.transactionEarnings))
                        add("Gross pay" to money(row.pay.grossPay))
                    },
                )
                Spacer(Modifier.height(12.dp))
                SlipSection(
                    "Deductions",
                    buildList {
                        if (row.pay.leaveDeductions > 0) add("Leave deduction" to money(row.pay.leaveDeductions))
                        if (row.pay.lateDeductions > 0) add("Late deduction" to money(row.pay.lateDeductions))
                        if (row.pay.delayPenalty > 0) add("Project delay penalty" to money(row.pay.delayPenalty))
                        if (row.pay.transactionDeductions > 0) add("Advances / recoveries" to money(row.pay.transactionDeductions))
                        if (isEmpty()) add("No deductions" to money(0.0))
                        add("Total deductions" to money(row.pay.totalDeductions))
                    },
                    valueColor = Danger,
                )

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("NET PAY", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Success)
                        Text(money(row.pay.estimatedPay), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Success)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { PayslipPdfExporter.exportAndShare(context, state.company, row, period) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export PDF & Share", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SlipSection(
    title: String,
    rows: List<Pair<String, String>>,
    valueColor: androidx.compose.ui.graphics.Color = Navy,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(4.dp))
            rows.forEachIndexed { i, (label, value) ->
                val last = i == rows.lastIndex
                Row(
                    Modifier.fillMaxWidth().height(38.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, fontSize = 13.sp, color = if (last) Navy else TextSecondary, fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal)
                    Text(value, fontSize = 13.sp, fontWeight = if (last) FontWeight.Bold else FontWeight.Medium, color = if (last) valueColor else Navy)
                }
                if (!last) HorizontalDivider(color = SubtleDivider)
            }
        }
    }
}

private fun money(v: Double): String = "₹%,.2f".format(v)

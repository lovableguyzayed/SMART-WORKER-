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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.PayrollRepository
import com.example.ui.CardBorder
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
import com.example.ui.theme.Warning
import com.example.ui.vm.PayrollViewModel
import java.time.format.DateTimeFormatter

@Composable
fun SmartWorkerPayrollScreen(vm: PayrollViewModel, isAdmin: Boolean) {
    val period by vm.period.collectAsStateLifecycle()
    val rows by vm.rows.collectAsStateLifecycle()
    val totals by vm.totals.collectAsStateLifecycle()
    val loading by vm.loading.collectAsStateLifecycle()
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SwTopBar(title = "Payroll") },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Month selector
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthNav(Icons.Filled.ChevronLeft, "Previous month") { vm.shiftMonth(-1) }
                Text(period.format(fmt), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                MonthNav(Icons.Filled.ChevronRight, "Next month") { vm.shiftMonth(1) }
            }

            // Totals card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TotalItem("Gross", totals.totalGross, Navy)
                        TotalItem("Deductions", totals.totalDeductions, Warning)
                        TotalItem("Net Payable", totals.totalNet, Success)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${totals.workerCount} workers", fontSize = 12.sp, color = TextSecondary)
                        Text("• ${totals.paidCount} paid", fontSize = 12.sp, color = Success)
                        Text("• ${totals.pendingCount} pending", fontSize = 12.sp, color = Warning)
                    }
                    if (isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.generate {} },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        ) { Text("Generate Payroll", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.worker.id }) { row ->
                        PayrollRow(row, isAdmin) { row.recordId?.let(vm::togglePaid) }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TotalItem(label: String, value: Double, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text("₹${"%,.0f".format(value)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PayrollRow(row: PayrollRepository.PayrollRow, isAdmin: Boolean, onTogglePaid: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                Text(row.worker.fullName.take(1), color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.worker.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(
                    "${row.pay.paidDays} paid days • ${row.worker.payType.replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp, color = TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${"%,.0f".format(row.pay.estimatedPay)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                Spacer(Modifier.height(4.dp))
                val label = when (row.status) { "paid" -> "Paid"; "pending" -> "Pending"; else -> "Unsaved" }
                val color = when (row.status) { "paid" -> Success; "pending" -> Warning; else -> TextSecondary }
                if (isAdmin && row.recordId != null) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onTogglePaid)) {
                        StatusPill(label, color)
                    }
                } else {
                    StatusPill(label, color)
                }
            }
        }
    }
}

@Composable
private fun MonthNav(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(CardBackground).border(1.dp, DividerColor, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Navy, modifier = Modifier.size(20.dp)) }
}

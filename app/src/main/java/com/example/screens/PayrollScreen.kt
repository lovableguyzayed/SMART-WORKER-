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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PayTypes
import com.example.data.model.TxnTypes
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.repo.PayrollRepository
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Danger
import com.example.ui.theme.DangerBg
import com.example.ui.theme.DividerColor
import com.example.ui.theme.IconBlueBg
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Purple
import com.example.ui.theme.PurpleBg
import com.example.ui.theme.Success
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Warning
import com.example.ui.theme.WarningBg
import com.example.ui.vm.PayrollViewModel
import com.example.util.CsvExporter
import com.example.util.LocalImage
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun SmartWorkerPayrollScreen(
    vm: PayrollViewModel,
    isAdmin: Boolean,
    onOpenPayslip: (Long, YearMonth) -> Unit = { _, _ -> },
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current

    val period by vm.period.collectAsStateLifecycle()
    val rows by vm.rows.collectAsStateLifecycle()
    val totals by vm.totals.collectAsStateLifecycle()
    val loading by vm.loading.collectAsStateLifecycle()
    val company by container.catalogRepository.company.collectAsStateWithLifecycle(initialValue = null)
    val txns by remember(period) {
        container.catalogRepository.transactionsBetween(period.atDay(1), period.atEndOfMonth())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var tab by remember { mutableStateOf(0) } // 0 Overview, 1 Salary, 2 Advances, 3 Deductions, 4 Payments
    var toast by remember { mutableStateOf<String?>(null) }

    val workersById = remember(rows) { rows.associate { it.worker.id to it.worker } }
    val advances = txns.filter { it.status == "active" && it.txnType in listOf("advance", "cash_advance") }
    val deductions = txns.filter { it.status == "active" && !it.isEarning }

    val paidAmount = rows.filter { it.status == "paid" }.sumOf { it.pay.estimatedPay }
    val pendingAmount = rows.filter { it.status != "paid" }.sumOf { it.pay.estimatedPay }
    val advancesGiven = advances.sumOf { it.amount }

    fun exportCsv() {
        CsvExporter.share(context, "payroll_$period.csv", CsvExporter.payrollCsv(rows, period))
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SwTopBar(title = "Payroll") },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Company + period header (replaces the site selector; payroll is not
            //    site-scoped, so this shows the company and the month being viewed) ──
            Card(
                Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(IconBlueBg), contentAlignment = Alignment.Center) {
                        LocalImage(company?.logo, "Company logo", Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) {
                            Icon(Icons.Filled.CurrencyRupee, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(company?.name ?: "SmartWorker", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Pay period", fontSize = 11.sp, color = TextSecondary)
                    }
                    MonthNav(Icons.Filled.ChevronLeft, "Previous month") { vm.shiftMonth(-1) }
                    Text(
                        period.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    MonthNav(Icons.Filled.ChevronRight, "Next month") { vm.shiftMonth(1) }
                }
            }

            // ── Segmented tabs ──
            val tabs = listOf("Overview", "Salary", "Advances", "Deductions", "Payments")
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(44.dp)) {
                tabs.forEachIndexed { i, label ->
                    val active = i == tab
                    Column(
                        Modifier.weight(1f).fillMaxSize().clickable { tab = i },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            label, fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) PrimaryBlue else TextSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (active) PrimaryBlue else Color.Transparent))
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = DividerColor)

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
                return@Scaffold
            }

            when (tab) {
                0 -> OverviewTab(
                    period = period,
                    totalWorkers = totals.workerCount,
                    totalPayroll = totals.totalGross,
                    paidAmount = paidAmount,
                    pendingAmount = pendingAmount,
                    advancesGiven = advancesGiven,
                    totalDeductions = totals.totalDeductions,
                    rows = rows,
                    isAdmin = isAdmin,
                    onGenerate = { if (isAdmin) vm.generate { toast = it } else { toast = "Only an administrator can generate payroll." } },
                    onExport = { if (rows.isEmpty()) { toast = "Nothing to export yet." } else exportCsv() },
                    onQuick = { tab = it },
                )
                1 -> WorkerPayrollList(rows, isAdmin, period, onOpenPayslip, vm)
                2 -> TxnTab("Advances this month", advances, workersById, "No advances recorded for this month.")
                3 -> TxnTab("Deductions this month", deductions, workersById, "No deductions recorded for this month.")
                4 -> PaymentsTab(rows, period, onOpenPayslip)
            }
        }
    }

    toast?.let {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { toast = null },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { toast = null }) { Text("OK") } },
            text = { Text(it) },
        )
    }
}

// ── Overview ──────────────────────────────────────────────────────────────────
@Composable
private fun OverviewTab(
    period: YearMonth,
    totalWorkers: Int,
    totalPayroll: Double,
    paidAmount: Double,
    pendingAmount: Double,
    advancesGiven: Double,
    totalDeductions: Double,
    rows: List<PayrollRepository.PayrollRow>,
    isAdmin: Boolean,
    onGenerate: () -> Unit,
    onExport: () -> Unit,
    onQuick: (Int) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SectionHeader("Payroll Summary")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Total Workers", totalWorkers.toString(), Navy, IconBlueBg, PrimaryBlue, Icons.Filled.Groups, Modifier.weight(1f))
                    SummaryCard("Total Payroll", money(totalPayroll), Success, SuccessBg, Success, Icons.Filled.CurrencyRupee, Modifier.weight(1f))
                    SummaryCard("Paid Amount", money(paidAmount), Success, SuccessBg, Success, Icons.Filled.PriceCheck, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Pending Amount", money(pendingAmount), Warning, WarningBg, Warning, Icons.Filled.Schedule, Modifier.weight(1f))
                    SummaryCard("Advances Given", money(advancesGiven), Purple, PurpleBg, Purple, Icons.Filled.AccountBalanceWallet, Modifier.weight(1f))
                    SummaryCard("Total Deductions", money(totalDeductions), Danger, DangerBg, Danger, Icons.Filled.RemoveCircleOutline, Modifier.weight(1f))
                }
            }
        }

        item { UpcomingPayrollCard(period, totalWorkers, isAdmin, onGenerate) }

        item {
            SectionHeader("Salary Distribution")
            Spacer(Modifier.height(12.dp))
            val daily = rows.filter { it.worker.payType == PayTypes.DAILY }
            val monthly = rows.filter { it.worker.payType == PayTypes.MONTHLY }
            val contract = rows.filter { it.worker.payType == PayTypes.PROJECT || it.worker.payType == PayTypes.HOURLY }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DistCard("Daily Wage Workers", daily.size, daily.sumOf { it.pay.estimatedPay }, IconBlueBg, PrimaryBlue, Modifier.weight(1f))
                DistCard("Monthly Salary Workers", monthly.size, monthly.sumOf { it.pay.estimatedPay }, PurpleBg, Purple, Modifier.weight(1f))
                DistCard("Contract Workers", contract.size, contract.sumOf { it.pay.estimatedPay }, WarningBg, Warning, Modifier.weight(1f))
            }
        }

        item {
            SectionHeader("Quick Payroll Actions")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Generate\nPayroll", Icons.Filled.CalendarMonth, PrimaryBlue, Modifier.weight(1f), onGenerate)
                QuickAction("Pay\nSalary", Icons.Filled.PriceCheck, Success, Modifier.weight(1f)) { onQuick(1) }
                QuickAction("Advances", Icons.Filled.AccountBalanceWallet, Purple, Modifier.weight(1f)) { onQuick(2) }
                QuickAction("Deductions", Icons.Filled.RemoveCircleOutline, Danger, Modifier.weight(1f)) { onQuick(3) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Export\nReport", Icons.Filled.Upload, Navy, Modifier.weight(1f), onExport)
                QuickAction("Payment\nHistory", Icons.Filled.History, PrimaryBlue, Modifier.weight(1f)) { onQuick(4) }
                Spacer(Modifier.weight(2f))
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun UpcomingPayrollCard(period: YearMonth, workers: Int, isAdmin: Boolean, onGenerate: () -> Unit) {
    val payDate = period.atEndOfMonth()
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), payDate)
    Column {
        SectionHeader("Upcoming Payroll")
        Spacer(Modifier.height(12.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(0.dp),
            border = CardBorder,
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(IconBlueBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Next Salary Date", fontSize = 11.sp, color = TextSecondary)
                    Text(payDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("For $workers Workers", fontSize = 11.sp, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(IconBlueBg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(if (daysLeft >= 0) "$daysLeft days left" else "Due", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                    }
                    if (isAdmin) {
                        Box(
                            Modifier.clip(RoundedCornerShape(9.dp)).background(PrimaryBlue).clickable(onClick = onGenerate).padding(horizontal = 12.dp, vertical = 9.dp),
                        ) { Text("Generate Payroll", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                    }
                }
            }
        }
    }
}

// ── Salary tab (per-worker list) ──
@Composable
private fun WorkerPayrollList(
    rows: List<PayrollRepository.PayrollRow>,
    isAdmin: Boolean,
    period: YearMonth,
    onOpenPayslip: (Long, YearMonth) -> Unit,
    vm: PayrollViewModel,
) {
    if (rows.isEmpty()) {
        EmptyTab("No active workers to show for this month.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.worker.id }) { row ->
            PayrollRow(row, isAdmin, onOpen = { onOpenPayslip(row.worker.id, period) }, onTogglePaid = { row.recordId?.let(vm::togglePaid) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Payments tab (paid rows) ──
@Composable
private fun PaymentsTab(rows: List<PayrollRepository.PayrollRow>, period: YearMonth, onOpenPayslip: (Long, YearMonth) -> Unit) {
    val paid = rows.filter { it.status == "paid" }
    if (paid.isEmpty()) {
        EmptyTab("No payments marked as paid for this month yet.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(paid, key = { it.worker.id }) { row ->
            PayrollRow(row, isAdmin = false, onOpen = { onOpenPayslip(row.worker.id, period) }, onTogglePaid = {})
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Advances / Deductions tab ──
@Composable
private fun TxnTab(title: String, txns: List<WorkerTransaction>, workersById: Map<Long, Worker>, emptyMsg: String) {
    if (txns.isEmpty()) {
        EmptyTab(emptyMsg)
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(title, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(txns, key = { it.id }) { t ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardBorder,
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(workersById[t.workerId]?.fullName ?: "Worker #${t.workerId}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                        Text(
                            "${TxnTypes.LABELS[t.txnType] ?: t.txnType} • ${t.date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))}",
                            fontSize = 12.sp, color = TextSecondary,
                        )
                    }
                    Text(money(t.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (t.isEarning) Success else Danger)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Pieces ────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
}

@Composable
private fun SummaryCard(label: String, value: String, valueColor: Color, iconBg: Color, iconTint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 2, lineHeight = 13.sp)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DistCard(label: String, count: Int, amount: Double, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Groups, null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 2, lineHeight = 13.sp)
            Text(count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text(money(amount), fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Navy, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun EmptyTab(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PayrollRow(
    row: PayrollRepository.PayrollRow,
    isAdmin: Boolean,
    onOpen: () -> Unit,
    onTogglePaid: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(AvatarBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                LocalImage(row.worker.profileImage, null, Modifier.size(42.dp).clip(CircleShape)) {
                    Text(row.worker.fullName.take(1), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
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
                Text(money(row.pay.estimatedPay), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                Spacer(Modifier.height(4.dp))
                val label = when (row.status) { "paid" -> "Paid"; "pending" -> "Pending"; else -> "Unsaved" }
                val color = when (row.status) { "paid" -> Success; "pending" -> Warning; else -> TextSecondary }
                if (isAdmin && row.recordId != null) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onTogglePaid)) { StatusPill(label, color) }
                } else {
                    StatusPill(label, color)
                }
            }
        }
    }
}

@Composable
private fun MonthNav(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(CardBackground).border(1.dp, DividerColor, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Navy, modifier = Modifier.size(18.dp)) }
}

private fun money(v: Double): String = "₹" + String.format(Locale("en", "IN"), "%,.0f", v)

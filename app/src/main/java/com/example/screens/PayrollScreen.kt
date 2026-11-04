package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.PayrollBatch
import com.example.data.model.PayTypes
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.repo.PayrollRepository
import com.example.ui.EmptyState
import com.example.ui.LocalAppContainer
import com.example.ui.MeterRow
import com.example.ui.PillChip
import com.example.ui.StatTile
import com.example.ui.SwCard
import com.example.ui.collectAsStateLifecycle
import com.example.ui.contentColorsFor
import com.example.ui.theme.Coral
import com.example.ui.theme.Flame
import com.example.ui.theme.Forest
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.OnDark
import com.example.ui.theme.OnDarkFaint
import com.example.ui.theme.OnDarkMuted
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.Surface3
import com.example.ui.theme.SwType
import com.example.ui.theme.Volt
import com.example.ui.theme.White
import com.example.ui.vm.PayrollViewModel
import com.example.util.CsvExporter
import com.example.util.LocalImage
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Payroll — the month's net payable as the hero figure, a scrollable month
 * strip, colour-blocked totals, then five tabs over the same period: overview,
 * per-worker salary, advances, deductions and payment status.
 */
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
    val batches by remember { container.db.payrollDao().recentBatches(6) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val sites by vm.sites.collectAsStateLifecycle()
    val siteId by vm.siteId.collectAsStateLifecycle()
    val activeSite = sites.firstOrNull { it.id == siteId }

    var toast by remember { mutableStateOf<String?>(null) }

    val workersById = remember(rows) { rows.associate { it.worker.id to it.worker } }
    // When a site is selected the ledger tabs follow the same roster as the rows.
    val scopedTxns = if (siteId == null) txns else txns.filter { it.workerId in workersById }

    PayrollContent(
        state = PayrollUiState(
            period = period,
            rows = rows,
            totals = totals,
            batches = batches,
            advances = scopedTxns.filter { it.status == "active" && it.txnType in listOf("advance", "cash_advance") },
            deductions = scopedTxns.filter { it.status == "active" && !it.isEarning },
            workersById = workersById,
            sites = sites,
            siteId = siteId,
            companyName = company?.name,
            companyLogo = company?.logo,
            loading = loading,
        ),
        isAdmin = isAdmin,
        onSetPeriod = vm::setPeriod,
        onSetSite = vm::setSite,
        onOpenPayslip = onOpenPayslip,
        onTogglePaid = vm::togglePaid,
        onGenerate = {
            if (isAdmin) vm.generate { toast = it } else toast = "Only an administrator can generate payroll."
        },
        onExport = {
            if (rows.isEmpty()) {
                toast = "Nothing to export yet."
            } else {
                val scope = activeSite?.name?.replace(Regex("[^A-Za-z0-9]+"), "_")?.let { "_$it" } ?: ""
                CsvExporter.share(context, "payroll_$period$scope.csv", CsvExporter.payrollCsv(rows, period))
            }
        },
    )

    toast?.let {
        AlertDialog(
            onDismissRequest = { toast = null },
            containerColor = Surface2,
            textContentColor = OnDark,
            confirmButton = { TextButton(onClick = { toast = null }) { Text("OK", color = Flame) } },
            text = { Text(it) },
        )
    }
}

/** Everything the payroll presentation needs, so it renders without a database. */
internal data class PayrollUiState(
    val period: YearMonth,
    val rows: List<PayrollRepository.PayrollRow>,
    val totals: PayrollRepository.PayrollTotals,
    val batches: List<PayrollBatch>,
    val advances: List<WorkerTransaction>,
    val deductions: List<WorkerTransaction>,
    val workersById: Map<Long, Worker>,
    val sites: List<Site>,
    val siteId: Long?,
    val companyName: String?,
    val companyLogo: String?,
    val loading: Boolean,
)

@Composable
internal fun PayrollContent(
    state: PayrollUiState,
    isAdmin: Boolean,
    onSetPeriod: (YearMonth) -> Unit,
    onSetSite: (Long?) -> Unit,
    onOpenPayslip: (Long, YearMonth) -> Unit,
    onTogglePaid: (Long) -> Unit,
    onGenerate: () -> Unit,
    onExport: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val activeSite = state.sites.firstOrNull { it.id == state.siteId }
    val paidAmount = state.rows.filter { it.status == "paid" }.sumOf { it.pay.estimatedPay }
    val pendingAmount = state.rows.filter { it.status != "paid" }.sumOf { it.pay.estimatedPay }
    val advancesGiven = state.advances.sumOf { it.amount }

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
                        Text(
                            state.companyName ?: "SmartWorker",
                            style = SwType.Caption, color = OnDarkMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("Payroll", style = SwType.Headline.copy(fontSize = 26.sp), color = OnDark)
                    }
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Surface3)
                            .clickable(onClick = onExport),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Download, "Export CSV", tint = OnDark, modifier = Modifier.size(19.dp))
                    }
                }
            }

            item {
                SiteSelector(
                    logo = state.companyLogo,
                    title = activeSite?.name ?: (state.companyName ?: "All sites"),
                    subtitle = activeSite
                        ?.let { "Site ID: SITE-${it.id.toString().padStart(4, '0')}" }
                        ?: "Company-wide payroll",
                    sites = state.sites,
                    selectedId = state.siteId,
                    onSelect = onSetSite,
                )
            }

            item { MonthStrip(selected = state.period, onSelect = onSetPeriod) }

            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Flame)
                    }
                }
                return@LazyColumn
            }

            // ── Hero: what actually leaves the account ──
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SwCard(Modifier.fillMaxWidth(), container = Flame) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("NET PAYABLE", style = SwType.Label, color = White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(10.dp))
                            Text(money(state.totals.totalNet), style = SwType.StatHuge, color = White)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${state.totals.workerCount} workers  ·  " +
                                    state.period.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = SwType.Caption, color = White.copy(alpha = 0.88f),
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            money(paidAmount), "Paid",
                            Modifier.weight(1f).height(104.dp), container = Lime,
                            caption = "${state.totals.paidCount} workers",
                        )
                        StatTile(
                            money(pendingAmount), "Pending",
                            Modifier.weight(1f).height(104.dp), container = Sun,
                            caption = "${state.totals.pendingCount} workers",
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            money(state.totals.totalDeductions), "Deductions",
                            Modifier.weight(1f).height(104.dp), container = Coral,
                        )
                        StatTile(
                            money(advancesGiven), "Advances",
                            Modifier.weight(1f).height(104.dp), container = Volt,
                        )
                    }
                }
            }

            // ── Tabs ──
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Overview", "Salary", "Advances", "Deductions", "Payments")
                        .forEachIndexed { i, label ->
                            PillChip(label, tab == i, { tab = i })
                        }
                }
            }

            when (tab) {
                0 -> overviewTab(state, isAdmin, onGenerate) { onSetPeriod(it); tab = 1 }
                1 -> salaryTab(state, isAdmin, onOpenPayslip, onTogglePaid)
                2 -> txnTab(state.advances, state.workersById, "No advances recorded for this month.")
                3 -> txnTab(state.deductions, state.workersById, "No deductions recorded for this month.")
                4 -> paymentsTab(state, onOpenPayslip)
            }
        }
    }
}

// ── Tabs (LazyListScope extensions so each keeps its own item boundaries) ─────

private fun androidx.compose.foundation.lazy.LazyListScope.overviewTab(
    state: PayrollUiState,
    isAdmin: Boolean,
    onGenerate: () -> Unit,
    onOpenBatch: (YearMonth) -> Unit,
) {
    item {
        Box(Modifier.padding(horizontal = 16.dp)) {
            SwCard(Modifier.fillMaxWidth(), container = Forest, onClick = if (isAdmin) onGenerate else null) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Generate payroll", style = SwType.Title.copy(fontSize = 16.sp), color = White)
                        Text(
                            if (isAdmin) {
                                "Recalculate ${state.totals.workerCount} workers for this month"
                            } else {
                                "Only an administrator can generate payroll"
                            },
                            style = SwType.Caption, color = White.copy(alpha = 0.78f),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = White, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }
    }

    // Pay-type split as meters — a quick read on where the money goes.
    val byType = listOf(
        "Daily wage" to state.rows.filter { it.worker.payType == PayTypes.DAILY },
        "Monthly" to state.rows.filter { it.worker.payType == PayTypes.MONTHLY },
        "Hourly" to state.rows.filter { it.worker.payType == PayTypes.HOURLY },
        "Project" to state.rows.filter { it.worker.payType == PayTypes.PROJECT },
    ).filter { it.second.isNotEmpty() }

    if (byType.isNotEmpty()) {
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                Text("Salary distribution", style = SwType.Headline.copy(fontSize = 18.sp), color = OnDark)
            }
        }
        val grand = state.rows.sumOf { it.pay.estimatedPay }.takeIf { it > 0 } ?: 1.0
        val accents = listOf(Flame, Volt, Lime, Sun)
        byType.forEachIndexed { i, (label, group) ->
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    MeterRow(
                        label = "$label  ·  ${group.size}",
                        percent = (group.sumOf { it.pay.estimatedPay } / grand * 100).toInt(),
                        accent = accents[i % accents.size],
                    )
                }
            }
        }
    }

    if (state.batches.isNotEmpty()) {
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    if (state.siteId != null) "Recent batches (all sites)" else "Recent batches",
                    style = SwType.Headline.copy(fontSize = 18.sp), color = OnDark,
                )
            }
        }
        items(state.batches.size) { i ->
            val b = state.batches[i]
            Box(Modifier.padding(horizontal = 16.dp)) {
                BatchRow(b) { onOpenBatch(YearMonth.of(b.year, b.month)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.salaryTab(
    state: PayrollUiState,
    isAdmin: Boolean,
    onOpenPayslip: (Long, YearMonth) -> Unit,
    onTogglePaid: (Long) -> Unit,
) {
    if (state.rows.isEmpty()) {
        item {
            EmptyState(
                Icons.Filled.Payments,
                "No payroll yet",
                "Generate payroll for this month to see per-worker figures.",
            )
        }
        return
    }
    items(state.rows.size) { i ->
        val row = state.rows[i]
        Box(Modifier.padding(horizontal = 16.dp)) {
            WorkerPayRow(
                row = row,
                isAdmin = isAdmin,
                onOpen = { onOpenPayslip(row.worker.id, state.period) },
                onTogglePaid = { row.recordId?.let(onTogglePaid) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.txnTab(
    txns: List<WorkerTransaction>,
    workersById: Map<Long, Worker>,
    emptyMsg: String,
) {
    if (txns.isEmpty()) {
        item { EmptyState(Icons.Filled.Payments, "Nothing here", emptyMsg) }
        return
    }
    items(txns.size) { i ->
        val t = txns[i]
        val worker = workersById[t.workerId]
        val accent = if (t.isEarning) Lime else Coral
        Box(Modifier.padding(horizontal = 16.dp)) {
            SwCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(5.dp).height(58.dp).background(accent))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f).padding(vertical = 11.dp)) {
                        Text(
                            worker?.fullName ?: "Worker #${t.workerId}",
                            style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                t.txnType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                t.date.format(DateTimeFormatter.ofPattern("dd MMM")),
                                t.description.takeIf { it.isNotBlank() },
                            ).joinToString("  ·  "),
                            style = SwType.Caption, color = OnDarkMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        (if (t.isEarning) "+" else "−") + money(t.amount),
                        style = SwType.Title.copy(fontSize = 15.sp), color = accent,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.paymentsTab(
    state: PayrollUiState,
    onOpenPayslip: (Long, YearMonth) -> Unit,
) {
    if (state.rows.isEmpty()) {
        item { EmptyState(Icons.Filled.Payments, "Nothing to pay", "Generate payroll for this month first.") }
        return
    }
    val sorted = state.rows.sortedBy { it.status == "paid" }
    items(sorted.size) { i ->
        val row = sorted[i]
        val paid = row.status == "paid"
        Box(Modifier.padding(horizontal = 16.dp)) {
            SwCard(Modifier.fillMaxWidth(), onClick = { onOpenPayslip(row.worker.id, state.period) }) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(5.dp).height(58.dp).background(if (paid) Lime else Sun))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f).padding(vertical = 11.dp)) {
                        Text(
                            row.worker.fullName,
                            style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (paid) "Paid" else "Awaiting payment",
                            style = SwType.Caption, color = if (paid) Lime else Sun,
                        )
                    }
                    Text(money(row.pay.estimatedPay), style = SwType.Title.copy(fontSize = 15.sp), color = OnDark)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ChevronRight, null, tint = OnDarkFaint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Pieces ───────────────────────────────────────────────────────────────────

@Composable
private fun SiteSelector(
    logo: String?,
    title: String,
    subtitle: String,
    sites: List<Site>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp)) {
        SwCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Surface3),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalImage(logo, "Site", Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))) {
                        Icon(Icons.Filled.Business, null, tint = Flame, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title, style = SwType.Title.copy(fontSize = 15.sp), color = OnDark,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle, style = SwType.Caption, color = OnDarkMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    Row(
                        Modifier.clip(CircleShape).background(Surface3)
                            .clickable { open = true }
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Switch", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnDark)
                        Icon(Icons.Filled.ArrowDropDown, null, tint = OnDarkMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "All sites", fontSize = 14.sp,
                                    fontWeight = if (selectedId == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedId == null) Flame else OnDark,
                                )
                            },
                            onClick = { onSelect(null); open = false },
                        )
                        sites.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        s.name, fontSize = 14.sp,
                                        fontWeight = if (selectedId == s.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedId == s.id) Flame else OnDark,
                                    )
                                },
                                onClick = { onSelect(s.id); open = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStrip(selected: YearMonth, onSelect: (YearMonth) -> Unit) {
    val now = remember { YearMonth.now() }
    val months = remember(selected) { (-4L..1L).map { selected.plusMonths(it) } }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        months.forEach { m ->
            val isSelected = m == selected
            val isNow = m == now
            Column(
                Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) Flame else if (isNow) Surface3 else Surface1)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    m.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                    style = SwType.Title.copy(fontSize = 14.sp),
                    color = if (isSelected) White else OnDark,
                )
                Text(
                    m.year.toString(),
                    style = SwType.Label,
                    color = if (isSelected) White.copy(alpha = 0.85f) else OnDarkMuted,
                )
            }
        }
    }
}

@Composable
private fun BatchRow(b: PayrollBatch, onClick: () -> Unit) {
    val paid = b.status == "paid"
    SwCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    YearMonth.of(b.year, b.month).format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                )
                Text(
                    "${b.workerCount} workers  ·  ${b.paidCount} paid",
                    style = SwType.Caption, color = OnDarkMuted,
                )
            }
            Text(money(b.total), style = SwType.Title.copy(fontSize = 15.sp), color = OnDark)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.clip(CircleShape)
                    .background((if (paid) Lime else Sun).copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    if (paid) "Paid" else "Processing",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (paid) Lime else Sun,
                )
            }
        }
    }
}

@Composable
private fun WorkerPayRow(
    row: PayrollRepository.PayrollRow,
    isAdmin: Boolean,
    onOpen: () -> Unit,
    onTogglePaid: () -> Unit,
) {
    val accent = accentFor(row.worker.workerCode.ifBlank { row.worker.fullName })
    val statusColor = when (row.status) {
        "paid" -> Lime
        "pending" -> Sun
        else -> OnDarkFaint
    }
    SwCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                LocalImage(row.worker.profileImage, null, Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))) {
                    Text(
                        row.worker.fullName.take(1).uppercase(Locale.US),
                        color = contentColorsFor(accent).first,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.worker.fullName, style = SwType.Title.copy(fontSize = 14.5.sp), color = OnDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.pay.paidDays} paid days  ·  ${row.worker.payType.replaceFirstChar { it.uppercase() }}",
                    style = SwType.Caption, color = OnDarkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(row.pay.estimatedPay), style = SwType.Title.copy(fontSize = 15.sp), color = OnDark)
                Spacer(Modifier.height(5.dp))
                val label = when (row.status) {
                    "paid" -> "Paid"
                    "pending" -> "Pending"
                    else -> "Unsaved"
                }
                Box(
                    Modifier.clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.18f))
                        .then(
                            if (isAdmin && row.recordId != null) {
                                Modifier.clickable(onClick = onTogglePaid)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
        }
    }
}

private fun money(v: Double): String =
    "₹" + String.format(Locale.forLanguageTag("en-IN"), "%,.0f", v)

package com.example.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CompanySetting
import com.example.data.model.Notification
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.repo.AttendanceRepository
import com.example.data.repo.PayrollRepository
import com.example.domain.PayrollCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// ─────────────────────────────────────────────────────────────────────────────
//  Shared factory — hands each ViewModel the AppContainer.
// ─────────────────────────────────────────────────────────────────────────────
class VmFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(c)
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(c)
        modelClass.isAssignableFrom(WorkersViewModel::class.java) -> WorkersViewModel(c)
        modelClass.isAssignableFrom(AttendanceViewModel::class.java) -> AttendanceViewModel(c)
        modelClass.isAssignableFrom(PayrollViewModel::class.java) -> PayrollViewModel(c)
        modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(c)
        modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(c)
        modelClass.isAssignableFrom(MoreViewModel::class.java) -> MoreViewModel(c)
        modelClass.isAssignableFrom(QuickMarkViewModel::class.java) -> QuickMarkViewModel(c)
        modelClass.isAssignableFrom(PayslipViewModel::class.java) -> PayslipViewModel(c)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}

// ─────────────────────────────────────────────────────────────────────────────
//  App-level: authentication + one-time UI events (snackbars).
// ─────────────────────────────────────────────────────────────────────────────
class AppViewModel(private val c: AppContainer) : ViewModel() {
    val currentUser: StateFlow<User?> = c.authRepository.currentUser
    val company: StateFlow<CompanySetting?> =
        c.catalogRepository.company.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val unreadNotifications: StateFlow<Int> =
        c.catalogRepository.unreadCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()
    fun showMessage(message: String) { _snackbar.value = message }
    fun clearMessage() { _snackbar.value = null }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val r = c.authRepository.login(username, password)) {
                is com.example.data.repo.AuthRepository.LoginResult.Success -> {
                    _loginError.value = null
                    onSuccess()
                }
                is com.example.data.repo.AuthRepository.LoginResult.Error -> _loginError.value = r.message
            }
        }
    }

    fun clearLoginError() { _loginError.value = null }
    fun logout() = c.authRepository.logout()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dashboard
// ─────────────────────────────────────────────────────────────────────────────
class DashboardViewModel(c: AppContainer) : ViewModel() {
    data class DashboardState(
        val totalWorkers: Int = 0,
        val present: Int = 0,
        val absent: Int = 0,
        val late: Int = 0,
        val leave: Int = 0,
        val recent: List<Pair<AttendanceRecord, Worker>> = emptyList(),
    ) {
        val marked get() = present + absent + late + leave
        val attendancePct get() = if (totalWorkers > 0) (present + late) * 100.0 / totalWorkers else 0.0
    }

    private val today = LocalDate.now()

    val state: StateFlow<DashboardState> = combine(
        c.workerRepository.activeWorkers,
        c.attendanceRepository.recordsOn(today),
    ) { workers, records ->
        val byId = workers.associateBy { it.id }
        val summary = PayrollCalculator.attendanceSummary(records)
        DashboardState(
            totalWorkers = workers.size,
            present = summary.presentDays,
            absent = summary.absentDays,
            late = summary.lateDays,
            leave = summary.leaveDays,
            recent = records.mapNotNull { r -> byId[r.workerId]?.let { r to it } }.take(8),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}

// ─────────────────────────────────────────────────────────────────────────────
//  Workers
// ─────────────────────────────────────────────────────────────────────────────
class WorkersViewModel(private val c: AppContainer) : ViewModel() {
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()
    private val _tab = MutableStateFlow(0) // 0 All, 1 Active, 2 Inactive, 3 On Leave
    val tab: StateFlow<Int> = _tab.asStateFlow()

    fun setSearch(q: String) { _search.value = q }
    fun setTab(i: Int) { _tab.value = i }

    val workers: StateFlow<List<Worker>> = combine(
        c.workerRepository.allWorkers, _search, _tab,
    ) { all, q, tab ->
        all.filter { w ->
            val matches = q.isBlank() ||
                w.fullName.contains(q, true) || w.workerCode.contains(q, true) || w.phone.contains(q)
            val tabOk = when (tab) {
                1 -> w.status == "active"
                2 -> w.status == "inactive"
                else -> true
            }
            matches && tabOk
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class Counts(val all: Int, val active: Int, val inactive: Int)
    val counts: StateFlow<Counts> = c.workerRepository.allWorkers.let { flow ->
        combine(flow, _search) { all, _ ->
            Counts(all.size, all.count { it.status == "active" }, all.count { it.status == "inactive" })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Counts(0, 0, 0))

    fun toggleActive(workerId: Long, onDone: (String?) -> Unit) {
        viewModelScope.launch { onDone(c.workerRepository.toggleActive(workerId)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Attendance
// ─────────────────────────────────────────────────────────────────────────────
class AttendanceViewModel(private val c: AppContainer) : ViewModel() {
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    fun setDate(d: LocalDate) { _date.value = d }
    fun shiftDate(days: Long) { _date.value = _date.value.plusDays(days) }
    fun today() { _date.value = LocalDate.now() }
    fun setSearch(q: String) { _search.value = q }

    data class Row(val worker: Worker, val record: AttendanceRecord?)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val recordsForDate = _date.flatMapLatest { d -> c.attendanceRepository.recordsOn(d) }

    val rows: StateFlow<List<Row>> = combine(
        c.workerRepository.activeWorkers,
        recordsForDate,
        _search,
    ) { workers, records, q ->
        val recByWorker = records.associateBy { it.workerId }
        workers.filter { q.isBlank() || it.fullName.contains(q, true) || it.workerCode.contains(q, true) }
            .map { Row(it, recByWorker[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class Totals(val present: Int, val absent: Int, val late: Int, val leave: Int, val expected: Int)
    val totals: StateFlow<Totals> = rows.let { flow ->
        combine(flow, _date) { rs, _ ->
            Totals(
                present = rs.count { it.record?.status == AttendanceStatus.PRESENT },
                absent = rs.count { it.record?.status == AttendanceStatus.ABSENT },
                late = rs.count { it.record?.status == AttendanceStatus.LATE },
                leave = rs.count { it.record?.status == AttendanceStatus.LEAVE },
                expected = rs.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Totals(0, 0, 0, 0, 0))

    fun mark(actor: User, worker: Worker, status: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val r = c.attendanceRepository.mark(actor, worker, _date.value, status)
            onResult(
                when (r) {
                    is AttendanceRepository.MarkResult.Ok -> r.message
                    is AttendanceRepository.MarkResult.Denied -> r.message
                }
            )
        }
    }

    fun bulkMark(actor: User, status: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val r = c.attendanceRepository.bulkMark(actor, _date.value, status)
            onResult(
                when (r) {
                    is AttendanceRepository.MarkResult.Ok -> r.message
                    is AttendanceRepository.MarkResult.Denied -> r.message
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Payroll
// ─────────────────────────────────────────────────────────────────────────────
class PayrollViewModel(private val c: AppContainer) : ViewModel() {
    private val _period = MutableStateFlow(YearMonth.now())
    val period: StateFlow<YearMonth> = _period.asStateFlow()

    private val _rows = MutableStateFlow<List<PayrollRepository.PayrollRow>>(emptyList())
    val rows: StateFlow<List<PayrollRepository.PayrollRow>> = _rows.asStateFlow()
    private val _totals = MutableStateFlow(PayrollRepository.PayrollTotals(0.0, 0.0, 0.0, 0, 0, 0))
    val totals: StateFlow<PayrollRepository.PayrollTotals> = _totals.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { refresh() }

    fun setPeriod(ym: YearMonth) { _period.value = ym; refresh() }
    fun shiftMonth(months: Long) { _period.value = _period.value.plusMonths(months); refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val (rows, totals) = c.payrollRepository.buildRows(_period.value.monthValue, _period.value.year)
            _rows.value = rows
            _totals.value = totals
            _loading.value = false
        }
    }

    fun generate(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val count = c.payrollRepository.generate(_period.value.monthValue, _period.value.year)
            refresh()
            onDone("Payroll generated for $count worker(s).")
        }
    }

    fun togglePaid(recordId: Long) {
        viewModelScope.launch {
            c.payrollRepository.togglePaid(recordId)
            refresh()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Transactions
// ─────────────────────────────────────────────────────────────────────────────
class TransactionsViewModel(private val c: AppContainer) : ViewModel() {
    private val _period = MutableStateFlow(YearMonth.now())
    val period: StateFlow<YearMonth> = _period.asStateFlow()

    fun shiftMonth(months: Long) { _period.value = _period.value.plusMonths(months) }

    val workers: StateFlow<List<Worker>> =
        c.workerRepository.activeWorkers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentTransactions: StateFlow<List<WorkerTransaction>> = _period.flatMapLatest { ym ->
        c.catalogRepository.transactionsBetween(ym.atDay(1), ym.atEndOfMonth())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totals: StateFlow<Pair<Double, Double>> = currentTransactions.let { flow ->
        combine(flow, _period) { txns, _ ->
            val earn = txns.filter { it.status == "active" && it.isEarning }.sumOf { it.amount }
            val ded = txns.filter { it.status == "active" && !it.isEarning }.sumOf { it.amount }
            earn to ded
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0 to 0.0)

    fun save(txn: WorkerTransaction, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val err = c.catalogRepository.saveTransaction(txn)
            onResult(err ?: "Transaction saved. Payroll updates automatically.")
        }
    }

    fun toggle(id: Long) { viewModelScope.launch { c.catalogRepository.toggleTransaction(id) } }
    fun delete(id: Long) { viewModelScope.launch { c.catalogRepository.deleteTransaction(id) } }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Notifications
// ─────────────────────────────────────────────────────────────────────────────
class NotificationsViewModel(private val c: AppContainer) : ViewModel() {
    val notifications: StateFlow<List<Notification>> =
        c.catalogRepository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAllRead() { viewModelScope.launch { c.catalogRepository.markNotificationsRead() } }
}

// ─────────────────────────────────────────────────────────────────────────────
//  More / profile
// ─────────────────────────────────────────────────────────────────────────────
class MoreViewModel(c: AppContainer) : ViewModel() {
    val company: StateFlow<CompanySetting?> =
        c.catalogRepository.company.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Quick Mark (QR / worker-ID attendance)
// ─────────────────────────────────────────────────────────────────────────────
class QuickMarkViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<AttendanceRepository.Lookup?>(null)
    val state: StateFlow<AttendanceRepository.Lookup?> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun reset() { _state.value = null; _message.value = null }

    fun lookup(actor: User, code: String) {
        viewModelScope.launch {
            _busy.value = true
            _state.value = c.attendanceRepository.lookup(actor, code, c.workerRepository)
            _busy.value = false
        }
    }

    /** Check the worker in (first tap) or out (second tap on an open shift). */
    fun markPresent(actor: User, worker: Worker) {
        viewModelScope.launch {
            _busy.value = true
            val r = c.attendanceRepository.mark(
                actor, worker, LocalDate.now(), AttendanceStatus.PRESENT, markedVia = "worker_id",
            )
            _message.value = when (r) {
                is AttendanceRepository.MarkResult.Ok -> r.message
                is AttendanceRepository.MarkResult.Denied -> r.message
            }
            // Refresh the lookup so the buttons reflect the new state.
            _state.value = c.attendanceRepository.lookup(actor, worker.workerCode, c.workerRepository)
            _busy.value = false
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Payslip (per-worker monthly breakdown)
// ─────────────────────────────────────────────────────────────────────────────
class PayslipViewModel(private val c: AppContainer) : ViewModel() {
    data class SlipState(
        val row: PayrollRepository.PayrollRow? = null,
        val company: CompanySetting? = null,
        val loading: Boolean = true,
    )

    private val _state = MutableStateFlow(SlipState())
    val state: StateFlow<SlipState> = _state.asStateFlow()

    fun load(workerId: Long, period: YearMonth) {
        viewModelScope.launch {
            _state.value = SlipState(loading = true)
            val worker = c.workerRepository.workerOnce(workerId)
            if (worker == null) {
                _state.value = SlipState(loading = false)
                return@launch
            }
            val row = c.payrollRepository.slip(worker, period.monthValue, period.year)
            val company = c.db.companyDao().settings().firstOrNull()
            _state.value = SlipState(row = row, company = company, loading = false)
        }
    }
}

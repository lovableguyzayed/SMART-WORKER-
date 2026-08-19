package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.update.DownloadState
import com.example.update.UpdateDialog
import com.example.update.UpdateState
import com.example.update.UpdateViewModel
import com.example.ui.LocalAppContainer
import com.example.ui.collectAsStateLifecycle
import com.example.screens.SmartWorkerAttendanceScreen
import com.example.screens.SmartWorkerMoreScreen
import com.example.screens.SmartWorkerPayrollScreen
import com.example.screens.SmartWorkerWorkerDetailsScreen
import com.example.screens.SmartWorkerWorkersScreen
import com.example.screens.HomeScreen
import com.example.screens.LoginScreen
import com.example.screens.NotificationsScreen
import com.example.screens.TransactionsScreen
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.DividerColor
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SmartWorkerTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.White
import com.example.ui.vm.AppViewModel
import com.example.ui.vm.VmFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SmartWorkerApp).container
        setContent {
            SmartWorkerTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    val factory = remember { VmFactory(container) }
                    val appVm: AppViewModel = viewModel(factory = factory)
                    AppRoot(appVm, factory)
                    // OTA self-update: overlays an AlertDialog when a newer APK is hosted.
                    UpdateGate()
                }
            }
        }
    }
}

@Composable
fun AppRoot(appVm: AppViewModel, factory: VmFactory) {
    val currentUser by appVm.currentUser.collectAsStateLifecycle()
    val snackbarMessage by appVm.snackbar.collectAsStateLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHost.showSnackbar(it)
            appVm.clearMessage()
        }
    }

    if (currentUser == null) {
        LoginScreen(appVm)
    } else {
        MainShell(appVm, factory, snackbarHost)
    }
}

/**
 * Checks the hosted manifest on launch and drives the OTA dialog. Renders nothing
 * until an update is available, so it can safely overlay the whole app.
 */
@Composable
private fun UpdateGate() {
    val vm: UpdateViewModel = viewModel()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val downloadState by vm.downloadState.collectAsStateWithLifecycle()

    // Run the version check once, off the main thread (inside the ViewModel).
    LaunchedEffect(Unit) { vm.checkForUpdate() }

    // API 26+ install-permission round trip: open the "unknown apps" settings
    // screen, then resume the install automatically when the user returns.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { vm.onInstallPermissionResult() }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.NeedsPermission) {
            permissionLauncher.launch(vm.installPermissionIntent())
        }
    }

    (updateState as? UpdateState.UpdateAvailable)?.let { available ->
        UpdateDialog(
            update = available,
            downloadState = downloadState,
            onDownload = { vm.downloadAndInstall(available.manifest) },
            onDismiss = { vm.dismissUpdate() },
        )
    }
}

@Composable
fun MainShell(appVm: AppViewModel, factory: VmFactory, snackbarHost: SnackbarHostState) {
    var currentScreen by remember { mutableStateOf("home") }
    var detailWorkerId by remember { mutableStateOf<Long?>(null) }
    var payslipWorkerId by remember { mutableStateOf<Long?>(null) }
    var payslipPeriod by remember { mutableStateOf(java.time.YearMonth.now()) }
    var formEditWorkerId by remember { mutableStateOf<Long?>(null) }
    var reportWorkerId by remember { mutableStateOf<Long?>(null) }
    var idCardWorkerId by remember { mutableStateOf<Long?>(null) }
    val currentUser by appVm.currentUser.collectAsStateLifecycle()
    val user = currentUser ?: return

    val isTab = currentScreen in listOf("home", "workers", "attendance", "payroll", "more", "quick_mark")

    // System back mirrors the on-screen back arrows instead of exiting the app.
    androidx.activity.compose.BackHandler(enabled = !isTab || currentScreen != "home") {
        currentScreen = when (currentScreen) {
            "worker_details" -> "workers"
            "worker_form" -> if (formEditWorkerId == null) "workers" else "worker_details"
            "worker_report" -> if (detailWorkerId != null) "worker_details" else "reports"
            "id_card" -> "worker_details"
            "payslip" -> "payroll"
            "quick_mark" -> "attendance"
            "transactions", "manage", "closures", "attendance_users", "company_settings", "reports" -> "more"
            "notifications" -> "home"
            else -> "home"
        }
    }

    Scaffold(
        bottomBar = {
            if (isTab) UnifiedBottomNavBar(currentScreen) { currentScreen = it }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTab) padding else PaddingValues(0.dp))
        ) {
            Crossfade(targetState = currentScreen, label = "screen") { screen ->
                when (screen) {
                    "home" -> HomeScreen(
                        vm = viewModel(factory = factory),
                        user = user,
                        onOpenNotifications = { currentScreen = "notifications" },
                        onQuickAction = { currentScreen = it },
                    )
                    "workers" -> SmartWorkerWorkersScreen(
                        vm = viewModel(factory = factory),
                        onOpenWorker = { id -> detailWorkerId = id; currentScreen = "worker_details" },
                        isAdmin = user.isAdmin,
                        onAddWorker = { formEditWorkerId = null; currentScreen = "worker_form" },
                    )
                    "attendance" -> SmartWorkerAttendanceScreen(
                        vm = viewModel(factory = factory),
                        user = user,
                        onOpenQuickMark = { currentScreen = "quick_mark" },
                    )
                    "payroll" -> SmartWorkerPayrollScreen(
                        vm = viewModel(factory = factory),
                        isAdmin = user.isAdmin,
                        onOpenPayslip = { workerId, period ->
                            payslipWorkerId = workerId
                            payslipPeriod = period
                            currentScreen = "payslip"
                        },
                    )
                    "more" -> SmartWorkerMoreScreen(
                        appVm = appVm,
                        onNavigate = { destination -> currentScreen = destination },
                        onLogout = { appVm.logout() },
                    )
                    "transactions" -> TransactionsScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "more" },
                    )
                    "notifications" -> NotificationsScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "home" },
                    )
                    "worker_details" -> SmartWorkerWorkerDetailsScreen(
                        workerId = detailWorkerId ?: 0L,
                        user = user,
                        adminVm = viewModel(factory = factory),
                        onBack = { currentScreen = "workers" },
                        onEdit = { id -> formEditWorkerId = id; currentScreen = "worker_form" },
                        onOpenReport = { id -> reportWorkerId = id; currentScreen = "worker_report" },
                        onOpenIdCard = { id -> idCardWorkerId = id; currentScreen = "id_card" },
                    )
                    "id_card" -> com.example.screens.IdCardScreen(
                        workerId = idCardWorkerId ?: 0L,
                        onBack = { currentScreen = "worker_details" },
                    )
                    "worker_form" -> com.example.screens.WorkerFormScreen(
                        vm = viewModel(factory = factory),
                        editWorkerId = formEditWorkerId,
                        onBack = {
                            currentScreen = if (formEditWorkerId == null) "workers" else "worker_details"
                        },
                        onSaved = { message ->
                            appVm.showMessage(message)
                            currentScreen = if (formEditWorkerId == null) "workers" else "worker_details"
                        },
                    )
                    "worker_report" -> com.example.screens.WorkerReportScreen(
                        vm = viewModel(factory = factory),
                        workerId = reportWorkerId ?: 0L,
                        onBack = { currentScreen = if (detailWorkerId != null) "worker_details" else "reports" },
                    )
                    "manage" -> com.example.screens.ManageScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "more" },
                        onOpenCompany = { currentScreen = "company_settings" },
                        onOpenAttendanceUsers = { currentScreen = "attendance_users" },
                    )
                    "closures" -> com.example.screens.ClosuresScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "more" },
                    )
                    "attendance_users" -> com.example.screens.AttendanceUsersScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "more" },
                    )
                    "company_settings" -> com.example.screens.CompanySettingsScreen(
                        vm = viewModel(factory = factory),
                        onBack = { currentScreen = "more" },
                    )
                    "reports" -> com.example.screens.ReportsScreen(
                        onBack = { currentScreen = "more" },
                        onOpenWorkerReport = { id ->
                            detailWorkerId = null
                            reportWorkerId = id
                            currentScreen = "worker_report"
                        },
                    )
                    "quick_mark" -> com.example.screens.QuickMarkScreen(
                        vm = viewModel(factory = factory),
                        user = user,
                        onBack = { currentScreen = "attendance" },
                    )
                    "payslip" -> com.example.screens.PayslipScreen(
                        vm = viewModel(factory = factory),
                        workerId = payslipWorkerId ?: 0L,
                        period = payslipPeriod,
                        onBack = { currentScreen = "payroll" },
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedBottomNavBar(currentTab: String, onTabSelected: (String) -> Unit) {
    // Floating rounded nav frame with a centred gradient "Scan QR" action —
    // mirrors the web app's .app-bottom-nav-frame / .app-nav-fab.
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(82.dp),
            shape = RoundedCornerShape(28.dp),
            color = White,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavTab(Icons.Filled.Home, "Home", currentTab == "home", Modifier.weight(1f)) { onTabSelected("home") }
                NavTab(Icons.Filled.Groups, "Workers", currentTab == "workers", Modifier.weight(1f)) { onTabSelected("workers") }
                ScanFab(currentTab == "quick_mark") { onTabSelected("quick_mark") }
                NavTab(Icons.Filled.Payments, "Payroll", currentTab == "payroll", Modifier.weight(1f)) { onTabSelected("payroll") }
                NavTab(Icons.Filled.MoreHoriz, "More", currentTab == "more", Modifier.weight(1f)) { onTabSelected("more") }
            }
        }
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg by animateColorAsState(if (selected) DarkBlue else TextSecondary, label = "fg")
    val iconBg by animateColorAsState(
        if (selected) PrimaryBlue.copy(alpha = 0.14f) else Color.Transparent,
        label = "iconBg",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.height(3.dp))
        Text(
            label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Centre action — opens Quick Mark (scan-to-attend), the app's primary action. */
@Composable
private fun ScanFab(selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(width = 60.dp, height = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(DarkBlue, PrimaryBlue)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.QrCodeScanner, "Scan QR", tint = White,
                modifier = Modifier.size(if (selected) 27.dp else 25.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text("Scan QR", color = White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

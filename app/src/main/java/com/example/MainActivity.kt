package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SelectedCardBackground
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

@Composable
fun MainShell(appVm: AppViewModel, factory: VmFactory, snackbarHost: SnackbarHostState) {
    var currentScreen by remember { mutableStateOf("home") }
    var detailWorkerId by remember { mutableStateOf<Long?>(null) }
    var payslipWorkerId by remember { mutableStateOf<Long?>(null) }
    var payslipPeriod by remember { mutableStateOf(java.time.YearMonth.now()) }
    val currentUser by appVm.currentUser.collectAsStateLifecycle()
    val user = currentUser ?: return

    val isTab = currentScreen in listOf("home", "workers", "attendance", "payroll", "more")

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
                        onOpenTransactions = { currentScreen = "transactions" },
                        onOpenNotifications = { currentScreen = "notifications" },
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
                        onBack = { currentScreen = "workers" },
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
    Surface(color = White, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedTab(Icons.Filled.Home, "Home", currentTab == "home") { onTabSelected("home") }
            AnimatedTab(Icons.Filled.Groups, "Workers", currentTab == "workers") { onTabSelected("workers") }
            AnimatedTab(Icons.Filled.CalendarToday, "Attendance", currentTab == "attendance") { onTabSelected("attendance") }
            AnimatedTab(Icons.Filled.Payments, "Payroll", currentTab == "payroll") { onTabSelected("payroll") }
            AnimatedTab(Icons.Filled.MoreHoriz, "More", currentTab == "more") { onTabSelected("more") }
        }
    }
}

@Composable
private fun AnimatedTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SelectedCardBackground else White, label = "bg")
    val fg by animateColorAsState(if (selected) PrimaryBlue else TextSecondary, label = "fg")
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(24.dp))
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

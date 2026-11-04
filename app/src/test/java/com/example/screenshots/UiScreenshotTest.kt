package com.example.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClosureDay
import com.example.data.model.PayTypes
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.screens.DashboardContent
import com.example.screens.WorkersContent
import com.example.ui.MeterRow
import com.example.ui.PillChip
import com.example.ui.SectionHeader
import com.example.ui.StatTile
import com.example.ui.StatusPill
import com.example.ui.SwTopBar
import com.example.ui.theme.Coral
import com.example.ui.theme.Cream
import com.example.ui.theme.Flame
import com.example.ui.theme.Ink
import com.example.ui.theme.Lime
import com.example.ui.theme.SmartWorkerTheme
import com.example.ui.theme.Sun
import com.example.ui.theme.Surface1
import com.example.ui.theme.Volt
import com.example.ui.vm.DashboardViewModel
import com.example.ui.vm.WorkersViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real composables to PNG so the redesign can be reviewed without a
 * device. Fed from fixtures rather than the database, so every run is identical.
 *
 * Record with:  ./gradlew testDebugUnitTest -Proborazzi.test.record=true
 * Output:       app/build/outputs/roborazzi/
 */
// Robolectric 4.16 ships no SDK 36 runtime, and leaving `sdk` unset makes it
// default to the app's targetSdk — which throws before any test runs. Pin to
// the newest SDK it does support.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class UiScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val admin = User(
        id = 1,
        username = "admin",
        passwordHash = "",
        fullName = "Ramesh Kumar",
        role = "admin",
    )

    private fun worker(id: Long, name: String, code: String) = Worker(
        id = id,
        workerCode = code,
        fullName = name,
        phone = "98765${43200 + id}",
        position = "Mason",
        department = "Masonry",
        employeeType = "Daily Wage",
        joinDate = LocalDate.of(2025, 4, 12),
        payType = PayTypes.DAILY,
        dailyRate = 780.0,
    )

    private fun record(workerId: Long, status: String, hour: Int, late: Int = 0) = AttendanceRecord(
        id = workerId,
        workerId = workerId,
        date = LocalDate.of(2026, 11, 4),
        checkInTime = LocalDateTime.of(2026, 11, 4, hour, if (late > 0) late else 2),
        status = status,
        lateMinutes = late,
    )

    private val dashboardState = DashboardViewModel.DashboardState(
        totalWorkers = 48,
        present = 31,
        absent = 6,
        late = 4,
        leave = 2,
        recent = listOf(
            record(1, AttendanceStatus.PRESENT, 8) to worker(1, "Suresh Patil", "MA001"),
            record(2, AttendanceStatus.LATE, 9, late = 24) to worker(2, "Anita Deshmukh", "MA002"),
            record(3, AttendanceStatus.PRESENT, 8) to worker(3, "Imran Shaikh", "CA014"),
            record(4, AttendanceStatus.ABSENT, 0) to worker(4, "Vikram Rathod", "EL007"),
            record(5, AttendanceStatus.LEAVE, 0) to worker(5, "Priya Nair", "MA009"),
        ),
    )

    private val closures = listOf(
        ClosureDay(
            id = 1,
            date = LocalDate.of(2026, 11, 14),
            reason = "Diwali — site shutdown",
            type = "holiday",
            allowAttendance = false,
        ),
        ClosureDay(
            id = 2,
            date = LocalDate.of(2026, 11, 27),
            reason = "Monsoon maintenance",
            type = "maintenance",
            allowAttendance = true,
        ),
    )

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { SmartWorkerTheme { content() } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun dashboard() = shot("01-dashboard") {
        DashboardContent(
            state = dashboardState,
            upcomingClosures = closures,
            companyName = "Shreeji Constructions",
            companyLogo = null,
            unread = 3,
            user = admin,
            onOpenNotifications = {},
            onQuickAction = {},
        )
    }

    @Test
    fun dashboardAllClear() = shot("02-dashboard-all-clear") {
        DashboardContent(
            state = dashboardState.copy(present = 46, absent = 0, late = 2, leave = 0),
            upcomingClosures = emptyList(),
            companyName = "Shreeji Constructions",
            companyLogo = null,
            unread = 0,
            user = admin,
            onOpenNotifications = {},
            onQuickAction = {},
        )
    }

    @Test
    fun workers() = shot("03-workers") {
        WorkersContent(
            workers = listOf(
                worker(1, "Suresh Patil", "MA001"),
                worker(2, "Anita Deshmukh", "MA002"),
                worker(3, "Imran Shaikh", "CA014"),
                worker(4, "Vikram Rathod", "EL007").copy(status = "inactive"),
                worker(5, "Priya Nair", "MA009"),
                worker(6, "Joseph D'Souza", "PL003"),
            ),
            search = "",
            tab = 0,
            counts = WorkersViewModel.Counts(all = 48, active = 44, inactive = 4),
            isAdmin = true,
            onSearch = {},
            onTab = {},
            onOpenWorker = {},
            onAddWorker = {},
        )
    }

    @Test
    fun designSystem() = shot("04-design-system") {
        Column(
            Modifier.fillMaxSize().background(Ink).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SwTopBar(title = "Design system", unreadCount = 5, onSearch = {}, onNotifications = {})
            SectionHeader("Stat tiles", action = "See all")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("31", "Present", Modifier.weight(1f).height(108.dp), container = Lime)
                StatTile("6", "Absent", Modifier.weight(1f).height(108.dp), container = Coral)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("4", "Late", Modifier.weight(1f).height(108.dp), container = Sun)
                StatTile("48", "Roster", Modifier.weight(1f).height(108.dp), container = Volt)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("₹4.2L", "Payroll", Modifier.weight(1f).height(108.dp), container = Cream)
                StatTile("92%", "Rate", Modifier.weight(1f).height(108.dp), container = Surface1)
            }
            SectionHeader("Meters")
            MeterRow("Attendance", 91, Lime)
            MeterRow("Payroll cleared", 64, Flame)
            SectionHeader("Chips & pills")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Today", true, {})
                PillChip("Week", false, {})
                PillChip("Month", false, {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Present", Lime)
                StatusPill("Late", Sun)
                StatusPill("Absent", Coral)
                StatusPill("Leave", Volt)
            }
        }
    }
}

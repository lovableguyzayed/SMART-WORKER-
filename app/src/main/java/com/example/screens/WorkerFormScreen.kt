package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PayTypes
import com.example.ui.CardBorder
import com.example.ui.SwTopBar
import com.example.ui.collectAsStateLifecycle
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TextSecondary
import com.example.ui.vm.EMPLOYEE_TYPES
import com.example.ui.vm.WorkerFormViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Add / edit worker — full port of the Flask worker form, including the
 * per-pay-type policy sections (shift, overtime, late policy, half-day rules,
 * leave policy, closure extra pay).
 */
@Composable
fun WorkerFormScreen(
    vm: WorkerFormViewModel,
    editWorkerId: Long?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val form by vm.form.collectAsStateLifecycle()
    val error by vm.error.collectAsStateLifecycle()
    val departments by vm.departments.collectAsStateLifecycle()

    LaunchedEffect(editWorkerId) { if (editWorkerId != null && editWorkerId > 0) vm.load(editWorkerId) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = if (form.editingId == null) "Add Worker" else "Edit Worker",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Spacer(Modifier.height(12.dp))

            FormCard("Identity") {
                FormField("Full name *", form.fullName) { v -> vm.update { it.copy(fullName = v) } }
                FormField("Phone *", form.phone, KeyboardType.Phone) { v -> vm.update { it.copy(phone = v) } }
                FormField("Email", form.email, KeyboardType.Email) { v -> vm.update { it.copy(email = v) } }
                FormField("Address", form.address) { v -> vm.update { it.copy(address = v) } }
            }

            Spacer(Modifier.height(12.dp))

            FormCard("Job") {
                FormField("Designation *", form.position) { v -> vm.update { it.copy(position = v) } }
                FormDropdown(
                    label = "Department *",
                    value = form.department,
                    options = departments.map { it.name },
                ) { v -> vm.update { it.copy(department = v) } }
                FormDropdown(
                    label = "Worker category",
                    value = form.employeeType,
                    options = EMPLOYEE_TYPES,
                ) { v -> vm.update { it.copy(employeeType = v) } }
                JoinDateField(form.joinDate) { d -> vm.update { it.copy(joinDate = d) } }
            }

            Spacer(Modifier.height(12.dp))

            FormCard("Pay type") {
                PayTypeSelector(form.payType) { v -> vm.update { it.copy(payType = v) } }
                Spacer(Modifier.height(12.dp))
                when (form.payType) {
                    PayTypes.DAILY -> DailySection(vm, form)
                    PayTypes.MONTHLY -> MonthlySection(vm, form)
                    PayTypes.HOURLY -> {
                        FormField("Hourly rate (₹) *", form.hourlyRate, KeyboardType.Decimal) { v -> vm.update { it.copy(hourlyRate = v) } }
                        FormField("Standard working hours / day", form.standardWorkingHours, KeyboardType.Number) { v -> vm.update { it.copy(standardWorkingHours = v) } }
                        Text(
                            "Hourly wage is computed from check-in/check-out duration.",
                            fontSize = 12.sp, color = TextSecondary,
                        )
                    }
                    PayTypes.PROJECT -> {
                        FormField("Project rate (₹) *", form.projectRate, KeyboardType.Decimal) { v -> vm.update { it.copy(projectRate = v) } }
                        Text(
                            "Fixed rate per payroll period. Project delay penalties (configured on the project) are deducted automatically.",
                            fontSize = 12.sp, color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { vm.save(onSaved) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Text(
                    if (form.editingId == null) "Add Worker" else "Save Changes",
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
            title = { Text("Check the form") },
            text = { Text(msg) },
        )
    }
}

// ── Pay-type sections ────────────────────────────────────────────────────────

@Composable
private fun DailySection(vm: WorkerFormViewModel, form: WorkerFormViewModel.FormState) {
    FormField("Daily rate (₹) *", form.dailyRate, KeyboardType.Decimal) { v -> vm.update { it.copy(dailyRate = v) } }
    ShiftFields(vm, form)
    OvertimeFields(vm, form)
    LatePolicyFields(vm, form)
    SwitchRow(
        "No work, no pay",
        "Absent days are unpaid. Turn off to pay the daily rate on absent days.",
        form.noWorkNoPay,
    ) { v -> vm.update { it.copy(noWorkNoPay = v) } }
    FormField("Half-day rate (₹, blank = half of daily)", form.halfDayRate, KeyboardType.Decimal) { v -> vm.update { it.copy(halfDayRate = v) } }
    FormField("Half-day grace minutes (15–25)", form.halfDayGraceMinutes, KeyboardType.Number) { v -> vm.update { it.copy(halfDayGraceMinutes = v) } }
}

@Composable
private fun MonthlySection(vm: WorkerFormViewModel, form: WorkerFormViewModel.FormState) {
    FormField("Monthly salary (₹) *", form.monthlySalary, KeyboardType.Decimal) { v -> vm.update { it.copy(monthlySalary = v) } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            FormField("Working days / month", form.monthlyWorkingDays, KeyboardType.Number) { v -> vm.update { it.copy(monthlyWorkingDays = v) } }
        }
        Column(Modifier.weight(1f)) {
            FormField("Hours / day", form.standardWorkingHours, KeyboardType.Number) { v -> vm.update { it.copy(standardWorkingHours = v) } }
        }
    }
    ShiftFields(vm, form)
    OvertimeFields(vm, form)
    LatePolicyFields(vm, form)

    SwitchRow(
        "Leave policy",
        "Monthly quota accumulates; extra leaves beyond quota + balance are deducted.",
        form.leavePolicyEnabled,
    ) { v -> vm.update { it.copy(leavePolicyEnabled = v) } }
    if (form.leavePolicyEnabled) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                FormField("Allowed leaves / month", form.allowedLeaves, KeyboardType.Number) { v -> vm.update { it.copy(allowedLeaves = v) } }
            }
            Column(Modifier.weight(1f)) {
                FormField("Deduction / extra day (₹)", form.leaveDeduction, KeyboardType.Decimal) { v -> vm.update { it.copy(leaveDeduction = v) } }
            }
        }
    }

    SwitchRow(
        "Closure day extra pay",
        "Bonus for working on closure days (holidays).",
        form.closureExtraPayEnabled,
    ) { v -> vm.update { it.copy(closureExtraPayEnabled = v) } }
    if (form.closureExtraPayEnabled) {
        FormDropdown(
            label = "Calculation method",
            value = form.closureCalculationMethod,
            options = listOf("daily_percent", "hourly_percent", "minute_percent"),
            display = { it.replace('_', ' ') },
        ) { v -> vm.update { it.copy(closureCalculationMethod = v) } }
        FormField("Extra percentage (%)", form.closureExtraPercentage, KeyboardType.Decimal) { v -> vm.update { it.copy(closureExtraPercentage = v) } }
    }
}

@Composable
private fun ShiftFields(vm: WorkerFormViewModel, form: WorkerFormViewModel.FormState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            FormField("Shift start (HH:mm)", form.startTime) { v -> vm.update { it.copy(startTime = v) } }
        }
        Column(Modifier.weight(1f)) {
            FormField("Shift end (HH:mm)", form.endTime) { v -> vm.update { it.copy(endTime = v) } }
        }
    }
}

@Composable
private fun OvertimeFields(vm: WorkerFormViewModel, form: WorkerFormViewModel.FormState) {
    SwitchRow("Overtime", "Pay extra for time worked beyond the shift.", form.overtimeEnabled) { v ->
        vm.update { it.copy(overtimeEnabled = v) }
    }
    if (form.overtimeEnabled) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                FormField("OT rate (₹)", form.overtimeRate, KeyboardType.Decimal) { v -> vm.update { it.copy(overtimeRate = v) } }
            }
            Column(Modifier.weight(1f)) {
                FormDropdown("Per", form.overtimeType, listOf("hour", "minute")) { v -> vm.update { it.copy(overtimeType = v) } }
            }
        }
    }
}

@Composable
private fun LatePolicyFields(vm: WorkerFormViewModel, form: WorkerFormViewModel.FormState) {
    SwitchRow("Late policy", "Deduct pay for late check-ins after grace time.", form.latePolicyEnabled) { v ->
        vm.update { it.copy(latePolicyEnabled = v) }
    }
    if (form.latePolicyEnabled) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                FormField("Deduction (₹)", form.lateDeduction, KeyboardType.Decimal) { v -> vm.update { it.copy(lateDeduction = v) } }
            }
            Column(Modifier.weight(1f)) {
                FormDropdown("Per", form.lateDeductionType, listOf("day", "hour", "minute")) { v -> vm.update { it.copy(lateDeductionType = v) } }
            }
        }
        FormField("Grace minutes", form.lateGraceMinutes, KeyboardType.Number) { v -> vm.update { it.copy(lateGraceMinutes = v) } }
    }
}

// ── Small form building blocks ───────────────────────────────────────────────

@Composable
private fun FormCard(title: String, content: @Composable Column.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardBorder,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            content()
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = DividerColor,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    label: String,
    value: String,
    options: List<String>,
    display: (String) -> String = { it },
    onChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = display(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DividerColor,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(display(option)) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinDateField(value: LocalDate, onChange: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
        onValueChange = {},
        readOnly = true,
        label = { Text("Joining date", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().clickable { show = true },
        enabled = false,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = DividerColor,
            disabledTextColor = Navy,
            disabledLabelColor = TextSecondary,
        ),
    )
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Navy)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayTypeSelector(selected: String, onSelect: (String) -> Unit) {
    val labels = mapOf(
        PayTypes.DAILY to "Daily",
        PayTypes.MONTHLY to "Monthly",
        PayTypes.HOURLY to "Hourly",
        PayTypes.PROJECT to "Project",
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PayTypes.ALL.forEachIndexed { index, type ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PayTypes.ALL.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = PrimaryBlue,
                    activeContentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) { Text(labels[type] ?: type, fontSize = 12.sp) }
        }
    }
}

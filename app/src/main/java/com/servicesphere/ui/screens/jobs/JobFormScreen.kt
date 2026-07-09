package com.servicesphere.ui.screens.jobs

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.reminders.NotificationPermissionManager
import com.servicesphere.reminders.ReminderTypes
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    jobId: String?,
    preselectedClientId: String?,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: JobFormViewModel = viewModel(
        factory = JobFormViewModel.Factory(
            ServiceLocator.jobRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobReminderRepository,
            ServiceLocator.reminderScheduler,
            ServiceLocator.preferences,
            ServiceLocator.activationTracker
        )
    )
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    var showClientDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onNotificationPermissionHandled()
    }
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(preselectedClientId) {
        if (preselectedClientId != null) viewModel.onClientSelected(preselectedClientId)
    }
    LaunchedEffect(jobId) {
        if (jobId != null) viewModel.loadJob(jobId)
    }
    LaunchedEffect(uiState.saveSuccess, uiState.id, uiState.shouldRequestNotificationPermission) {
        val savedId = uiState.id
        if (uiState.saveSuccess && savedId != null) {
            if (
                uiState.shouldRequestNotificationPermission &&
                NotificationPermissionManager.needsRuntimePermission() &&
                !NotificationPermissionManager.hasNotificationPermission(context)
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect
            }
            if (uiState.shouldRequestNotificationPermission) {
                viewModel.onNotificationPermissionHandled()
            }
            viewModel.resetSaveSuccess()
            onSaved(savedId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { hideKeyboardAndClearFocus() })
            },
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = if (jobId == null) "New Job" else "Edit Job",
                subtitle = "Plan the work, schedule the visit, and keep field notes together."
            )
        }
        uiState.errorMessage?.let { message ->
            item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } }
        }
        if (uiState.jobNotFound) {
            item {
                EmptyState("Job not found", "This local job record is missing or was deleted.", actionLabel = "Back", onAction = onCancel)
            }
        } else if (uiState.isLoading) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading job", color = ServiceSphereTextSecondary)
                }
            }
        } else {
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton(
                            label = "Client: ${uiState.selectedClientName}",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                hideKeyboardAndClearFocus()
                                showClientDialog = true
                            }
                        )
                        ServiceSphereTextField(
                            uiState.title,
                            viewModel::onTitleChanged,
                            "Job title",
                            isError = uiState.titleError != null,
                            supportingText = uiState.titleError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        ServiceSphereTextField(uiState.description, viewModel::onDescriptionChanged, "Description", minLines = 3, maxLines = 6)
                        ServiceSphereTextField(uiState.address, viewModel::onAddressChanged, "Service address", minLines = 2, maxLines = 4)
                        SchedulePickerSection(
                            uiState = uiState,
                            onDateClick = {
                                hideKeyboardAndClearFocus()
                                showDatePicker = true
                            },
                            onTimeClick = {
                                hideKeyboardAndClearFocus()
                                showTimePicker = true
                            },
                            onClearSchedule = viewModel::clearSchedule
                        )
                        OutlinedButton(
                            enabled = uiState.scheduledAt != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                hideKeyboardAndClearFocus()
                                showReminderDialog = true
                            }
                        ) {
                            Text("Reminder: ${ReminderTypes.label(uiState.reminderType)}")
                        }
                        if (uiState.scheduledAt == null) {
                            Text("Add a schedule to enable reminders.", color = ServiceSphereTextSecondary)
                        }
                        uiState.reminderError?.let { Text(it, color = ServiceSphereDanger) }
                        ServiceSphereOutlinedButton(
                            label = "Status: ${uiState.status.toDisplayStatus()}",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                hideKeyboardAndClearFocus()
                                showStatusDialog = true
                            }
                        )
                        ServiceSphereTextField(
                            value = uiState.estimatedPrice,
                            onValueChange = viewModel::onEstimatedPriceChanged,
                            label = "Estimated price",
                            isError = uiState.estimatedPriceError != null,
                            supportingText = uiState.estimatedPriceError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { hideKeyboardAndClearFocus() })
                        )
                        ServiceSphereTextField(uiState.internalNotes, viewModel::onInternalNotesChanged, "Internal notes", minLines = 3, maxLines = 6)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.errorMessage?.let { message ->
                        Text(message, color = ServiceSphereDanger, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton(
                            "Cancel",
                            Modifier.weight(1f),
                            onClick = {
                                hideKeyboardAndClearFocus()
                                onCancel()
                            }
                        )
                        ServiceSphereButton(
                            if (uiState.isEditing) "Save Changes" else "Save Job",
                            Modifier.weight(1f),
                            enabled = !uiState.isSaving,
                            onClick = {
                                hideKeyboardAndClearFocus()
                                viewModel.saveJob()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDateMillis ?: todayPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onScheduledDatePicked)
                        showDatePicker = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val fallbackTime = remember { roundedCurrentTime() }
        val timePickerState = androidx.compose.material3.rememberTimePickerState(
            initialHour = uiState.selectedHour ?: fallbackTime.hour,
            initialMinute = uiState.selectedMinute ?: fallbackTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Scheduled time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onScheduledTimePicked(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showClientDialog) {
        ClientSelectorDialog(
            clients = uiState.availableClients,
            onDismiss = { showClientDialog = false },
            onSelect = {
                viewModel.onClientSelected(it)
                showClientDialog = false
            }
        )
    }
    if (showStatusDialog) {
        StatusSelectorDialog(
            selectedStatus = uiState.status,
            onDismiss = { showStatusDialog = false },
            onSelect = {
                viewModel.onStatusChanged(it)
                showStatusDialog = false
            }
        )
    }
    if (showReminderDialog) {
        ReminderSelectorDialog(
            selectedType = uiState.reminderType,
            onDismiss = { showReminderDialog = false },
            onSelect = {
                viewModel.onReminderTypeChanged(it)
                showReminderDialog = false
            }
        )
    }
}

@Composable
private fun SchedulePickerSection(
    uiState: JobFormUiState,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onClearSchedule: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SchedulePickerField(
                label = "Scheduled date",
                value = uiState.scheduledDateDisplay.ifBlank { "Select date" },
                icon = Icons.Filled.CalendarMonth,
                modifier = Modifier.weight(1f),
                onClick = onDateClick
            )
            SchedulePickerField(
                label = "Scheduled time",
                value = uiState.scheduledTimeDisplay.ifBlank { "Select time" },
                icon = Icons.Filled.AccessTime,
                modifier = Modifier.weight(1f),
                onClick = onTimeClick
            )
        }
        uiState.scheduleError?.let { Text(it, color = ServiceSphereDanger) }
        if (uiState.scheduledAt != null) {
            TextButton(onClick = onClearSchedule) {
                Text("Clear schedule")
            }
        }
    }
}

@Composable
private fun SchedulePickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = ServiceSphereTextSecondary)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun todayPickerMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun roundedCurrentTime(): LocalTime {
    val now = LocalTime.now()
    val roundedMinutes = (((now.hour * 60) + now.minute + 7) / 15) * 15
    return LocalTime.of((roundedMinutes / 60) % 24, roundedMinutes % 60)
}

@Composable
fun ReminderSelectorDialog(selectedType: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderTypes.formOptions.forEach { type ->
                    TextButton(onClick = { onSelect(type) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ReminderTypes.label(type), style = MaterialTheme.typography.bodyLarge)
                            if (type == selectedType) Text("Selected", color = ServiceSphereTextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ClientSelectorDialog(clients: List<ClientPickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSelect(null) }) { Text("No client selected") }
                clients.forEach { client ->
                    TextButton(onClick = { onSelect(client.id) }) {
                        Text(client.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StatusSelectorDialog(selectedStatus: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                jobStatuses.forEach { status ->
                    TextButton(onClick = { onSelect(status) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(status.toDisplayStatus(), toneForJob(status))
                            if (status == selectedStatus) Text("Selected", color = ServiceSphereTextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

val jobStatuses = listOf(
    JobStatus.NEW,
    JobStatus.SCHEDULED,
    JobStatus.IN_PROGRESS,
    JobStatus.COMPLETED,
    JobStatus.INVOICED,
    JobStatus.PAID,
    JobStatus.CANCELLED
)

package com.servicesphere.ui.screens.jobs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.JobCard
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereSearchBar
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun JobsScreen(
    initialViewMode: JobsViewMode = JobsViewMode.LIST,
    onAddJob: () -> Unit,
    onJobClick: (String) -> Unit,
    viewModel: JobsViewModel = viewModel(
        factory = JobsViewModel.Factory(ServiceLocator.jobRepository, ServiceLocator.clientRepository, ServiceLocator.jobReminderRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<JobUiModel?>(null) }

    LaunchedEffect(initialViewMode) {
        viewModel.onViewModeChanged(initialViewMode)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Jobs", "Track work from new request through paid invoice.") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JobsViewMode.entries.forEach { mode ->
                        Box(Modifier.weight(1f).clickable { viewModel.onViewModeChanged(mode) }) {
                            StatusChip(mode.label, if (uiState.viewMode == mode) StatusTone.Primary else StatusTone.Neutral)
                        }
                    }
                }
            }
            if (uiState.viewMode == JobsViewMode.CALENDAR) {
                item {
                    CalendarAgenda(
                        uiState = uiState,
                        onRangeChanged = viewModel::onRangeChanged,
                        onPrevious = viewModel::goPrevious,
                        onNext = viewModel::goNext,
                        onToday = viewModel::goToToday,
                        onJobClick = onJobClick,
                        onAddJob = onAddJob
                    )
                }
            } else {
            item { ServiceSphereSearchBar(value = uiState.searchQuery, onValueChange = viewModel::onSearchQueryChanged, placeholder = "Search jobs") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JobFilter.entries.forEach { filter ->
                        Box(Modifier.clickable { viewModel.onFilterChanged(filter) }) {
                            StatusChip(filter.label, if (uiState.selectedFilter == filter) StatusTone.Primary else StatusTone.Neutral)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${uiState.jobs.size} jobs", style = MaterialTheme.typography.labelLarge, color = ServiceSphereTextSecondary)
                    StatusChip(uiState.selectedFilter.label, StatusTone.Info)
                }
            }
            uiState.errorMessage?.let { message ->
                item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } }
            }
            when {
                uiState.isLoading -> item { LoadingJobs() }
                uiState.jobs.isEmpty() && uiState.searchQuery.isBlank() && uiState.selectedFilter == JobFilter.ALL -> item {
                    EmptyState(
                        title = "No jobs yet",
                        message = "Jobs help you track scheduled work, status, notes, photos, and signatures.",
                        icon = Icons.Filled.HomeRepairService,
                        actionLabel = "Create Job",
                        onAction = onAddJob
                    )
                }
                uiState.jobs.isEmpty() -> item {
                    EmptyState(
                        title = "No matching jobs",
                        message = "Try adjusting your search or filters.",
                        icon = Icons.Filled.HomeRepairService
                    )
                }
                else -> items(uiState.jobs, key = { it.id }) { job ->
                    Box(Modifier.clickable { onJobClick(job.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JobCard(
                                title = job.title,
                                client = job.clientName ?: "No client linked",
                                status = job.displayStatus,
                                schedule = job.displaySchedule ?: job.address,
                                price = job.displayPrice,
                                tone = toneForJob(job.status)
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { pendingDelete = job }) {
                                    Text("Delete", color = ServiceSphereDanger, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        FloatingActionButton(
            onClick = onAddJob,
            containerColor = ServiceSpherePrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Job")
        }
    }

    pendingDelete?.let { job ->
        DeleteJobDialog(
            onDismiss = { pendingDelete = null },
            onDelete = {
                viewModel.deleteJob(job.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun CalendarAgenda(
    uiState: JobsUiState,
    onRangeChanged: (CalendarRange) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onJobClick: (String) -> Unit,
    onAddJob: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ServiceSphereCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(uiState.calendarTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = ServiceSpherePrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServiceSphereOutlinedButton("Prev", Modifier.weight(1f), onClick = onPrevious)
                    ServiceSphereOutlinedButton("Today", Modifier.weight(1f), onClick = onToday)
                    ServiceSphereOutlinedButton("Next", Modifier.weight(1f), onClick = onNext)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalendarRange.entries.forEach { range ->
                        Box(Modifier.clickable { onRangeChanged(range) }) {
                            StatusChip(range.label, if (uiState.selectedRange == range) StatusTone.Primary else StatusTone.Neutral)
                        }
                    }
                }
            }
        }
        if (uiState.calendarJobs.isEmpty()) {
            val message = when (uiState.selectedRange) {
                CalendarRange.TODAY -> "No jobs scheduled today"
                CalendarRange.WEEK -> "No jobs scheduled this week"
                CalendarRange.MONTH -> "No jobs scheduled this month"
            }
            EmptyState(
                title = message,
                message = "Scheduled jobs will appear in this agenda view.",
                icon = Icons.Filled.CalendarMonth,
                actionLabel = "Create Job",
                onAction = onAddJob
            )
        } else {
            uiState.groupedCalendarJobs.forEach { (date, jobs) ->
                Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                jobs.forEach { job ->
                    ServiceSphereCard(onClick = { onJobClick(job.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(job.scheduledAt?.let(::formatTime).orEmpty(), fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (job.hasReminder) Icon(Icons.Filled.Notifications, contentDescription = "Reminder enabled", tint = ServiceSpherePrimary)
                                    StatusChip(job.displayStatus, toneForJob(job.status))
                                }
                            }
                            Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(job.clientName ?: "No client linked", color = ServiceSphereTextSecondary)
                            job.address?.takeIf { it.isNotBlank() }?.let { Text(it, color = ServiceSphereTextSecondary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingJobs() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading jobs", modifier = Modifier.padding(top = 12.dp), color = ServiceSphereTextSecondary)
    }
}

@Composable
fun DeleteJobDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete job?") },
        text = { Text("This will remove the job from your local records. Linked quotes or invoices may remain but will no longer show this job.") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = ServiceSphereDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun toneForJob(status: String): StatusTone = when (status) {
    "COMPLETED", "PAID" -> StatusTone.Success
    "SCHEDULED", "IN_PROGRESS" -> StatusTone.Info
    "INVOICED" -> StatusTone.Warning
    "CANCELLED" -> StatusTone.Danger
    else -> StatusTone.Neutral
}

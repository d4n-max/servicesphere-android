package com.servicesphere.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.domain.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class JobFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    UNPAID("Unpaid"),
    CANCELLED("Cancelled")
}

enum class JobsViewMode(val label: String) {
    LIST("List"),
    CALENDAR("Calendar")
}

enum class CalendarRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month")
}

data class JobUiModel(
    val id: String,
    val clientId: String?,
    val clientName: String?,
    val clientPhone: String?,
    val clientEmail: String?,
    val title: String,
    val description: String?,
    val address: String?,
    val scheduledAt: Long?,
    val status: String,
    val estimatedPrice: Double?,
    val internalNotes: String?,
    val hasReminder: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayStatus: String get() = status.toDisplayStatus()
    val displaySchedule: String? get() = scheduledAt?.let(::formatDateTime)
    val displayPrice: String? get() = estimatedPrice?.let { NumberFormat.getCurrencyInstance().format(it) }
}

data class JobsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: JobFilter = JobFilter.ALL,
    val viewMode: JobsViewMode = JobsViewMode.LIST,
    val selectedRange: CalendarRange = CalendarRange.TODAY,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val jobs: List<JobUiModel> = emptyList(),
    val totalActiveJobs: Int = 0,
    val calendarJobs: List<JobUiModel> = emptyList(),
    val groupedCalendarJobs: Map<String, List<JobUiModel>> = emptyMap(),
    val calendarTitle: String = "Today",
    val errorMessage: String? = null,
    val isEmpty: Boolean = true
)

class JobsViewModel(
    private val jobRepository: JobRepository,
    clientRepository: ClientRepository,
    reminderRepository: JobReminderRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(JobFilter.ALL)
    private val viewMode = MutableStateFlow(JobsViewMode.LIST)
    private val selectedRange = MutableStateFlow(CalendarRange.TODAY)
    private val selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    private val errorMessage = MutableStateFlow<String?>(null)
    private val zoneId = ZoneId.systemDefault()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<JobsUiState> = combine(
        jobRepository.observeJobs(),
        clientRepository.observeClients(),
        reminderRepository.observeAllReminders(),
        searchQuery,
        selectedFilter,
        viewMode,
        selectedRange,
        selectedDateMillis,
        errorMessage
    ) { values ->
        val jobs = values[0] as List<JobEntity>
        val clients = values[1] as List<ClientEntity>
        val reminders = values[2] as List<com.servicesphere.data.local.JobReminderEntity>
        val query = values[3] as String
        val filter = values[4] as JobFilter
        val mode = values[5] as JobsViewMode
        val range = values[6] as CalendarRange
        val selectedDate = values[7] as Long
        val error = values[8] as String?
        val clientMap = clients.associateBy { it.id }
        val reminderJobIds = reminders.filter { it.isEnabled && !it.hasFired }.map { it.jobId }.toSet()
        val filtered = jobs
            .filter { it.matchesQuery(query) }
            .filter { it.matchesFilter(filter) }
            .map { it.toUiModel(clientMap[it.clientId], it.id in reminderJobIds) }
        val totalActiveJobs = jobs.count { it.status != JobStatus.CANCELLED }
        val calendarJobs = jobs
            .filter { it.scheduledAt != null }
            .filter { it.matchesCalendarRange(range, selectedDate) }
            .sortedBy { it.scheduledAt }
            .map { it.toUiModel(clientMap[it.clientId], it.id in reminderJobIds) }
        val grouped = calendarJobs.groupBy { job ->
            job.scheduledAt?.let { formatCalendarGroup(it) } ?: "Unscheduled"
        }
        JobsUiState(
            isLoading = false,
            searchQuery = query,
            selectedFilter = filter,
            viewMode = mode,
            selectedRange = range,
            selectedDateMillis = selectedDate,
            jobs = filtered,
            totalActiveJobs = totalActiveJobs,
            calendarJobs = calendarJobs,
            groupedCalendarJobs = grouped,
            calendarTitle = calendarTitle(range, selectedDate),
            errorMessage = error,
            isEmpty = filtered.isEmpty()
        )
    }
        .catch { error ->
            emit(JobsUiState(isLoading = false, errorMessage = error.message ?: "Unable to load jobs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobsUiState())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterChanged(filter: JobFilter) {
        selectedFilter.value = filter
    }

    fun onViewModeChanged(mode: JobsViewMode) {
        viewMode.value = mode
    }

    fun onRangeChanged(range: CalendarRange) {
        selectedRange.value = range
    }

    fun goToToday() {
        selectedDateMillis.value = System.currentTimeMillis()
    }

    fun goPrevious() {
        selectedDateMillis.value = shiftSelectedDate(-1)
    }

    fun goNext() {
        selectedDateMillis.value = shiftSelectedDate(1)
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            runCatching {
                jobRepository.getJobByIdOnce(jobId)?.let { jobRepository.deleteJob(it) }
                com.servicesphere.data.ServiceLocator.jobReminderRepository.getFirstReminderForJobOnce(jobId)?.let {
                    com.servicesphere.data.ServiceLocator.reminderScheduler.cancel(it.id)
                }
                com.servicesphere.data.ServiceLocator.jobReminderRepository.deleteRemindersForJob(jobId)
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to delete job"
            }
        }
    }

    fun clearError() {
        errorMessage.update { null }
    }

    private fun JobEntity.matchesQuery(query: String): Boolean {
        val value = query.trim()
        if (value.isBlank()) return true
        return listOf(title, description, address, status.toDisplayStatus(), status)
            .filterNotNull()
            .any { it.contains(value, ignoreCase = true) }
    }

    private fun JobEntity.matchesFilter(filter: JobFilter): Boolean {
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zoneId)
        return when (filter) {
            JobFilter.ALL -> true
            JobFilter.TODAY -> scheduledAt?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() == today } == true
            JobFilter.UPCOMING -> scheduledAt != null && scheduledAt > now && status !in setOf(JobStatus.COMPLETED, JobStatus.CANCELLED, JobStatus.PAID)
            JobFilter.IN_PROGRESS -> status == JobStatus.IN_PROGRESS
            JobFilter.COMPLETED -> status == JobStatus.COMPLETED
            JobFilter.UNPAID -> status in setOf(JobStatus.COMPLETED, JobStatus.INVOICED)
            JobFilter.CANCELLED -> status == JobStatus.CANCELLED
        }
    }

    private fun JobEntity.matchesCalendarRange(range: CalendarRange, selectedDateMillis: Long): Boolean {
        val scheduled = scheduledAt ?: return false
        val selected = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalDate()
        val jobDate = Instant.ofEpochMilli(scheduled).atZone(zoneId).toLocalDate()
        return when (range) {
            CalendarRange.TODAY -> jobDate == selected
            CalendarRange.WEEK -> {
                val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
                val end = start.plusDays(6)
                !jobDate.isBefore(start) && !jobDate.isAfter(end)
            }
            CalendarRange.MONTH -> YearMonth.from(jobDate) == YearMonth.from(selected)
        }
    }

    private fun shiftSelectedDate(direction: Long): Long {
        val date = Instant.ofEpochMilli(selectedDateMillis.value).atZone(zoneId).toLocalDate()
        val shifted = when (selectedRange.value) {
            CalendarRange.TODAY -> date.plusDays(direction)
            CalendarRange.WEEK -> date.plusWeeks(direction)
            CalendarRange.MONTH -> date.plusMonths(direction)
        }
        return shifted.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    class Factory(
        private val jobRepository: JobRepository,
        private val clientRepository: ClientRepository,
        private val reminderRepository: JobReminderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = JobsViewModel(jobRepository, clientRepository, reminderRepository) as T
    }
}

fun JobEntity.toUiModel(client: ClientEntity?, hasReminder: Boolean = false): JobUiModel = JobUiModel(
    id = id,
    clientId = clientId,
    clientName = client?.name,
    clientPhone = client?.phone,
    clientEmail = client?.email,
    title = title,
    description = description,
    address = address,
    scheduledAt = scheduledAt,
    status = status,
    estimatedPrice = estimatedPrice,
    internalNotes = internalNotes,
    hasReminder = hasReminder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun String.toDisplayStatus(): String =
    lowercase(Locale.US).split("_").joinToString(" ") { part ->
        part.replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

fun formatDateTime(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

fun formatDate(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

fun formatTime(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a"))

private fun formatCalendarGroup(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

private fun calendarTitle(range: CalendarRange, selectedDateMillis: Long): String {
    val date = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (range) {
        CalendarRange.TODAY -> if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        CalendarRange.WEEK -> "Week of ${date.minusDays((date.dayOfWeek.value - 1).toLong()).format(DateTimeFormatter.ofPattern("MMM d"))}"
        CalendarRange.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}

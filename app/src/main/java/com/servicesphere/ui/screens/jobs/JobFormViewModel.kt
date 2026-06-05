package com.servicesphere.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.servicesphere.BuildConfig
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.preferences.UserPreferences
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.reminders.JobReminderScheduler
import com.servicesphere.reminders.ReminderTimeCalculator
import com.servicesphere.reminders.ReminderTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class ClientPickerUiModel(
    val id: String,
    val name: String,
    val address: String?
)

data class JobFormUiState(
    val id: String? = null,
    val clientId: String? = null,
    val availableClients: List<ClientPickerUiModel> = emptyList(),
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val scheduledDateText: String = "",
    val scheduledTimeText: String = "",
    val scheduledAt: Long? = null,
    val status: String = JobStatus.NEW,
    val estimatedPrice: String = "",
    val internalNotes: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val titleError: String? = null,
    val estimatedPriceError: String? = null,
    val scheduleError: String? = null,
    val reminderType: String = ReminderTypes.NONE,
    val reminderEnabled: Boolean = false,
    val reminderError: String? = null,
    val shouldRequestNotificationPermission: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val jobNotFound: Boolean = false
) {
    val selectedClientName: String
        get() = availableClients.firstOrNull { it.id == clientId }?.name ?: "No client selected"
}

class JobFormViewModel(
    private val jobRepository: JobRepository,
    private val clientRepository: ClientRepository,
    private val reminderRepository: JobReminderRepository,
    private val reminderScheduler: JobReminderScheduler,
    private val preferences: UserPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobFormUiState())
    val uiState: StateFlow<JobFormUiState> = _uiState.asStateFlow()
    private val zoneId = ZoneId.systemDefault()
    private var defaultReminderType: String = ReminderTypes.NONE
    private var userChangedReminder = false

    init {
        loadClients()
        loadDefaultReminder()
    }

    private fun loadDefaultReminder() {
        viewModelScope.launch {
            defaultReminderType = preferences.defaultJobReminderType.first()
        }
    }

    fun loadClients(preselectedClientId: String? = null) {
        viewModelScope.launch {
            runCatching { clientRepository.observeClients() }.onSuccess { flow ->
                flow.collect { clients ->
                    val pickerClients = clients.map { it.toPicker() }
                    _uiState.update { state ->
                        val nextClientId = state.clientId ?: preselectedClientId
                        val selectedClient = pickerClients.firstOrNull { it.id == nextClientId }
                        state.copy(
                            availableClients = pickerClients,
                            clientId = nextClientId,
                            address = if (state.address.isBlank()) selectedClient?.address.orEmpty() else state.address
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to load clients") }
            }
        }
    }

    fun loadJob(jobId: String) {
        if (_uiState.value.id == jobId && _uiState.value.isEditing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditing = true, errorMessage = null) }
            runCatching { jobRepository.getJobByIdOnce(jobId) }
                .onSuccess { job ->
                    if (job == null) {
                        _uiState.update { it.copy(isLoading = false, jobNotFound = true, errorMessage = "Job not found") }
                    } else {
                        val dateTime = job.scheduledAt?.let { millis ->
                            val zoned = java.time.Instant.ofEpochMilli(millis).atZone(zoneId)
                            zoned.toLocalDate().toString() to zoned.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                        }
                        _uiState.update {
                            val reminder = reminderRepository.getFirstReminderForJobOnce(job.id)
                            it.copy(
                                id = job.id,
                                clientId = job.clientId,
                                title = job.title,
                                description = job.description.orEmpty(),
                                address = job.address.orEmpty(),
                                scheduledDateText = dateTime?.first.orEmpty(),
                                scheduledTimeText = dateTime?.second.orEmpty(),
                                scheduledAt = job.scheduledAt,
                                status = job.status,
                                estimatedPrice = job.estimatedPrice?.toString().orEmpty(),
                                internalNotes = job.internalNotes.orEmpty(),
                                reminderType = reminder?.takeIf { existing -> existing.isEnabled }?.reminderType ?: ReminderTypes.NONE,
                                reminderEnabled = reminder?.isEnabled == true,
                                isEditing = true,
                                isLoading = false,
                                jobNotFound = false
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load job") }
                }
        }
    }

    fun onClientSelected(clientId: String?) {
        _uiState.update { state ->
            val client = state.availableClients.firstOrNull { it.id == clientId }
            state.copy(
                clientId = clientId,
                address = if (state.address.isBlank()) client?.address.orEmpty() else state.address
            )
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, titleError = null) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onAddressChanged(value: String) = _uiState.update { it.copy(address = value) }
    fun onScheduledDateChanged(value: String) {
        _uiState.update { it.copy(scheduledDateText = value, scheduleError = null) }
        applyDefaultReminderIfReady()
    }
    fun onScheduledTimeChanged(value: String) {
        _uiState.update { it.copy(scheduledTimeText = value, scheduleError = null) }
        applyDefaultReminderIfReady()
    }
    fun onStatusChanged(value: String) = _uiState.update { it.copy(status = value) }
    fun onEstimatedPriceChanged(value: String) = _uiState.update { it.copy(estimatedPrice = value, estimatedPriceError = null) }
    fun onInternalNotesChanged(value: String) = _uiState.update { it.copy(internalNotes = value) }
    fun onReminderTypeChanged(value: String) {
        userChangedReminder = true
        _uiState.update { it.copy(reminderType = value, reminderError = null) }
    }

    fun saveJob() {
        val current = _uiState.value
        logSave("Save Job tapped. editing=${current.isEditing}, reminder=${current.reminderType}, hasDate=${current.scheduledDateText.isNotBlank()}, hasTime=${current.scheduledTimeText.isNotBlank()}, hasPrice=${current.estimatedPrice.isNotBlank()}")
        val titleError = if (current.title.trim().isBlank()) "Job title is required" else null
        val price = current.estimatedPrice.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val priceError = when {
            current.estimatedPrice.trim().isBlank() -> null
            price == null || price < 0.0 -> "Enter a valid price"
            else -> null
        }
        val scheduleResult = parseSchedule(current.scheduledDateText.trim(), current.scheduledTimeText.trim())
        val parsedSchedule = scheduleResult.getOrNull()
        val scheduleError = scheduleResult.exceptionOrNull()?.message
        val reminderError = when {
            current.reminderType == ReminderTypes.NONE -> null
            parsedSchedule == null -> "Add a schedule to enable reminders"
            ReminderTimeCalculator.calculate(parsedSchedule, current.reminderType) < System.currentTimeMillis() -> "Reminder time is in the past"
            else -> null
        }
        if (titleError != null || priceError != null || scheduleError != null || reminderError != null) {
            logSave("Save Job validation failed. title=${titleError != null}, schedule=${scheduleError != null}, reminder=${reminderError != null}, price=${priceError != null}")
            _uiState.update {
                it.copy(
                    isSaving = false,
                    titleError = titleError,
                    estimatedPriceError = priceError,
                    scheduleError = scheduleError,
                    reminderError = reminderError,
                    errorMessage = "Fix the highlighted fields and try again."
                )
            }
            return
        }
        logSave("Save Job validation passed. scheduledAt=$parsedSchedule, reminder=${current.reminderType}")

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val entity = JobEntity(
                    id = if (current.isEditing) current.id ?: UUID.randomUUID().toString() else UUID.randomUUID().toString(),
                    clientId = current.clientId,
                    title = current.title.trim(),
                    description = current.description.trim().ifBlank { null },
                    address = current.address.trim().ifBlank { null },
                    scheduledAt = parsedSchedule,
                    status = current.status,
                    estimatedPrice = price,
                    internalNotes = current.internalNotes.trim().ifBlank { null },
                    createdAt = if (current.isEditing) jobRepository.getJobByIdOnce(current.id.orEmpty())?.createdAt ?: now else now,
                    updatedAt = now
                )
                val previousReminder = if (current.isEditing) reminderRepository.getFirstReminderForJobOnce(entity.id) else null
                if (current.isEditing) jobRepository.updateJob(entity) else jobRepository.insertJob(entity)
                handleReminderSave(entity, previousReminder?.id)
                logSave("Save Job repository write succeeded. editing=${current.isEditing}, reminder=${current.reminderType}")
                entity.id
            }.onSuccess { savedId ->
                val permissionNeeded = current.reminderType != ReminderTypes.NONE
                _uiState.update {
                    it.copy(
                        id = savedId,
                        isSaving = false,
                        saveSuccess = true,
                        shouldRequestNotificationPermission = permissionNeeded,
                        successMessage = if (current.reminderType == ReminderTypes.NONE) "Job saved" else "Reminder saved"
                    )
                }
            }.onFailure { error ->
                logSave("Save Job failed: ${error.message}")
                _uiState.update { it.copy(isSaving = false, errorMessage = "Couldn't save job. Please try again.") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccessMessage() = _uiState.update { it.copy(successMessage = null) }
    fun onNotificationPermissionHandled() = _uiState.update { it.copy(shouldRequestNotificationPermission = false) }
    fun resetSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    private suspend fun handleReminderSave(job: JobEntity, previousReminderId: String?) {
        val terminalStatus = job.status in setOf(JobStatus.COMPLETED, JobStatus.PAID, JobStatus.CANCELLED)
        if (terminalStatus && preferences.autoDisableCompletedJobReminders.first()) {
            previousReminderId?.let(reminderScheduler::cancel)
            reminderRepository.disableRemindersForJob(job.id)
            return
        }
        if (uiState.value.reminderType == ReminderTypes.NONE) {
            previousReminderId?.let(reminderScheduler::cancel)
            reminderRepository.deleteRemindersForJob(job.id)
            return
        }
        val scheduledAt = job.scheduledAt ?: error("Add a schedule to enable reminders")
        val reminder = reminderRepository.createOrUpdateReminderForJob(job.id, uiState.value.reminderType, scheduledAt)
        if (previousReminderId != null && previousReminderId != reminder.id) reminderScheduler.cancel(previousReminderId)
        reminderScheduler.schedule(reminder)
    }

    private fun parseSchedule(dateText: String, timeText: String): Result<Long?> {
        if (dateText.isBlank() && timeText.isBlank()) return Result.success(null)
        if (dateText.isBlank()) return Result.failure(IllegalArgumentException("Add a date or clear the scheduled time"))
        return try {
            val date = LocalDate.parse(dateText)
            val time = if (timeText.isBlank()) LocalTime.of(9, 0) else LocalTime.parse(timeText)
            Result.success(date.atTime(time).atZone(zoneId).toInstant().toEpochMilli())
        } catch (_: DateTimeParseException) {
            Result.failure(IllegalArgumentException("Use date YYYY-MM-DD and time HH:MM"))
        }
    }

    private fun applyDefaultReminderIfReady() {
        val current = _uiState.value
        if (userChangedReminder || defaultReminderType == ReminderTypes.NONE || current.reminderType != ReminderTypes.NONE) return
        val hasValidSchedule = parseSchedule(current.scheduledDateText.trim(), current.scheduledTimeText.trim()).getOrNull() != null
        if (hasValidSchedule) {
            _uiState.update { it.copy(reminderType = defaultReminderType) }
        }
    }

    class Factory(
        private val jobRepository: JobRepository,
        private val clientRepository: ClientRepository,
        private val reminderRepository: JobReminderRepository,
        private val reminderScheduler: JobReminderScheduler,
        private val preferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            JobFormViewModel(jobRepository, clientRepository, reminderRepository, reminderScheduler, preferences) as T
    }
}

private fun ClientEntity.toPicker(): ClientPickerUiModel = ClientPickerUiModel(id = id, name = name, address = address)

private const val TAG = "JobFormViewModel"

private fun logSave(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

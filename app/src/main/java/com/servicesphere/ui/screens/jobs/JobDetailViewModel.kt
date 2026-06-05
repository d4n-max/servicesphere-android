package com.servicesphere.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.reminders.JobReminderScheduler
import com.servicesphere.reminders.ReminderTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobDetailUiState(
    val isLoading: Boolean = true,
    val job: JobUiModel? = null,
    val reminder: JobReminderUiModel? = null,
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class JobDetailViewModel(
    private val jobId: String,
    private val jobRepository: JobRepository,
    clientRepository: ClientRepository,
    private val reminderRepository: JobReminderRepository,
    private val reminderScheduler: JobReminderScheduler
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)

    val uiState: StateFlow<JobDetailUiState> = combine(
        jobRepository.observeJobById(jobId),
        clientRepository.observeClients(),
        reminderRepository.observeRemindersForJob(jobId),
        errorMessage,
        deleteSuccess
    ) { job, clients, reminders, error, deleted ->
        JobDetailUiState(
            isLoading = false,
            job = job?.toUiModel(clients.firstOrNull { it.id == job.clientId }),
            reminder = reminders.firstOrNull { it.isEnabled }?.let {
                JobReminderUiModel(
                    id = it.id,
                    type = it.reminderType,
                    label = ReminderTypes.label(it.reminderType),
                    reminderTimeMillis = it.reminderTimeMillis,
                    hasFired = it.hasFired
                )
            },
            errorMessage = error,
            deleteSuccess = deleted
        )
    }
        .catch { error -> emit(JobDetailUiState(isLoading = false, errorMessage = error.message ?: "Unable to load job")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobDetailUiState())

    fun updateStatus(status: String) {
        viewModelScope.launch {
            runCatching {
                val job = jobRepository.getJobByIdOnce(jobId) ?: return@runCatching
                jobRepository.updateJob(job.copy(status = status, updatedAt = System.currentTimeMillis()))
                if (status in setOf(JobStatus.COMPLETED, JobStatus.PAID, JobStatus.CANCELLED)) {
                    reminderRepository.getFirstReminderForJobOnce(jobId)?.let { reminderScheduler.cancel(it.id) }
                    reminderRepository.disableRemindersForJob(jobId)
                }
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to update status"
            }
        }
    }

    fun deleteJob() {
        viewModelScope.launch {
            runCatching {
                jobRepository.getJobByIdOnce(jobId)?.let { jobRepository.deleteJob(it) }
                reminderRepository.getFirstReminderForJobOnce(jobId)?.let { reminderScheduler.cancel(it.id) }
                reminderRepository.deleteRemindersForJob(jobId)
            }.onSuccess {
                deleteSuccess.value = true
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to delete job"
            }
        }
    }

    fun clearError() = errorMessage.update { null }

    fun disableReminder() {
        viewModelScope.launch {
            runCatching {
                reminderRepository.getFirstReminderForJobOnce(jobId)?.let {
                    reminderScheduler.cancel(it.id)
                    reminderRepository.deleteReminder(it)
                }
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to disable reminder"
            }
        }
    }

    class Factory(
        private val jobId: String,
        private val jobRepository: JobRepository,
        private val clientRepository: ClientRepository,
        private val reminderRepository: JobReminderRepository,
        private val reminderScheduler: JobReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            JobDetailViewModel(jobId, jobRepository, clientRepository, reminderRepository, reminderScheduler) as T
    }
}

data class JobReminderUiModel(
    val id: String,
    val type: String,
    val label: String,
    val reminderTimeMillis: Long,
    val hasFired: Boolean
) {
    val displayTime: String get() = formatDateTime(reminderTimeMillis)
}

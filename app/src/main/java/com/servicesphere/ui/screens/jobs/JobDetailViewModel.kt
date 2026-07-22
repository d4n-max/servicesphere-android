package com.servicesphere.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.WorkflowRepository
import com.servicesphere.data.repository.ConversionResult
import com.servicesphere.analytics.AnalyticsTracker
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.reminders.JobReminderScheduler
import com.servicesphere.reminders.ReminderTypes
import com.servicesphere.ui.timeline.ActivityTimelineItem
import com.servicesphere.ui.timeline.buildTimeline
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val sourceQuoteId: String? = null,
    val linkedInvoiceId: String? = null,
    val timeline: List<ActivityTimelineItem> = emptyList(),
    val isConverting: Boolean = false,
    val convertedInvoiceId: String? = null,
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class JobDetailViewModel(
    private val jobId: String,
    private val jobRepository: JobRepository,
    clientRepository: ClientRepository,
    private val reminderRepository: JobReminderRepository,
    private val reminderScheduler: JobReminderScheduler,
    private val workflowRepository: WorkflowRepository,
    quoteRepository: QuoteRepository,
    invoiceRepository: InvoiceRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)
    private val isConverting = MutableStateFlow(false)
    private val convertedInvoiceId = MutableStateFlow<String?>(null)
    private val _statusUpdateEvents = MutableSharedFlow<String>()
    val statusUpdateEvents: SharedFlow<String> = _statusUpdateEvents.asSharedFlow()

    private val detailRows = combine(
        jobRepository.observeJobById(jobId),
        clientRepository.observeClients(),
        reminderRepository.observeRemindersForJob(jobId),
        quoteRepository.observeQuotes(),
        invoiceRepository.observeInvoices()
    ) { job, clients, reminders, quotes, invoices -> DetailRows(job, clients, reminders, quotes, invoices) }

    val uiState: StateFlow<JobDetailUiState> = combine(
        detailRows,
        errorMessage,
        deleteSuccess,
        isConverting,
        convertedInvoiceId
    ) { rows, error, deleted, converting, invoiceId ->
        val (job, clients, reminders, quotes, invoices) = rows
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
            sourceQuoteId = job?.sourceQuoteId,
            linkedInvoiceId = invoices.firstOrNull { it.jobId == jobId }?.id,
            timeline = job?.let { buildTimeline(quotes.filter { quote -> quote.id == it.sourceQuoteId || quote.jobId == it.id }, listOf(it), invoices.filter { invoice -> invoice.jobId == it.id }) }.orEmpty(),
            isConverting = converting,
            convertedInvoiceId = invoiceId,
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
                _statusUpdateEvents.emit(status)
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
    fun clearConvertedInvoice() { convertedInvoiceId.value = null }

    fun createInvoice() {
        if (isConverting.value) return
        viewModelScope.launch {
            isConverting.value = true
            analyticsTracker.workflowConversion(AnalyticsTracker.Events.JOB_TO_INVOICE_STARTED, "job_detail")
            when (val result = workflowRepository.createInvoiceFromJob(jobId)) {
                is ConversionResult.Created -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.JOB_TO_INVOICE_COMPLETED, "job_detail", "created"); convertedInvoiceId.value = result.value.id }
                is ConversionResult.Existing -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.JOB_TO_INVOICE_COMPLETED, "job_detail", "existing"); convertedInvoiceId.value = result.value.id }
                ConversionResult.SourceNotFound -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.JOB_TO_INVOICE_FAILED, "job_detail", "not_found"); errorMessage.value = "Job not found" }
                is ConversionResult.Failure -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.JOB_TO_INVOICE_FAILED, "job_detail", "failure"); errorMessage.value = result.message }
            }
            isConverting.value = false
        }
    }

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
        private val reminderScheduler: JobReminderScheduler,
        private val workflowRepository: WorkflowRepository,
        private val quoteRepository: QuoteRepository,
        private val invoiceRepository: InvoiceRepository,
        private val analyticsTracker: AnalyticsTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            JobDetailViewModel(jobId, jobRepository, clientRepository, reminderRepository, reminderScheduler, workflowRepository, quoteRepository, invoiceRepository, analyticsTracker) as T
    }
}

private data class DetailRows(
    val job: com.servicesphere.data.local.JobEntity?,
    val clients: List<com.servicesphere.data.local.ClientEntity>,
    val reminders: List<com.servicesphere.data.local.JobReminderEntity>,
    val quotes: List<com.servicesphere.data.local.QuoteEntity>,
    val invoices: List<com.servicesphere.data.local.InvoiceEntity>
)

data class JobReminderUiModel(
    val id: String,
    val type: String,
    val label: String,
    val reminderTimeMillis: Long,
    val hasFired: Boolean
) {
    val displayTime: String get() = formatDateTime(reminderTimeMillis)
}

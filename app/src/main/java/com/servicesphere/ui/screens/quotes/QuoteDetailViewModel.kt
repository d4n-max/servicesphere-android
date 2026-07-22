package com.servicesphere.ui.screens.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.WorkflowRepository
import com.servicesphere.data.repository.ConversionResult
import com.servicesphere.analytics.AnalyticsTracker
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.LineItemParentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuoteLineItemUiModel(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

data class QuoteDetailUiState(
    val isLoading: Boolean = true,
    val quote: QuoteUiModel? = null,
    val lineItems: List<QuoteLineItemUiModel> = emptyList(),
    val linkedJobId: String? = null,
    val linkedInvoiceId: String? = null,
    val isCreatingJob: Boolean = false,
    val createdJobId: String? = null,
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class QuoteDetailViewModel(
    private val quoteId: String,
    private val quoteRepository: QuoteRepository,
    private val lineItemRepository: LineItemRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository,
    invoiceRepository: InvoiceRepository,
    private val workflowRepository: WorkflowRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)
    private val isCreatingJob = MutableStateFlow(false)
    private val createdJobId = MutableStateFlow<String?>(null)

    private val detailRows = combine(
        quoteRepository.observeQuoteById(quoteId),
        lineItemRepository.observeLineItems(quoteId, LineItemParentType.QUOTE),
        clientRepository.observeClients(),
        jobRepository.observeJobs(),
        invoiceRepository.observeInvoices()
    ) { quote, items, clients, jobs, invoices ->
        DetailRows(quote, items, clients, jobs, invoices)
    }

    val uiState: StateFlow<QuoteDetailUiState> = combine(
        detailRows,
        errorMessage,
        deleteSuccess,
        isCreatingJob,
        createdJobId
    ) { rows, error, deleted, creatingJob, jobId ->
        QuoteDetailUiState(
            isLoading = false,
            quote = rows.quote?.toUiModel(rows.clients.firstOrNull { it.id == rows.quote.clientId }, rows.jobs.firstOrNull { it.id == rows.quote.jobId }),
            lineItems = rows.items.map(LineItemEntity::toUi),
            linkedJobId = rows.jobs.firstOrNull { it.sourceQuoteId == quoteId }?.id,
            linkedInvoiceId = rows.invoices.firstOrNull { it.quoteId == quoteId }?.id,
            isCreatingJob = creatingJob,
            createdJobId = jobId,
            errorMessage = error,
            deleteSuccess = deleted
        )
    }
        .catch { error -> emit(QuoteDetailUiState(isLoading = false, errorMessage = error.message ?: "Unable to load quote")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuoteDetailUiState())

    fun updateStatus(status: String) {
        viewModelScope.launch {
            runCatching {
                val quote = quoteRepository.getQuoteByIdOnce(quoteId) ?: return@runCatching
                quoteRepository.updateQuote(quote.copy(status = status, updatedAt = System.currentTimeMillis()))
            }.onFailure { error -> errorMessage.value = error.message ?: "Unable to update quote status" }
        }
    }

    fun deleteQuote() {
        viewModelScope.launch {
            runCatching {
                quoteRepository.getQuoteByIdOnce(quoteId)?.let {
                    lineItemRepository.deleteLineItemsForParent(quoteId, LineItemParentType.QUOTE)
                    quoteRepository.deleteQuote(it)
                }
            }.onSuccess { deleteSuccess.value = true }
                .onFailure { error -> errorMessage.value = error.message ?: "Unable to delete quote" }
        }
    }

    fun clearError() = errorMessage.update { null }
    fun clearCreatedJob() { createdJobId.value = null }

    fun createJobFromQuote() {
        if (isCreatingJob.value) return
        viewModelScope.launch {
            isCreatingJob.value = true
            analyticsTracker.workflowConversion(AnalyticsTracker.Events.QUOTE_TO_JOB_STARTED, "quote_detail")
            when (val result = workflowRepository.createJobFromQuote(quoteId)) {
                is ConversionResult.Created -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.QUOTE_TO_JOB_COMPLETED, "quote_detail", "created"); createdJobId.value = result.value.id }
                is ConversionResult.Existing -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.QUOTE_TO_JOB_COMPLETED, "quote_detail", "existing"); createdJobId.value = result.value.id }
                ConversionResult.SourceNotFound -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.QUOTE_TO_JOB_FAILED, "quote_detail", "not_found"); errorMessage.value = "Quote not found" }
                is ConversionResult.Failure -> { analyticsTracker.workflowConversion(AnalyticsTracker.Events.QUOTE_TO_JOB_FAILED, "quote_detail", "failure"); errorMessage.value = result.message }
            }
            isCreatingJob.value = false
        }
    }

    class Factory(
        private val quoteId: String,
        private val quoteRepository: QuoteRepository,
        private val lineItemRepository: LineItemRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val invoiceRepository: InvoiceRepository,
        private val workflowRepository: WorkflowRepository,
        private val analyticsTracker: AnalyticsTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuoteDetailViewModel(quoteId, quoteRepository, lineItemRepository, clientRepository, jobRepository, invoiceRepository, workflowRepository, analyticsTracker) as T
    }
}

private fun LineItemEntity.toUi(): QuoteLineItemUiModel = QuoteLineItemUiModel(description, quantity, unitPrice, total)

private data class DetailRows(
    val quote: com.servicesphere.data.local.QuoteEntity?,
    val items: List<LineItemEntity>,
    val clients: List<com.servicesphere.data.local.ClientEntity>,
    val jobs: List<com.servicesphere.data.local.JobEntity>,
    val invoices: List<com.servicesphere.data.local.InvoiceEntity>
)

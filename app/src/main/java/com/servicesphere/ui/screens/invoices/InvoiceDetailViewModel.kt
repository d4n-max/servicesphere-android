package com.servicesphere.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.LineItemParentType
import com.servicesphere.domain.model.PaymentMethod
import com.servicesphere.domain.model.QuoteStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class InvoiceLineItemUiModel(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

data class InvoiceDetailUiState(
    val isLoading: Boolean = true,
    val invoice: InvoiceUiModel? = null,
    val lineItems: List<InvoiceLineItemUiModel> = emptyList(),
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

data class QuoteConversionUiState(
    val isConverting: Boolean = false,
    val convertedInvoiceId: String? = null,
    val errorMessage: String? = null
)

class InvoiceDetailViewModel(
    private val invoiceId: String,
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository,
    quoteRepository: QuoteRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)

    private val detailRows = combine(
        invoiceRepository.observeInvoiceById(invoiceId),
        lineItemRepository.observeLineItems(invoiceId, LineItemParentType.INVOICE),
        clientRepository.observeClients()
    ) { invoice, items, clients -> Triple(invoice, items, clients) }

    private val joinedRows = combine(detailRows, jobRepository.observeJobs(), quoteRepository.observeQuotes()) { rows, jobs, quotes ->
        InvoiceDetailRows(rows.first, rows.second, rows.third, jobs, quotes)
    }

    val uiState: StateFlow<InvoiceDetailUiState> = combine(
        joinedRows,
        errorMessage,
        deleteSuccess
    ) { rows, error, deleted ->
        val invoice = rows.invoice
        InvoiceDetailUiState(
            isLoading = false,
            invoice = invoice?.toUiModel(
                rows.clients.firstOrNull { it.id == invoice.clientId },
                rows.jobs.firstOrNull { it.id == invoice.jobId },
                rows.quotes.firstOrNull { it.id == invoice.quoteId }
            ),
            lineItems = rows.items.map { it.toUi() },
            errorMessage = error,
            deleteSuccess = deleted
        )
    }
        .catch { error -> emit(InvoiceDetailUiState(isLoading = false, errorMessage = error.message ?: "Unable to load invoice")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceDetailUiState())

    fun updateStatus(status: String, paymentMethod: String? = null) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val invoice = invoiceRepository.getInvoiceByIdOnce(invoiceId) ?: return@runCatching
                invoiceRepository.updateInvoice(
                    invoice.copy(
                        status = status,
                        paidDate = if (status == InvoiceStatus.PAID && invoice.paidDate == null) now else invoice.paidDate,
                        paymentMethod = if (status == InvoiceStatus.PAID && invoice.paymentMethod == null) paymentMethod ?: PaymentMethod.OTHER else invoice.paymentMethod,
                        updatedAt = now
                    )
                )
            }.onFailure { error -> errorMessage.value = error.message ?: "Unable to update invoice status" }
        }
    }

    fun markPaid(paymentMethod: String) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val invoice = invoiceRepository.getInvoiceByIdOnce(invoiceId) ?: return@runCatching
                invoiceRepository.updateInvoice(invoice.copy(status = InvoiceStatus.PAID, paidDate = now, paymentMethod = paymentMethod, updatedAt = now))
            }.onFailure { error -> errorMessage.value = error.message ?: "Unable to mark invoice paid" }
        }
    }

    fun deleteInvoice() {
        viewModelScope.launch {
            runCatching {
                invoiceRepository.getInvoiceByIdOnce(invoiceId)?.let {
                    lineItemRepository.deleteLineItemsForParent(invoiceId, LineItemParentType.INVOICE)
                    invoiceRepository.deleteInvoice(it)
                }
            }.onSuccess { deleteSuccess.value = true }
                .onFailure { error -> errorMessage.value = error.message ?: "Unable to delete invoice" }
        }
    }

    fun clearError() = errorMessage.update { null }

    class Factory(
        private val invoiceId: String,
        private val invoiceRepository: InvoiceRepository,
        private val lineItemRepository: LineItemRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InvoiceDetailViewModel(invoiceId, invoiceRepository, lineItemRepository, clientRepository, jobRepository, quoteRepository) as T
    }
}

class ConvertQuoteToInvoiceViewModel(
    private val quoteId: String,
    private val workflowRepository: com.servicesphere.data.repository.WorkflowRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuoteConversionUiState())
    val uiState: StateFlow<QuoteConversionUiState> = _uiState

    fun convertQuoteToInvoice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, errorMessage = null, convertedInvoiceId = null) }
            when (val result = workflowRepository.createInvoiceFromQuote(quoteId)) {
                is com.servicesphere.data.repository.ConversionResult.Created -> _uiState.update { it.copy(isConverting = false, convertedInvoiceId = result.value.id) }
                is com.servicesphere.data.repository.ConversionResult.Existing -> _uiState.update { it.copy(isConverting = false, convertedInvoiceId = result.value.id) }
                com.servicesphere.data.repository.ConversionResult.SourceNotFound -> _uiState.update { it.copy(isConverting = false, errorMessage = "Quote not found") }
                is com.servicesphere.data.repository.ConversionResult.Failure -> _uiState.update { it.copy(isConverting = false, errorMessage = result.message) }
            }
        }
    }

    fun clearConvertedInvoice() = _uiState.update { it.copy(convertedInvoiceId = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    class Factory(
        private val quoteId: String,
        private val workflowRepository: com.servicesphere.data.repository.WorkflowRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConvertQuoteToInvoiceViewModel(quoteId, workflowRepository) as T
    }
}

private fun LineItemEntity.toUi(): InvoiceLineItemUiModel = InvoiceLineItemUiModel(description, quantity, unitPrice, total)

private data class InvoiceDetailRows(
    val invoice: InvoiceEntity?,
    val items: List<LineItemEntity>,
    val clients: List<com.servicesphere.data.local.ClientEntity>,
    val jobs: List<com.servicesphere.data.local.JobEntity>,
    val quotes: List<com.servicesphere.data.local.QuoteEntity>
)

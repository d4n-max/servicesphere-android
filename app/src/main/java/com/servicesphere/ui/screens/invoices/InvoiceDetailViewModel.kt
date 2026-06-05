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
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    private val businessRepository: BusinessRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuoteConversionUiState())
    val uiState: StateFlow<QuoteConversionUiState> = _uiState

    fun convertQuoteToInvoice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, errorMessage = null, convertedInvoiceId = null) }
            runCatching {
                val quote = quoteRepository.getQuoteByIdOnce(quoteId) ?: error("Quote not found")
                val quoteItems = lineItemRepository.observeLineItems(quoteId, LineItemParentType.QUOTE).first()
                if (quoteItems.isEmpty()) error("Add at least one line item before converting this quote")
                val profile = ensureBusinessProfile()
                val now = System.currentTimeMillis()
                val invoiceId = UUID.randomUUID().toString()
                val dueDate = LocalDate.now().plusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val invoice = InvoiceEntity(
                    id = invoiceId,
                    clientId = quote.clientId,
                    jobId = quote.jobId,
                    quoteId = quote.id,
                    invoiceNumber = nextInvoiceNumber(profile),
                    status = InvoiceStatus.DRAFT,
                    issueDate = now,
                    dueDate = dueDate,
                    subtotal = quote.subtotal,
                    discountAmount = quote.discountAmount,
                    taxAmount = quote.taxAmount,
                    total = quote.total,
                    notes = quote.notes,
                    createdAt = now,
                    updatedAt = now
                )
                invoiceRepository.insertInvoice(invoice)
                lineItemRepository.insertLineItems(quoteItems.map {
                    LineItemEntity(
                        id = UUID.randomUUID().toString(),
                        parentId = invoiceId,
                        parentType = LineItemParentType.INVOICE,
                        description = it.description,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        total = it.total,
                        sortOrder = it.sortOrder
                    )
                })
                businessRepository.updateBusinessProfile(profile.copy(nextInvoiceNumber = profile.nextInvoiceNumber + 1, updatedAt = now))
                quoteRepository.updateQuote(quote.copy(status = QuoteStatus.CONVERTED_TO_INVOICE, updatedAt = now))
                invoiceId
            }.onSuccess { invoiceId ->
                _uiState.update { it.copy(isConverting = false, convertedInvoiceId = invoiceId) }
            }.onFailure { error ->
                _uiState.update { it.copy(isConverting = false, errorMessage = error.message ?: "Unable to convert quote") }
            }
        }
    }

    fun clearConvertedInvoice() = _uiState.update { it.copy(convertedInvoiceId = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private suspend fun ensureBusinessProfile(): BusinessProfileEntity {
        val existing = businessRepository.getBusinessProfileOnce()
        if (existing != null) return existing
        val fallback = BusinessProfileEntity(businessName = "ServiceSphere Business")
        businessRepository.upsertBusinessProfile(fallback)
        return fallback
    }

    private fun nextInvoiceNumber(profile: BusinessProfileEntity): String {
        val prefix = profile.invoicePrefix.ifBlank { "INV-" }
        return "$prefix${profile.nextInvoiceNumber.toString().padStart(4, '0')}"
    }

    class Factory(
        private val quoteId: String,
        private val quoteRepository: QuoteRepository,
        private val invoiceRepository: InvoiceRepository,
        private val lineItemRepository: LineItemRepository,
        private val businessRepository: BusinessRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConvertQuoteToInvoiceViewModel(quoteId, quoteRepository, invoiceRepository, lineItemRepository, businessRepository) as T
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

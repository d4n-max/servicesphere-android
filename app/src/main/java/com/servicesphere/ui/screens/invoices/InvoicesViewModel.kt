package com.servicesphere.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.LineItemParentType
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class InvoiceUiModel(
    val id: String,
    val invoiceNumber: String,
    val clientId: String?,
    val clientName: String?,
    val clientPhone: String?,
    val clientEmail: String?,
    val jobId: String?,
    val jobTitle: String?,
    val quoteId: String?,
    val quoteNumber: String?,
    val status: String,
    val issueDate: Long,
    val dueDate: Long?,
    val paidDate: Long?,
    val paymentMethod: String?,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayStatus: String get() = effectiveStatus().toDisplayStatus()
    val displayIssueDate: String get() = formatInvoiceDate(issueDate)
    val displayDueDate: String? get() = dueDate?.let(::formatInvoiceDate)
    val displayPaidDate: String? get() = paidDate?.let(::formatInvoiceDate)
    val displayTotal: String get() = formatInvoiceMoney(total)

    fun effectiveStatus(now: Long = System.currentTimeMillis()): String =
        if (status !in setOf(InvoiceStatus.PAID, InvoiceStatus.CANCELLED) && dueDate != null && dueDate < now) InvoiceStatus.OVERDUE else status
}

data class InvoicesUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val invoices: List<InvoiceUiModel> = emptyList(),
    val totalInvoices: Int = 0,
    val errorMessage: String? = null,
    val isEmpty: Boolean = true
)

class InvoicesViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository,
    quoteRepository: QuoteRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedStatusFilter = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val invoiceRows = combine(
        invoiceRepository.observeInvoices(),
        clientRepository.observeClients(),
        jobRepository.observeJobs()
    ) { invoices, clients, jobs -> Triple(invoices, clients, jobs) }

    private val joinedRows = combine(invoiceRows, quoteRepository.observeQuotes()) { rows, quotes ->
        InvoiceRows(rows.first, rows.second, rows.third, quotes)
    }

    val uiState: StateFlow<InvoicesUiState> = combine(
        joinedRows,
        searchQuery,
        selectedStatusFilter,
        errorMessage
    ) { rows, query, filter, error ->
        val clientMap = rows.clients.associateBy { it.id }
        val jobMap = rows.jobs.associateBy { it.id }
        val quoteMap = rows.quotes.associateBy { it.id }
        val invoices = rows.invoices
            .map { invoice -> invoice.toUiModel(clientMap[invoice.clientId], jobMap[invoice.jobId], quoteMap[invoice.quoteId]) }
            .filter { filter == null || it.effectiveStatus() == filter }
            .filter { it.matchesQuery(query) }
        InvoicesUiState(false, query, filter, invoices, rows.invoices.size, error, invoices.isEmpty())
    }
        .catch { error -> emit(InvoicesUiState(isLoading = false, errorMessage = error.message ?: "Unable to load invoices")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoicesUiState())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterChanged(status: String?) {
        selectedStatusFilter.value = status
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            runCatching {
                invoiceRepository.getInvoiceByIdOnce(invoiceId)?.let {
                    lineItemRepository.deleteLineItemsForParent(invoiceId, LineItemParentType.INVOICE)
                    invoiceRepository.deleteInvoice(it)
                }
            }.onFailure { error -> errorMessage.value = error.message ?: "Unable to delete invoice" }
        }
    }

    fun markInvoicePaid(invoiceId: String, paymentMethod: String) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val invoice = invoiceRepository.getInvoiceByIdOnce(invoiceId) ?: return@runCatching
                invoiceRepository.updateInvoice(
                    invoice.copy(
                        status = InvoiceStatus.PAID,
                        paidDate = now,
                        paymentMethod = paymentMethod,
                        updatedAt = now
                    )
                )
            }.onFailure { error -> errorMessage.value = error.message ?: "Unable to mark invoice paid" }
        }
    }

    fun clearError() = errorMessage.update { null }

    class Factory(
        private val invoiceRepository: InvoiceRepository,
        private val lineItemRepository: LineItemRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InvoicesViewModel(invoiceRepository, lineItemRepository, clientRepository, jobRepository, quoteRepository) as T
    }
}

fun InvoiceEntity.toUiModel(client: ClientEntity?, job: JobEntity?, quote: QuoteEntity?): InvoiceUiModel = InvoiceUiModel(
    id = id,
    invoiceNumber = invoiceNumber,
    clientId = clientId,
    clientName = client?.name,
    clientPhone = client?.phone,
    clientEmail = client?.email,
    jobId = jobId,
    jobTitle = job?.title,
    quoteId = quoteId,
    quoteNumber = quote?.quoteNumber,
    status = status,
    issueDate = issueDate,
    dueDate = dueDate,
    paidDate = paidDate,
    paymentMethod = paymentMethod,
    subtotal = subtotal,
    discountAmount = discountAmount,
    taxAmount = taxAmount,
    total = total,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun InvoiceUiModel.matchesQuery(query: String): Boolean {
    val value = query.trim()
    if (value.isBlank()) return true
    return listOf(invoiceNumber, clientName, jobTitle, quoteNumber, status, displayStatus, paymentMethod, notes)
        .filterNotNull()
        .any { it.contains(value, ignoreCase = true) }
}

private data class InvoiceRows(
    val invoices: List<InvoiceEntity>,
    val clients: List<ClientEntity>,
    val jobs: List<JobEntity>,
    val quotes: List<QuoteEntity>
)

val invoiceStatuses = listOf(
    InvoiceStatus.DRAFT,
    InvoiceStatus.SENT,
    InvoiceStatus.UNPAID,
    InvoiceStatus.PAID,
    InvoiceStatus.OVERDUE,
    InvoiceStatus.CANCELLED
)

fun String.toDisplayStatus(): String =
    lowercase(Locale.US).split("_").joinToString(" ") { part ->
        part.replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

fun formatInvoiceDate(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

fun formatInvoiceMoney(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

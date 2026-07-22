package com.servicesphere.ui.screens.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.LineItemParentType
import com.servicesphere.domain.model.QuoteStatus
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

data class QuoteUiModel(
    val id: String,
    val quoteNumber: String,
    val clientId: String?,
    val clientName: String?,
    val clientPhone: String?,
    val clientEmail: String?,
    val jobId: String?,
    val jobTitle: String?,
    val status: String,
    val issueDate: Long,
    val validUntil: Long?,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val notes: String?,
    val pdfPath: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayStatus: String get() = status.toDisplayStatus()
    val displayIssueDate: String get() = formatQuoteDate(issueDate)
    val displayValidUntil: String? get() = validUntil?.let(::formatQuoteDate)
    val displayTotal: String get() = formatMoney(total)
}

data class QuotesUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val quotes: List<QuoteUiModel> = emptyList(),
    val totalQuotes: Int = 0,
    val errorMessage: String? = null,
    val isEmpty: Boolean = true
)

class QuotesViewModel(
    private val quoteRepository: QuoteRepository,
    private val lineItemRepository: LineItemRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedStatusFilter = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val quoteRows = combine(
        quoteRepository.observeQuotes(),
        clientRepository.observeClients(),
        jobRepository.observeJobs()
    ) { quotes, clients, jobs ->
        Triple(quotes, clients, jobs)
    }

    val uiState: StateFlow<QuotesUiState> = combine(
        quoteRows,
        searchQuery,
        selectedStatusFilter,
        errorMessage
    ) { rows, query, filter, error ->
        val (quotes, clients, jobs) = rows
        val clientMap = clients.associateBy { it.id }
        val jobMap = jobs.associateBy { it.id }
        val rows = quotes
            .filter { filter == null || it.status == filter }
            .map { quote -> quote.toUiModel(clientMap[quote.clientId], jobMap[quote.jobId]) }
            .filter { it.matchesQuery(query) }
        QuotesUiState(false, query, filter, rows, quotes.size, error, rows.isEmpty())
    }
        .catch { error -> emit(QuotesUiState(isLoading = false, errorMessage = error.message ?: "Unable to load quotes")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuotesUiState())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onStatusFilterChanged(status: String?) {
        selectedStatusFilter.value = status
    }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch {
            runCatching {
                quoteRepository.getQuoteByIdOnce(quoteId)?.let {
                    lineItemRepository.deleteLineItemsForParent(quoteId, LineItemParentType.QUOTE)
                    quoteRepository.deleteQuote(it)
                }
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to delete quote"
            }
        }
    }

    fun clearError() = errorMessage.update { null }

    class Factory(
        private val quoteRepository: QuoteRepository,
        private val lineItemRepository: LineItemRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuotesViewModel(quoteRepository, lineItemRepository, clientRepository, jobRepository) as T
    }
}

fun QuoteEntity.toUiModel(client: ClientEntity?, job: JobEntity?): QuoteUiModel = QuoteUiModel(
    id = id,
    quoteNumber = quoteNumber,
    clientId = clientId,
    clientName = client?.name,
    clientPhone = client?.phone,
    clientEmail = client?.email,
    jobId = jobId,
    jobTitle = job?.title,
    status = status,
    issueDate = issueDate,
    validUntil = validUntil,
    subtotal = subtotal,
    discountAmount = discountAmount,
    taxAmount = taxAmount,
    total = total,
    notes = notes,
    pdfPath = pdfPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun QuoteUiModel.matchesQuery(query: String): Boolean {
    val value = query.trim()
    if (value.isBlank()) return true
    return listOf(quoteNumber, clientName, jobTitle, status, displayStatus, notes)
        .filterNotNull()
        .any { it.contains(value, ignoreCase = true) }
}

val quoteStatuses = listOf(
    QuoteStatus.DRAFT,
    QuoteStatus.SENT,
    QuoteStatus.ACCEPTED,
    QuoteStatus.REJECTED,
    QuoteStatus.CONVERTED_TO_INVOICE
)

fun String.toDisplayStatus(): String =
    lowercase(Locale.US).split("_").joinToString(" ") { part ->
        part.replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

fun formatQuoteDate(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

fun formatMoney(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

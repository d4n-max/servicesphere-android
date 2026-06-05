package com.servicesphere.ui.screens.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobRepository
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
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class QuoteDetailViewModel(
    private val quoteId: String,
    private val quoteRepository: QuoteRepository,
    private val lineItemRepository: LineItemRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)

    private val detailRows = combine(
        quoteRepository.observeQuoteById(quoteId),
        lineItemRepository.observeLineItems(quoteId, LineItemParentType.QUOTE),
        clientRepository.observeClients(),
        jobRepository.observeJobs()
    ) { quote, items, clients, jobs ->
        DetailRows(quote, items, clients, jobs)
    }

    val uiState: StateFlow<QuoteDetailUiState> = combine(
        detailRows,
        errorMessage,
        deleteSuccess
    ) { rows, error, deleted ->
        QuoteDetailUiState(
            isLoading = false,
            quote = rows.quote?.toUiModel(rows.clients.firstOrNull { it.id == rows.quote.clientId }, rows.jobs.firstOrNull { it.id == rows.quote.jobId }),
            lineItems = rows.items.map(LineItemEntity::toUi),
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

    class Factory(
        private val quoteId: String,
        private val quoteRepository: QuoteRepository,
        private val lineItemRepository: LineItemRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuoteDetailViewModel(quoteId, quoteRepository, lineItemRepository, clientRepository, jobRepository) as T
    }
}

private fun LineItemEntity.toUi(): QuoteLineItemUiModel = QuoteLineItemUiModel(description, quantity, unitPrice, total)

private data class DetailRows(
    val quote: com.servicesphere.data.local.QuoteEntity?,
    val items: List<LineItemEntity>,
    val clients: List<com.servicesphere.data.local.ClientEntity>,
    val jobs: List<com.servicesphere.data.local.JobEntity>
)

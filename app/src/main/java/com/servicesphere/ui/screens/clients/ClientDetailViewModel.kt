package com.servicesphere.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.ui.timeline.ActivityTimelineItem
import com.servicesphere.ui.timeline.buildTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientDetailUiState(
    val isLoading: Boolean = true,
    val client: ClientUiModel? = null,
    val timeline: List<ActivityTimelineItem> = emptyList(),
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class ClientDetailViewModel(
    private val clientId: String,
    private val clientRepository: ClientRepository,
    jobRepository: JobRepository,
    quoteRepository: QuoteRepository,
    invoiceRepository: InvoiceRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)

    private val activityRows = combine(
        clientRepository.observeClientById(clientId),
        jobRepository.observeJobs(),
        quoteRepository.observeQuotes(),
        invoiceRepository.observeInvoices()
    ) { client, jobs, quotes, invoices -> ClientActivityRows(client, jobs, quotes, invoices) }

    val uiState: StateFlow<ClientDetailUiState> = combine(
        activityRows,
        errorMessage,
        deleteSuccess
    ) { rows, error, deleted ->
        ClientDetailUiState(
            isLoading = false,
            client = rows.client?.toUiModel(),
            timeline = buildTimeline(rows.quotes.filter { it.clientId == clientId }, rows.jobs.filter { it.clientId == clientId }, rows.invoices.filter { it.clientId == clientId }),
            errorMessage = error,
            deleteSuccess = deleted
        )
    }
        .catch { error ->
            emit(ClientDetailUiState(isLoading = false, errorMessage = error.message ?: "Unable to load client"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ClientDetailUiState()
        )

    fun deleteClient() {
        viewModelScope.launch {
            runCatching {
                clientRepository.getClientByIdOnce(clientId)?.let { clientRepository.deleteClient(it) }
            }.onSuccess {
                deleteSuccess.value = true
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to delete client"
            }
        }
    }

    fun clearError() {
        errorMessage.update { null }
    }

    class Factory(
        private val clientId: String,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository,
        private val invoiceRepository: InvoiceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientDetailViewModel(clientId, clientRepository, jobRepository, quoteRepository, invoiceRepository) as T
        }
    }
}

private data class ClientActivityRows(
    val client: ClientEntity?,
    val jobs: List<com.servicesphere.data.local.JobEntity>,
    val quotes: List<com.servicesphere.data.local.QuoteEntity>,
    val invoices: List<com.servicesphere.data.local.InvoiceEntity>
)

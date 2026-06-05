package com.servicesphere.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.repository.ClientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientUiModel(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class ClientsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val clients: List<ClientUiModel> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsViewModel(
    private val clientRepository: ClientRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ClientsUiState> = combine(
        searchQuery,
        searchQuery.flatMapLatest { query ->
            if (query.isBlank()) clientRepository.observeClients() else clientRepository.searchClients(query.trim())
        }.map { clients -> clients.map(ClientEntity::toUiModel) }
            .catch { error ->
                errorMessage.value = error.message ?: "Unable to load clients"
                emit(emptyList())
            },
        errorMessage
    ) { query, clients, error ->
        ClientsUiState(
            isLoading = false,
            searchQuery = query,
            clients = clients,
            errorMessage = error,
            isEmpty = clients.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            runCatching {
                clientRepository.getClientByIdOnce(clientId)?.let { clientRepository.deleteClient(it) }
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Unable to delete client"
            }
        }
    }

    fun clearError() {
        errorMessage.update { null }
    }

    class Factory(private val clientRepository: ClientRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientsViewModel(clientRepository) as T
        }
    }
}

fun ClientEntity.toUiModel(): ClientUiModel = ClientUiModel(
    id = id,
    name = name,
    phone = phone,
    email = email,
    address = address,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

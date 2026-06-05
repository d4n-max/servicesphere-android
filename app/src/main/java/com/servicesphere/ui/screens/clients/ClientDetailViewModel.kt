package com.servicesphere.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.repository.ClientRepository
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
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class ClientDetailViewModel(
    private val clientId: String,
    private val clientRepository: ClientRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val deleteSuccess = MutableStateFlow(false)

    val uiState: StateFlow<ClientDetailUiState> = combine(
        clientRepository.observeClientById(clientId),
        errorMessage,
        deleteSuccess
    ) { client, error, deleted ->
        ClientDetailUiState(
            isLoading = false,
            client = client?.toUiModel(),
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
        private val clientRepository: ClientRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientDetailViewModel(clientId, clientRepository) as T
        }
    }
}

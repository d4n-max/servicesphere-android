package com.servicesphere.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.repository.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientFormUiState(
    val id: String? = null,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val clientNotFound: Boolean = false
)

class ClientFormViewModel(
    private val clientRepository: ClientRepository,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientFormUiState())
    val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

    fun loadClient(clientId: String) {
        if (_uiState.value.id == clientId && _uiState.value.isEditing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditing = true, errorMessage = null) }
            runCatching { clientRepository.getClientByIdOnce(clientId) }
                .onSuccess { client ->
                    if (client == null) {
                        _uiState.update { it.copy(isLoading = false, clientNotFound = true, errorMessage = "Client not found") }
                    } else {
                        _uiState.update {
                            it.copy(
                                id = client.id,
                                name = client.name,
                                phone = client.phone.orEmpty(),
                                email = client.email.orEmpty(),
                                address = client.address.orEmpty(),
                                notes = client.notes.orEmpty(),
                                isEditing = true,
                                isLoading = false,
                                clientNotFound = false
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load client") }
                }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = value, phoneError = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onAddressChanged(value: String) {
        _uiState.update { it.copy(address = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveClient() {
        val current = _uiState.value
        val trimmedName = current.name.trim()
        val trimmedEmail = current.email.trim()
        val trimmedPhone = current.phone.trim()
        val nameError = if (trimmedName.isBlank()) "Client name is required" else null
        val emailError = if (trimmedEmail.isNotBlank() && !looksLikeEmail(trimmedEmail)) "Enter a valid email address" else null
        if (nameError != null || emailError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val entity = ClientEntity(
                    id = current.id ?: java.util.UUID.randomUUID().toString(),
                    name = trimmedName,
                    phone = trimmedPhone.ifBlank { null },
                    email = trimmedEmail.ifBlank { null },
                    address = current.address.trim().ifBlank { null },
                    notes = current.notes.trim().ifBlank { null },
                    createdAt = if (current.isEditing) {
                        clientRepository.getClientByIdOnce(current.id.orEmpty())?.createdAt ?: now
                    } else {
                        now
                    },
                    updatedAt = now
                )
                if (current.isEditing) {
                    clientRepository.updateClient(entity)
                } else {
                    clientRepository.insertClient(entity)
                    activationTracker.trackFirst(
                        ActivationEvents.FIRST_CLIENT_CREATED,
                        mapOf(ActivationParams.SOURCE_SCREEN to "client_form")
                    )
                }
                entity.id
            }.onSuccess { savedId ->
                _uiState.update { it.copy(id = savedId, isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Unable to save client") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun looksLikeEmail(value: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()
    }

    class Factory(
        private val clientRepository: ClientRepository,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientFormViewModel(clientRepository, activationTracker) as T
        }
    }
}

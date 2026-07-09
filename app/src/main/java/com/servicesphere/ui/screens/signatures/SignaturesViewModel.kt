package com.servicesphere.ui.screens.signatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.data.local.SignatureImageStorage
import com.servicesphere.data.repository.SignatureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignatureUiModel(
    val id: String,
    val jobId: String?,
    val invoiceId: String?,
    val localUri: String,
    val signedBy: String?,
    val approvalText: String?,
    val createdAt: Long
)

data class SignaturesUiState(
    val isLoading: Boolean = true,
    val signatures: List<SignatureUiModel> = emptyList(),
    val selectedSignature: SignatureUiModel? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val editSignatureId: String? = null,
    val signedByDraft: String = "",
    val approvalTextDraft: String = ""
)

class SignaturesViewModel(
    private val signaturesFlow: Flow<List<SignatureEntity>>,
    private val repository: SignatureRepository,
    private val storage: SignatureImageStorage
) : ViewModel() {
    private val selectedSignatureId = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)
    private val editSignatureId = MutableStateFlow<String?>(null)
    private val signedByDraft = MutableStateFlow("")
    private val approvalTextDraft = MutableStateFlow("")

    private val baseRows = combine(signaturesFlow, selectedSignatureId, errorMessage) { signatures, selectedId, error ->
        SignatureBaseRows(signatures, selectedId, error)
    }
    private val editRows = combine(editSignatureId, signedByDraft, approvalTextDraft, successMessage) { editId, signedBy, approval, success ->
        SignatureEditRows(editId, signedBy, approval, success)
    }

    val uiState: StateFlow<SignaturesUiState> = combine(baseRows, editRows) { base, edit ->
        val models = base.signatures.map { it.toUiModel() }
        SignaturesUiState(
            isLoading = false,
            signatures = models,
            selectedSignature = models.firstOrNull { it.id == base.selectedSignatureId },
            errorMessage = base.errorMessage,
            successMessage = edit.successMessage,
            editSignatureId = edit.editSignatureId,
            signedByDraft = edit.signedByDraft,
            approvalTextDraft = edit.approvalTextDraft
        )
    }
        .catch { error -> emit(SignaturesUiState(isLoading = false, errorMessage = error.message ?: "Unable to load signatures")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignaturesUiState())

    fun onSignatureSelected(signatureId: String) {
        selectedSignatureId.value = signatureId
    }

    fun closePreview() {
        selectedSignatureId.value = null
    }

    fun startEditSignature(signatureId: String) {
        val signature = uiState.value.signatures.firstOrNull { it.id == signatureId }
        editSignatureId.value = signatureId
        signedByDraft.value = signature?.signedBy.orEmpty()
        approvalTextDraft.value = signature?.approvalText.orEmpty()
    }

    fun onSignedByChanged(value: String) {
        signedByDraft.value = value
    }

    fun onApprovalTextChanged(value: String) {
        approvalTextDraft.value = value
    }

    fun saveSignatureMetadata() {
        val signatureId = editSignatureId.value ?: return
        viewModelScope.launch {
            runCatching {
                val signature = repository.getSignatureByIdOnce(signatureId) ?: return@runCatching
                repository.updateSignature(
                    signature.copy(
                        signedBy = signedByDraft.value.trim().ifBlank { null },
                        approvalText = approvalTextDraft.value.trim().ifBlank { null }
                    )
                )
            }.onSuccess {
                editSignatureId.value = null
                signedByDraft.value = ""
                approvalTextDraft.value = ""
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Could not save signature details"
            }
        }
    }

    fun deleteSignature(signatureId: String) {
        viewModelScope.launch {
            runCatching {
                val signature = repository.getSignatureByIdOnce(signatureId) ?: return@runCatching
                repository.deleteSignature(signature)
                storage.deleteStoredSignature(signature.localUri)
            }.onSuccess {
                if (selectedSignatureId.value == signatureId) selectedSignatureId.value = null
                successMessage.value = "Signature deleted"
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Could not delete signature"
            }
        }
    }

    fun clearError() = errorMessage.update { null }
    fun clearSuccess() = successMessage.update { null }
    fun cancelEdit() {
        editSignatureId.value = null
        signedByDraft.value = ""
        approvalTextDraft.value = ""
    }

    class Factory(
        private val signaturesFlow: Flow<List<SignatureEntity>>,
        private val repository: SignatureRepository,
        private val storage: SignatureImageStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SignaturesViewModel(signaturesFlow, repository, storage) as T
    }
}

private fun SignatureEntity.toUiModel(): SignatureUiModel = SignatureUiModel(
    id = id,
    jobId = jobId,
    invoiceId = invoiceId,
    localUri = localUri,
    signedBy = signedBy,
    approvalText = approvalText,
    createdAt = createdAt
)

private data class SignatureBaseRows(
    val signatures: List<SignatureEntity>,
    val selectedSignatureId: String?,
    val errorMessage: String?
)

private data class SignatureEditRows(
    val editSignatureId: String?,
    val signedByDraft: String,
    val approvalTextDraft: String,
    val successMessage: String?
)

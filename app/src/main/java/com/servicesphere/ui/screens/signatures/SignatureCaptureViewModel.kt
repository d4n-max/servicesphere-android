package com.servicesphere.ui.screens.signatures

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.data.local.SignatureImageStorage
import com.servicesphere.data.repository.SignatureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class SignatureCaptureUiState(
    val jobId: String? = null,
    val invoiceId: String? = null,
    val signedBy: String = "",
    val approvalText: String = "",
    val isSaving: Boolean = false,
    val hasDrawnSignature: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessSignatureId: String? = null
) {
    val isInvoiceSignature: Boolean get() = invoiceId != null
}

class SignatureCaptureViewModel(
    private val repository: SignatureRepository,
    private val storage: SignatureImageStorage,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignatureCaptureUiState())
    val uiState: StateFlow<SignatureCaptureUiState> = _uiState.asStateFlow()

    fun initialize(jobId: String?, invoiceId: String?) {
        val cleanJobId = jobId?.takeIf { it.isNotBlank() }
        val cleanInvoiceId = invoiceId?.takeIf { it.isNotBlank() }
        val preferredInvoiceId = cleanInvoiceId
        val preferredJobId = if (preferredInvoiceId == null) cleanJobId else null
        val current = _uiState.value
        if (current.jobId == preferredJobId && current.invoiceId == preferredInvoiceId) return
        _uiState.value = SignatureCaptureUiState(
            jobId = preferredJobId,
            invoiceId = preferredInvoiceId,
            approvalText = if (preferredInvoiceId != null) {
                "I confirm this invoice has been reviewed and accepted."
            } else {
                "I confirm the work described for this job has been completed."
            },
            errorMessage = if (preferredJobId == null && preferredInvoiceId == null) "A job or invoice is required to capture a signature." else null
        )
    }

    fun onSignedByChanged(value: String) = _uiState.update { it.copy(signedBy = value) }
    fun onApprovalTextChanged(value: String) = _uiState.update { it.copy(approvalText = value) }
    fun onSignatureDrawnChanged(hasDrawing: Boolean) = _uiState.update { it.copy(hasDrawnSignature = hasDrawing, errorMessage = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetSaveSuccess() = _uiState.update { it.copy(saveSuccessSignatureId = null) }

    fun saveSignature(bitmap: Bitmap?) {
        val state = _uiState.value
        val parentId = state.invoiceId ?: state.jobId
        when {
            parentId == null -> _uiState.update { it.copy(errorMessage = "A job or invoice is required to capture a signature.") }
            !state.hasDrawnSignature || bitmap == null -> _uiState.update { it.copy(errorMessage = "Please add a signature before saving.") }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                runCatching {
                    val localUri = storage.saveSignatureBitmap(bitmap, parentId)
                    val signature = SignatureEntity(
                        id = UUID.randomUUID().toString(),
                        jobId = state.jobId,
                        invoiceId = state.invoiceId,
                        localUri = localUri,
                        signedBy = state.signedBy.trim().ifBlank { null },
                        approvalText = state.approvalText.trim().ifBlank { null },
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertSignature(signature)
                    signature.id
                }.onSuccess { signatureId ->
                    activationTracker.trackFirst(
                        ActivationEvents.FIRST_SIGNATURE_CAPTURED,
                        mapOf(
                            ActivationParams.SOURCE_SCREEN to "signature_capture",
                            ActivationParams.SIGNATURE_TARGET to if (state.invoiceId != null) "invoice" else "job"
                        )
                    )
                    _uiState.update { it.copy(isSaving = false, saveSuccessSignatureId = signatureId) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't save signature") }
                }
            }
        }
    }

    class Factory(
        private val repository: SignatureRepository,
        private val storage: SignatureImageStorage,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SignatureCaptureViewModel(repository, storage, activationTracker) as T
    }
}

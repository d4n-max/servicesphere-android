package com.servicesphere.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.data.export.DataExportManager
import com.servicesphere.data.export.ExportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataExportUiState(
    val isExporting: Boolean = false,
    val lastExportFileName: String? = null,
    val lastExportUri: String? = null,
    val lastExportMimeType: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val deleteConfirmationText: String = "",
    val resetSetupAfterDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteComplete: Boolean = false
)

class DataExportViewModel(
    private val exportManager: DataExportManager,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataExportUiState())
    val uiState: StateFlow<DataExportUiState> = _uiState.asStateFlow()

    fun exportBackupJson() = runExport("backup_json", exportManager::exportBackupJson)
    fun exportClientsCsv() = runExport("clients_csv", exportManager::exportClientsCsv)
    fun exportJobsCsv() = runExport("jobs_csv", exportManager::exportJobsCsv)
    fun exportQuotesCsv() = runExport("quotes_csv", exportManager::exportQuotesCsv)
    fun exportInvoicesCsv() = runExport("invoices_csv", exportManager::exportInvoicesCsv)

    fun shareLastExport() {
        val state = uiState.value
        val uri = state.lastExportUri?.let(Uri::parse)
        val mimeType = state.lastExportMimeType
        if (uri == null || mimeType.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No export file to share") }
            return
        }
        exportManager.shareFile(uri, mimeType)
            .onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "No app found to share this file") }
            }
    }

    fun requestDeleteAllData() {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = true,
                deleteConfirmationText = "",
                resetSetupAfterDelete = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelDeleteAllData() {
        _uiState.update { it.copy(showDeleteConfirmation = false, deleteConfirmationText = "", resetSetupAfterDelete = false) }
    }

    fun onDeleteConfirmationTextChanged(value: String) {
        _uiState.update { it.copy(deleteConfirmationText = value) }
    }

    fun onResetSetupChanged(value: Boolean) {
        _uiState.update { it.copy(resetSetupAfterDelete = value) }
    }

    fun confirmDeleteAllData() {
        val state = uiState.value
        if (!state.deleteConfirmationText.isDeleteConfirmation()) {
            _uiState.update { it.copy(errorMessage = "Type DELETE to confirm") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, successMessage = null) }
            val success = exportManager.deleteAllLocalData(state.resetSetupAfterDelete)
            _uiState.update {
                if (success) {
                    it.copy(
                        isDeleting = false,
                        showDeleteConfirmation = false,
                        deleteConfirmationText = "",
                        successMessage = "Local data deleted",
                        deleteComplete = true
                    )
                } else {
                    it.copy(isDeleting = false, errorMessage = "Couldn't delete local data")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun markDeleteHandled() {
        _uiState.update { it.copy(deleteComplete = false) }
    }

    private fun runExport(exportType: String, export: suspend () -> ExportResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, successMessage = null) }
            val result = export()
            if (result.success && result.fileUri != null) {
                activationTracker.track(
                    ActivationEvents.DATA_EXPORT_CREATED,
                    mapOf(
                        ActivationParams.SOURCE_SCREEN to "data_export",
                        ActivationParams.EXPORT_TYPE to exportType
                    )
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastExportFileName = result.fileName,
                        lastExportUri = result.fileUri.toString(),
                        lastExportMimeType = result.mimeType,
                        successMessage = "Export created"
                    )
                }
                exportManager.shareFile(result.fileUri, result.mimeType ?: "application/octet-stream")
                    .onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message ?: "No app found to share this file") }
                    }
            } else {
                _uiState.update { it.copy(isExporting = false, errorMessage = result.errorMessage ?: "Couldn't export data") }
            }
        }
    }

    class Factory(
        private val exportManager: DataExportManager,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DataExportViewModel(exportManager, activationTracker) as T
    }
}

fun String.isDeleteConfirmation(): Boolean = trim().equals("DELETE", ignoreCase = true)

package com.servicesphere.ui.screens.pdf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.billing.FeatureGateManager
import com.servicesphere.pdf.PdfShareManager
import com.servicesphere.pdf.ServiceSpherePdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfActionUiState(
    val isGeneratingPdf: Boolean = false,
    val generatedPdfPath: String? = null,
    val pdfErrorMessage: String? = null,
    val pdfSuccessMessage: String? = null
)

class QuotePdfActionViewModel(
    private val quoteId: String,
    private val appContext: Context,
    private val generator: ServiceSpherePdfGenerator,
    private val shareManager: PdfShareManager,
    private val featureGateManager: FeatureGateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfActionUiState())
    val uiState: StateFlow<PdfActionUiState> = _uiState.asStateFlow()

    fun generateQuotePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateQuotePdf(appContext, quoteId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            _uiState.update {
                if (result.success) {
                    it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfSuccessMessage = "Quote PDF generated")
                } else {
                    it.copy(isGeneratingPdf = false, pdfErrorMessage = result.errorMessage ?: "Couldn't generate quote PDF")
                }
            }
        }
    }

    fun shareQuotePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateQuotePdf(appContext, quoteId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            if (!result.success || result.filePath == null) {
                _uiState.update { it.copy(isGeneratingPdf = false, pdfErrorMessage = result.errorMessage ?: "Couldn't generate quote PDF") }
                return@launch
            }
            val shareError = shareManager.sharePdf(result.filePath, "Share Quote PDF")
            _uiState.update {
                if (shareError == null) it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath)
                else it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfErrorMessage = shareError)
            }
        }
    }

    fun clearPdfMessage() = _uiState.update { it.copy(pdfErrorMessage = null, pdfSuccessMessage = null) }

    private suspend fun pdfLogoAllowed(): Boolean = featureGateManager.canUseBusinessLogoOnPdf().allowed

    class Factory(
        private val quoteId: String,
        private val appContext: Context,
        private val generator: ServiceSpherePdfGenerator,
        private val shareManager: PdfShareManager,
        private val featureGateManager: FeatureGateManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuotePdfActionViewModel(quoteId, appContext, generator, shareManager, featureGateManager) as T
    }
}

class InvoicePdfActionViewModel(
    private val invoiceId: String,
    private val appContext: Context,
    private val generator: ServiceSpherePdfGenerator,
    private val shareManager: PdfShareManager,
    private val featureGateManager: FeatureGateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfActionUiState())
    val uiState: StateFlow<PdfActionUiState> = _uiState.asStateFlow()

    fun generateInvoicePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateInvoicePdf(appContext, invoiceId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            _uiState.update {
                if (result.success) {
                    it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfSuccessMessage = "Invoice PDF generated")
                } else {
                    it.copy(isGeneratingPdf = false, pdfErrorMessage = result.errorMessage ?: "Couldn't generate invoice PDF")
                }
            }
        }
    }

    fun shareInvoicePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateInvoicePdf(appContext, invoiceId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            if (!result.success || result.filePath == null) {
                _uiState.update { it.copy(isGeneratingPdf = false, pdfErrorMessage = result.errorMessage ?: "Couldn't generate invoice PDF") }
                return@launch
            }
            val shareError = shareManager.sharePdf(result.filePath, "Share Invoice PDF")
            _uiState.update {
                if (shareError == null) it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath)
                else it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfErrorMessage = shareError)
            }
        }
    }

    fun clearPdfMessage() = _uiState.update { it.copy(pdfErrorMessage = null, pdfSuccessMessage = null) }

    private suspend fun pdfLogoAllowed(): Boolean = featureGateManager.canUseBusinessLogoOnPdf().allowed

    class Factory(
        private val invoiceId: String,
        private val appContext: Context,
        private val generator: ServiceSpherePdfGenerator,
        private val shareManager: PdfShareManager,
        private val featureGateManager: FeatureGateManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InvoicePdfActionViewModel(invoiceId, appContext, generator, shareManager, featureGateManager) as T
    }
}

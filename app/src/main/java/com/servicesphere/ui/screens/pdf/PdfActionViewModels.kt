package com.servicesphere.ui.screens.pdf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.billing.FeatureGateManager
import com.servicesphere.pdf.PdfShareManager
import com.servicesphere.pdf.ServiceSpherePdfGenerator
import com.servicesphere.documents.DocumentLifecycleRepository
import com.servicesphere.documents.DocumentType
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfActionUiState(
    val isGeneratingPdf: Boolean = false,
    val generatedPdfPath: String? = null,
    val pdfErrorMessage: String? = null,
    val pdfSuccessMessage: String? = null,
    val quoteShareSuccessEventId: Long? = null
)

class QuotePdfActionViewModel(
    private val quoteId: String,
    private val appContext: Context,
    private val generator: ServiceSpherePdfGenerator,
    private val shareManager: PdfShareManager,
    private val featureGateManager: FeatureGateManager,
    private val activationTracker: ActivationTracker,
    private val lifecycleRepository: DocumentLifecycleRepository,
    private val quoteRepository: QuoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfActionUiState())
    val uiState: StateFlow<PdfActionUiState> = _uiState.asStateFlow()

    fun generateQuotePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateQuotePdf(appContext, quoteId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            _uiState.update {
                if (result.success) {
                    result.filePath?.let { path -> quoteRepository.getQuoteByIdOnce(quoteId)?.let { lifecycleRepository.recordPdf(quoteId, DocumentType.QUOTE, path, it.updatedAt) } }
                    trackPdfGenerated("quote", "quote_detail")
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
            if (shareError == null) lifecycleRepository.recordShareStarted(quoteId, DocumentType.QUOTE)
            if (shareError == null) trackPdfGenerated("quote", "quote_detail")
            _uiState.update {
                if (shareError == null) it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, quoteShareSuccessEventId = System.currentTimeMillis())
                else it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfErrorMessage = shareError)
            }
        }
    }

    fun clearPdfMessage() = _uiState.update { it.copy(pdfErrorMessage = null, pdfSuccessMessage = null) }

    private suspend fun pdfLogoAllowed(): Boolean = featureGateManager.canUseBusinessLogoOnPdf().allowed

    private fun trackPdfGenerated(documentType: String, sourceScreen: String) {
        activationTracker.trackFirst(
            ActivationEvents.FIRST_PDF_GENERATED,
            mapOf(
                ActivationParams.SOURCE_SCREEN to sourceScreen,
                ActivationParams.DOCUMENT_TYPE to documentType
            )
        )
    }

    class Factory(
        private val quoteId: String,
        private val appContext: Context,
        private val generator: ServiceSpherePdfGenerator,
        private val shareManager: PdfShareManager,
        private val featureGateManager: FeatureGateManager,
        private val activationTracker: ActivationTracker,
        private val lifecycleRepository: DocumentLifecycleRepository,
        private val quoteRepository: QuoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuotePdfActionViewModel(quoteId, appContext, generator, shareManager, featureGateManager, activationTracker, lifecycleRepository, quoteRepository) as T
    }
}

class InvoicePdfActionViewModel(
    private val invoiceId: String,
    private val appContext: Context,
    private val generator: ServiceSpherePdfGenerator,
    private val shareManager: PdfShareManager,
    private val featureGateManager: FeatureGateManager,
    private val activationTracker: ActivationTracker,
    private val lifecycleRepository: DocumentLifecycleRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfActionUiState())
    val uiState: StateFlow<PdfActionUiState> = _uiState.asStateFlow()

    fun generateInvoicePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfErrorMessage = null, pdfSuccessMessage = null) }
            val result = generator.generateInvoicePdf(appContext, invoiceId, pdfLogoAllowed(), featureGateManager.shouldShowPdfWatermark())
            _uiState.update {
                if (result.success) {
                    result.filePath?.let { path -> invoiceRepository.getInvoiceByIdOnce(invoiceId)?.let { lifecycleRepository.recordPdf(invoiceId, DocumentType.INVOICE, path, it.updatedAt) } }
                    trackPdfGenerated("invoice", "invoice_detail")
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
            if (shareError == null) lifecycleRepository.recordShareStarted(invoiceId, DocumentType.INVOICE)
            if (shareError == null) trackPdfGenerated("invoice", "invoice_detail")
            _uiState.update {
                if (shareError == null) it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath)
                else it.copy(isGeneratingPdf = false, generatedPdfPath = result.filePath, pdfErrorMessage = shareError)
            }
        }
    }

    fun clearPdfMessage() = _uiState.update { it.copy(pdfErrorMessage = null, pdfSuccessMessage = null) }

    private suspend fun pdfLogoAllowed(): Boolean = featureGateManager.canUseBusinessLogoOnPdf().allowed

    private fun trackPdfGenerated(documentType: String, sourceScreen: String) {
        activationTracker.trackFirst(
            ActivationEvents.FIRST_PDF_GENERATED,
            mapOf(
                ActivationParams.SOURCE_SCREEN to sourceScreen,
                ActivationParams.DOCUMENT_TYPE to documentType
            )
        )
    }

    class Factory(
        private val invoiceId: String,
        private val appContext: Context,
        private val generator: ServiceSpherePdfGenerator,
        private val shareManager: PdfShareManager,
        private val featureGateManager: FeatureGateManager,
        private val activationTracker: ActivationTracker,
        private val lifecycleRepository: DocumentLifecycleRepository,
        private val invoiceRepository: InvoiceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InvoicePdfActionViewModel(invoiceId, appContext, generator, shareManager, featureGateManager, activationTracker, lifecycleRepository, invoiceRepository) as T
    }
}

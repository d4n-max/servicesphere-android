package com.servicesphere.ui.screens.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.messaging.ClientMessageData
import com.servicesphere.messaging.MessageShareManager
import com.servicesphere.messaging.MessageTemplateGenerator
import com.servicesphere.messaging.MessageTemplateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class MessageComposerUiState(
    val isLoading: Boolean = true,
    val type: MessageTemplateType = MessageTemplateType.CUSTOM,
    val clientId: String? = null,
    val jobId: String? = null,
    val quoteId: String? = null,
    val invoiceId: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val clientEmail: String? = null,
    val subject: String = "ServiceSphere message",
    val message: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shareSuccessEventId: Long? = null
)

class MessageComposerViewModel(
    private val businessRepository: BusinessRepository,
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val shareManager: MessageShareManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(MessageComposerUiState())
    val uiState: StateFlow<MessageComposerUiState> = _uiState.asStateFlow()
    private var initializedKey: String? = null

    fun initialize(typeValue: String?, clientId: String?, jobId: String?, quoteId: String?, invoiceId: String?) {
        val key = listOf(typeValue, clientId, jobId, quoteId, invoiceId).joinToString("|")
        if (initializedKey == key) return
        initializedKey = key
        val type = MessageTemplateType.fromRoute(typeValue)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    type = type,
                    clientId = clientId,
                    jobId = jobId,
                    quoteId = quoteId,
                    invoiceId = invoiceId,
                    errorMessage = null,
                    successMessage = null
                )
            }
            runCatching { buildMessageState(type, clientId, jobId, quoteId, invoiceId) }
                .onSuccess { next -> _uiState.value = next }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            type = type,
                            errorMessage = error.message ?: "Unable to prepare message",
                            message = MessageTemplateGenerator.generate(type, ClientMessageData())
                        )
                    }
                }
        }
    }

    fun onMessageChanged(value: String) = _uiState.update { it.copy(message = value) }

    fun shareMessage() = runShareAction(success = "Message shared", countsForReview = true) { shareManager.sharePlainText(uiState.value.message) }

    fun sendSms() = runShareAction(success = "Message shared", countsForReview = true) { shareManager.sendSms(uiState.value.clientPhone, uiState.value.message) }

    fun sendEmail() = runShareAction(success = "Message shared", countsForReview = true) { shareManager.sendEmail(uiState.value.clientEmail, uiState.value.subject, uiState.value.message) }

    fun copyToClipboard() = runShareAction(success = "Message copied") { shareManager.copyToClipboard(uiState.value.message) }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    private fun runShareAction(success: String? = null, countsForReview: Boolean = false, action: () -> Result<Unit>) {
        if (uiState.value.message.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Write a message before sharing.", successMessage = null) }
            return
        }
        action()
            .onSuccess {
                _uiState.update {
                    it.copy(
                        errorMessage = null,
                        successMessage = success,
                        shareSuccessEventId = if (countsForReview) System.currentTimeMillis() else it.shareSuccessEventId
                    )
                }
            }
            .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "No app found to send this message", successMessage = null) } }
    }

    private suspend fun buildMessageState(
        type: MessageTemplateType,
        clientId: String?,
        jobId: String?,
        quoteId: String?,
        invoiceId: String?
    ): MessageComposerUiState {
        val profile = businessRepository.getBusinessProfileOnce()
        var client: ClientEntity? = clientId?.let { clientRepository.getClientByIdOnce(it) }
        var job: JobEntity? = jobId?.let { jobRepository.getJobByIdOnce(it) }
        var quote: QuoteEntity? = quoteId?.let { quoteRepository.getQuoteByIdOnce(it) }
        var invoice: InvoiceEntity? = invoiceId?.let { invoiceRepository.getInvoiceByIdOnce(it) }

        if (invoice != null) {
            if (client == null) client = invoice.clientId?.let { clientRepository.getClientByIdOnce(it) }
            if (job == null) job = invoice.jobId?.let { jobRepository.getJobByIdOnce(it) }
            if (quote == null) quote = invoice.quoteId?.let { quoteRepository.getQuoteByIdOnce(it) }
        }
        if (quote != null) {
            if (client == null) client = quote.clientId?.let { clientRepository.getClientByIdOnce(it) }
            if (job == null) job = quote.jobId?.let { jobRepository.getJobByIdOnce(it) }
        }
        if (job != null && client == null) {
            client = job.clientId?.let { clientRepository.getClientByIdOnce(it) }
        }

        val data = ClientMessageData(
            businessName = profile?.businessName,
            ownerName = profile?.ownerName,
            clientName = client?.name,
            clientPhone = client?.phone,
            clientEmail = client?.email,
            jobTitle = job?.title,
            jobAddress = job?.address,
            quoteNumber = quote?.quoteNumber,
            invoiceNumber = invoice?.invoiceNumber,
            total = invoice?.total?.let { formatCurrency(it, profile?.currencyCode) } ?: quote?.total?.let { formatCurrency(it, profile?.currencyCode) },
            dueDate = invoice?.dueDate?.let(::formatDate),
            paymentInstructions = profile?.paymentInstructions,
            currencyCode = profile?.currencyCode
        )

        return MessageComposerUiState(
            isLoading = false,
            type = type,
            clientId = client?.id ?: clientId,
            jobId = job?.id ?: jobId,
            quoteId = quote?.id ?: quoteId,
            invoiceId = invoice?.id ?: invoiceId,
            clientName = client?.name,
            clientPhone = client?.phone,
            clientEmail = client?.email,
            subject = subjectFor(type, quote, invoice, job, profile),
            message = MessageTemplateGenerator.generate(type, data)
        )
    }

    private fun subjectFor(
        type: MessageTemplateType,
        quote: QuoteEntity?,
        invoice: InvoiceEntity?,
        job: JobEntity?,
        profile: BusinessProfileEntity?
    ): String {
        val business = profile?.businessName?.takeIf { it.isNotBlank() } ?: "ServiceSphere"
        return when (type) {
            MessageTemplateType.QUOTE_FOLLOW_UP -> "Following up on ${quote?.quoteNumber ?: "your quote"}"
            MessageTemplateType.QUOTE_ACCEPTED_NEXT_STEPS -> "Next steps for ${quote?.quoteNumber ?: "your quote"}"
            MessageTemplateType.INVOICE_PAYMENT_REMINDER -> "Payment reminder for ${invoice?.invoiceNumber ?: "your invoice"}"
            MessageTemplateType.INVOICE_OVERDUE_REMINDER -> "Overdue invoice ${invoice?.invoiceNumber ?: ""}".trim()
            MessageTemplateType.JOB_COMPLETED -> "${job?.title ?: "Your job"} completed"
            MessageTemplateType.THANK_YOU_REVIEW_REQUEST -> "Thank you from $business"
            MessageTemplateType.CUSTOM -> "Message from $business"
        }
    }

    class Factory(
        private val businessRepository: BusinessRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository,
        private val invoiceRepository: InvoiceRepository,
        private val shareManager: MessageShareManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessageComposerViewModel(businessRepository, clientRepository, jobRepository, quoteRepository, invoiceRepository, shareManager) as T
    }
}

fun formatCurrency(amount: Double, currencyCode: String?): String {
    if (amount <= 0.0) return ""
    val formatter = NumberFormat.getCurrencyInstance()
    currencyCode?.takeIf { it.isNotBlank() }?.let {
        runCatching { formatter.currency = Currency.getInstance(it.uppercase(Locale.US)) }
    }
    return formatter.format(amount)
}

fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

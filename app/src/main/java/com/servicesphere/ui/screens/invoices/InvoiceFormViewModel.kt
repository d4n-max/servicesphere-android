package com.servicesphere.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.activation.AnalyticsValueBuckets
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.LineItemParentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

data class InvoiceClientPickerUiModel(val id: String, val name: String)
data class InvoiceJobPickerUiModel(val id: String, val title: String, val clientId: String?)
data class QuotePickerUiModel(val id: String, val quoteNumber: String, val clientId: String?, val jobId: String?, val status: String, val total: Double)

data class InvoiceLineItemFormUiModel(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0",
    val total: Double = 0.0,
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val unitPriceError: String? = null
)

data class InvoiceFormUiState(
    val id: String? = null,
    val invoiceNumber: String = "INV-0001",
    val clientId: String? = null,
    val jobId: String? = null,
    val quoteId: String? = null,
    val availableClients: List<InvoiceClientPickerUiModel> = emptyList(),
    val availableJobs: List<InvoiceJobPickerUiModel> = emptyList(),
    val availableQuotes: List<QuotePickerUiModel> = emptyList(),
    val status: String = InvoiceStatus.DRAFT,
    val issueDateText: String = LocalDate.now().toString(),
    val dueDateText: String = LocalDate.now().plusDays(14).toString(),
    val issueDate: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    val dueDate: Long? = LocalDate.now().plusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    val paidDate: Long? = null,
    val paymentMethod: String? = null,
    val lineItems: List<InvoiceLineItemFormUiModel> = listOf(InvoiceLineItemFormUiModel()),
    val discountAmount: String = "0",
    val taxRatePercent: String = "0",
    val taxAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val notes: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val invoiceNotFound: Boolean = false,
    val clientError: String? = null,
    val lineItemsError: String? = null,
    val dateError: String? = null,
    val discountError: String? = null,
    val taxRateError: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
) {
    val selectedClientName: String get() = availableClients.firstOrNull { it.id == clientId }?.name ?: "No client selected"
    val selectedJobTitle: String get() = availableJobs.firstOrNull { it.id == jobId }?.title ?: "No job selected"
    val selectedQuoteNumber: String get() = availableQuotes.firstOrNull { it.id == quoteId }?.quoteNumber ?: "No quote selected"
}

class InvoiceFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    private val businessRepository: BusinessRepository,
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val quoteRepository: QuoteRepository,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvoiceFormUiState())
    val uiState: StateFlow<InvoiceFormUiState> = _uiState.asStateFlow()
    private val zoneId = ZoneId.systemDefault()
    private var initializedCreate = false

    init {
        loadClientsJobsQuotes()
    }

    fun initializeForCreate(optionalClientId: String?, optionalJobId: String?, optionalQuoteId: String?) {
        if (initializedCreate || _uiState.value.isEditing) return
        initializedCreate = true
        viewModelScope.launch {
            runCatching {
                val profile = ensureBusinessProfile()
                val jobs = jobRepository.observeJobs().first()
                val quotes = quoteRepository.observeQuotes().first()
                val selectedQuote = optionalQuoteId?.let { id -> quotes.firstOrNull { it.id == id } }
                val selectedJob = selectedQuote?.jobId?.let { jobId -> jobs.firstOrNull { it.id == jobId } }
                    ?: optionalJobId?.let { id -> jobs.firstOrNull { it.id == id } }
                val selectedClientId = selectedQuote?.clientId ?: selectedJob?.clientId ?: optionalClientId
                _uiState.update {
                    recalculate(
                        it.copy(
                            invoiceNumber = nextInvoiceNumber(profile),
                            clientId = selectedClientId,
                            jobId = selectedQuote?.jobId ?: selectedJob?.id ?: optionalJobId,
                            quoteId = selectedQuote?.id ?: optionalQuoteId,
                            taxRatePercent = (profile.taxRatePercent).stripZero()
                        )
                    )
                }
                selectedQuote?.let { applyQuote(it) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to prepare invoice") }
            }
        }
    }

    fun initializeFromQuote(quoteId: String) = initializeForCreate(null, null, quoteId)

    fun loadInvoice(invoiceId: String) {
        if (_uiState.value.id == invoiceId && _uiState.value.isEditing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditing = true) }
            runCatching {
                val invoice = invoiceRepository.getInvoiceByIdOnce(invoiceId)
                val lineItems = lineItemRepository.observeLineItems(invoiceId, LineItemParentType.INVOICE).first()
                invoice to lineItems
            }.onSuccess { (invoice, items) ->
                if (invoice == null) {
                    _uiState.update { it.copy(isLoading = false, invoiceNotFound = true, errorMessage = "Invoice not found") }
                } else {
                    _uiState.update {
                        recalculate(
                            it.copy(
                                id = invoice.id,
                                invoiceNumber = invoice.invoiceNumber,
                                clientId = invoice.clientId,
                                jobId = invoice.jobId,
                                quoteId = invoice.quoteId,
                                status = invoice.status,
                                issueDateText = epochToDate(invoice.issueDate),
                                dueDateText = invoice.dueDate?.let(::epochToDate).orEmpty(),
                                issueDate = invoice.issueDate,
                                dueDate = invoice.dueDate,
                                paidDate = invoice.paidDate,
                                paymentMethod = invoice.paymentMethod,
                                lineItems = items.map { item -> item.toForm() }.ifEmpty { listOf(InvoiceLineItemFormUiModel()) },
                                discountAmount = invoice.discountAmount.stripZero(),
                                taxRatePercent = taxRateFromAmounts(invoice.subtotal, invoice.discountAmount, invoice.taxAmount).stripZero(),
                                notes = invoice.notes.orEmpty(),
                                isEditing = true,
                                isLoading = false,
                                invoiceNotFound = false
                            )
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load invoice") }
            }
        }
    }

    fun loadClientsJobsQuotes() {
        viewModelScope.launch {
            runCatching {
                Triple(clientRepository.observeClients().first(), jobRepository.observeJobs().first(), quoteRepository.observeQuotes().first())
            }.onSuccess { (clients, jobs, quotes) ->
                _uiState.update { state ->
                    state.copy(
                        availableClients = clients.map { InvoiceClientPickerUiModel(it.id, it.name) },
                        availableJobs = jobs.map { InvoiceJobPickerUiModel(it.id, it.title, it.clientId) },
                        availableQuotes = quotes.map { QuotePickerUiModel(it.id, it.quoteNumber, it.clientId, it.jobId, it.status, it.total) }
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to load clients, jobs, and quotes") }
            }
        }
    }

    fun onClientSelected(clientId: String?) {
        _uiState.update { state ->
            val jobId = state.jobId?.takeIf { id -> state.availableJobs.firstOrNull { it.id == id }?.clientId == clientId }
            val quoteId = state.quoteId?.takeIf { id -> state.availableQuotes.firstOrNull { it.id == id }?.clientId == clientId }
            recalculate(state.copy(clientId = clientId, jobId = jobId, quoteId = quoteId, clientError = null))
        }
    }

    fun onJobSelected(jobId: String?) {
        _uiState.update { state ->
            val job = state.availableJobs.firstOrNull { it.id == jobId }
            recalculate(state.copy(jobId = jobId, clientId = job?.clientId ?: state.clientId))
        }
    }

    fun onQuoteSelected(quoteId: String?) {
        if (quoteId == null) {
            _uiState.update { recalculate(it.copy(quoteId = null)) }
            return
        }
        viewModelScope.launch {
            runCatching {
                quoteRepository.getQuoteByIdOnce(quoteId)
            }.onSuccess { quote ->
                if (quote != null) applyQuote(quote) else _uiState.update { it.copy(errorMessage = "Quote not found") }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to load quote") }
            }
        }
    }

    fun onStatusChanged(status: String) = _uiState.update { it.copy(status = status) }
    fun onIssueDateChanged(value: String) = _uiState.update { recalculate(it.copy(issueDateText = value, dateError = null)) }
    fun onDueDateChanged(value: String) = _uiState.update { recalculate(it.copy(dueDateText = value, dateError = null)) }
    fun addLineItem() = _uiState.update { recalculate(it.copy(lineItems = it.lineItems + InvoiceLineItemFormUiModel())) }
    fun removeLineItem(lineItemId: String) = _uiState.update { recalculate(it.copy(lineItems = it.lineItems.filterNot { item -> item.id == lineItemId })) }
    fun onLineItemDescriptionChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(description = value, descriptionError = null) }
    fun onLineItemQuantityChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(quantity = value, quantityError = null) }
    fun onLineItemUnitPriceChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(unitPrice = value, unitPriceError = null) }
    fun onDiscountAmountChanged(value: String) = _uiState.update { recalculate(it.copy(discountAmount = value, discountError = null)) }
    fun onTaxRatePercentChanged(value: String) = _uiState.update { recalculate(it.copy(taxRatePercent = value, taxRateError = null)) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun recalculateTotals() = _uiState.update(::recalculate)
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun saveInvoice() {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.hasErrors()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val now = System.currentTimeMillis()
                val id = validated.id ?: UUID.randomUUID().toString()
                val issueDate = parseDate(validated.issueDateText).getOrThrow()
                val dueDate = validated.dueDateText.trim().takeIf { it.isNotBlank() }?.let { parseDate(it).getOrThrow() }
                val invoice = InvoiceEntity(
                    id = id,
                    clientId = validated.clientId,
                    jobId = validated.jobId,
                    quoteId = validated.quoteId,
                    invoiceNumber = validated.invoiceNumber,
                    status = validated.status,
                    issueDate = issueDate,
                    dueDate = dueDate,
                    paidDate = validated.paidDate,
                    paymentMethod = validated.paymentMethod,
                    subtotal = validated.subtotal,
                    discountAmount = validated.discountAmount.toDoubleOrNull() ?: 0.0,
                    taxAmount = validated.taxAmount,
                    total = validated.total,
                    notes = validated.notes.trim().ifBlank { null },
                    createdAt = if (validated.isEditing) invoiceRepository.getInvoiceByIdOnce(id)?.createdAt ?: now else now,
                    updatedAt = now
                )
                if (validated.isEditing) invoiceRepository.updateInvoice(invoice) else {
                    invoiceRepository.insertInvoice(invoice)
                    incrementInvoiceNumber()
                    activationTracker.trackFirst(
                        ActivationEvents.FIRST_INVOICE_CREATED,
                        mapOf(
                            ActivationParams.SOURCE_SCREEN to "invoice_form",
                            ActivationParams.HAS_CLIENT to (invoice.clientId != null).toString(),
                            ActivationParams.ITEM_COUNT to validated.lineItems.size.toString(),
                            ActivationParams.VALUE_BUCKET to AnalyticsValueBuckets.fromAmount(invoice.total),
                            ActivationParams.CURRENCY to ensureBusinessProfile().currencyCode
                        )
                    )
                }
                lineItemRepository.deleteLineItemsForParent(id, LineItemParentType.INVOICE)
                lineItemRepository.insertLineItems(validated.lineItems.mapIndexed { index, item ->
                    val quantity = item.quantity.toDoubleOrNull() ?: 0.0
                    val unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0
                    LineItemEntity(
                        id = UUID.randomUUID().toString(),
                        parentId = id,
                        parentType = LineItemParentType.INVOICE,
                        description = item.description.trim(),
                        quantity = quantity,
                        unitPrice = unitPrice,
                        total = quantity * unitPrice,
                        sortOrder = index
                    )
                })
                id
            }.onSuccess { savedId ->
                _uiState.update { it.copy(id = savedId, isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Unable to save invoice") }
            }
        }
    }

    private suspend fun applyQuote(quote: QuoteEntity) {
        val quoteItems = lineItemRepository.observeLineItems(quote.id, LineItemParentType.QUOTE).first()
        val taxRate = taxRateFromAmounts(quote.subtotal, quote.discountAmount, quote.taxAmount)
        _uiState.update { state ->
            recalculate(
                state.copy(
                    clientId = quote.clientId ?: state.clientId,
                    jobId = quote.jobId ?: state.jobId,
                    quoteId = quote.id,
                    lineItems = quoteItems.map { it.toForm() }.ifEmpty { state.lineItems },
                    discountAmount = quote.discountAmount.stripZero(),
                    taxRatePercent = taxRate.stripZero(),
                    notes = quote.notes.orEmpty()
                )
            )
        }
    }

    private fun updateItem(lineItemId: String, transform: (InvoiceLineItemFormUiModel) -> InvoiceLineItemFormUiModel) {
        _uiState.update { state -> recalculate(state.copy(lineItems = state.lineItems.map { if (it.id == lineItemId) transform(it) else it })) }
    }

    private fun recalculate(state: InvoiceFormUiState): InvoiceFormUiState {
        val items = state.lineItems.map {
            val quantity = it.quantity.toDoubleOrNull() ?: 0.0
            val unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0
            it.copy(total = quantity * unitPrice)
        }
        val subtotal = items.sumOf { it.total }
        val discount = state.discountAmount.toDoubleOrNull() ?: 0.0
        val taxRate = state.taxRatePercent.toDoubleOrNull() ?: 0.0
        val taxable = (subtotal - discount).coerceAtLeast(0.0)
        val tax = taxable * taxRate / 100.0
        return state.copy(lineItems = items, subtotal = subtotal, taxAmount = tax, total = taxable + tax)
    }

    private fun validate(state: InvoiceFormUiState): InvoiceFormUiState {
        var lineError: String? = null
        val items = state.lineItems.map { item ->
            val descError = if (item.description.trim().isBlank()) "Description is required" else null
            val qty = item.quantity.toDoubleOrNull()
            val qtyError = if (qty == null || qty <= 0.0) "Quantity must be positive" else null
            val price = item.unitPrice.toDoubleOrNull()
            val priceError = if (price == null || price < 0.0) "Enter a valid price" else null
            if (descError != null || qtyError != null || priceError != null) lineError = "Add at least one valid line item"
            item.copy(descriptionError = descError, quantityError = qtyError, unitPriceError = priceError)
        }
        if (items.isEmpty()) lineError = "Add at least one valid line item"
        val discount = state.discountAmount.toDoubleOrNull()
        val taxRate = state.taxRatePercent.toDoubleOrNull()
        val issue = parseDate(state.issueDateText)
        val due = state.dueDateText.trim().takeIf { it.isNotBlank() }?.let { parseDate(it) }
        val dateError = when {
            issue.isFailure -> "Use issue date YYYY-MM-DD"
            due?.isFailure == true -> "Use due date YYYY-MM-DD"
            due?.getOrNull()?.let { it < issue.getOrThrow() } == true -> "Due date cannot be before issue date"
            else -> null
        }
        return recalculate(
            state.copy(
                lineItems = items,
                lineItemsError = lineError,
                discountError = when {
                    discount == null || discount < 0.0 -> "Enter a valid discount"
                    discount > state.subtotal -> "Discount cannot exceed subtotal"
                    else -> null
                },
                taxRateError = if (taxRate == null || taxRate < 0.0) "Enter a valid tax rate" else null,
                dateError = dateError
            )
        )
    }

    private fun InvoiceFormUiState.hasErrors(): Boolean =
        lineItemsError != null || discountError != null || taxRateError != null || dateError != null

    private fun parseDate(value: String): Result<Long> = try {
        Result.success(LocalDate.parse(value).atStartOfDay(zoneId).toInstant().toEpochMilli())
    } catch (_: DateTimeParseException) {
        Result.failure(IllegalArgumentException("Use YYYY-MM-DD"))
    }

    private fun epochToDate(value: Long): String = Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate().toString()

    private suspend fun ensureBusinessProfile(): BusinessProfileEntity {
        val existing = businessRepository.getBusinessProfileOnce()
        if (existing != null) return existing
        val fallback = BusinessProfileEntity(businessName = "ServiceSphere Business")
        businessRepository.upsertBusinessProfile(fallback)
        return fallback
    }

    private fun nextInvoiceNumber(profile: BusinessProfileEntity): String {
        val prefix = profile.invoicePrefix.ifBlank { "INV-" }
        return "$prefix${profile.nextInvoiceNumber.toString().padStart(4, '0')}"
    }

    private suspend fun incrementInvoiceNumber() {
        val profile = ensureBusinessProfile()
        businessRepository.updateBusinessProfile(profile.copy(nextInvoiceNumber = profile.nextInvoiceNumber + 1, updatedAt = System.currentTimeMillis()))
    }

    class Factory(
        private val invoiceRepository: InvoiceRepository,
        private val lineItemRepository: LineItemRepository,
        private val businessRepository: BusinessRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InvoiceFormViewModel(invoiceRepository, lineItemRepository, businessRepository, clientRepository, jobRepository, quoteRepository, activationTracker) as T
    }
}

private fun LineItemEntity.toForm(): InvoiceLineItemFormUiModel = InvoiceLineItemFormUiModel(
    description = description,
    quantity = quantity.stripZero(),
    unitPrice = unitPrice.stripZero(),
    total = total
)

private fun taxRateFromAmounts(subtotal: Double, discount: Double, tax: Double): Double {
    val taxable = subtotal - discount
    return if (taxable <= 0.0) 0.0 else tax / taxable * 100.0
}

private fun Double.stripZero(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

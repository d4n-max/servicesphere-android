package com.servicesphere.ui.screens.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.activation.ActivationTracker
import com.servicesphere.activation.AnalyticsValueBuckets
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.LineItemParentType
import com.servicesphere.domain.model.QuoteStatus
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

data class QuoteClientPickerUiModel(val id: String, val name: String)
data class JobPickerUiModel(val id: String, val title: String, val clientId: String?)

data class LineItemFormUiModel(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0",
    val total: Double = 0.0,
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val unitPriceError: String? = null
)

data class QuoteFormUiState(
    val id: String? = null,
    val quoteNumber: String = "Q-0001",
    val clientId: String? = null,
    val jobId: String? = null,
    val availableClients: List<QuoteClientPickerUiModel> = emptyList(),
    val availableJobs: List<JobPickerUiModel> = emptyList(),
    val status: String = QuoteStatus.DRAFT,
    val issueDateText: String = LocalDate.now().toString(),
    val validUntilText: String = "",
    val issueDate: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    val validUntil: Long? = null,
    val lineItems: List<LineItemFormUiModel> = listOf(LineItemFormUiModel()),
    val discountAmount: String = "0",
    val taxRatePercent: String = "0",
    val taxAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val notes: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val quoteNotFound: Boolean = false,
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
}

class QuoteFormViewModel(
    private val quoteRepository: QuoteRepository,
    private val lineItemRepository: LineItemRepository,
    private val businessRepository: BusinessRepository,
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val activationTracker: ActivationTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuoteFormUiState())
    val uiState: StateFlow<QuoteFormUiState> = _uiState.asStateFlow()
    private val zoneId = ZoneId.systemDefault()
    private var initializedCreate = false

    init {
        loadClientsAndJobs()
    }

    fun initializeForCreate(optionalClientId: String?, optionalJobId: String?) {
        if (initializedCreate || _uiState.value.isEditing) return
        initializedCreate = true
        viewModelScope.launch {
            val profile = businessRepository.getBusinessProfileOnce()
            val jobs = jobRepository.observeJobs().first()
            val selectedJob = optionalJobId?.let { id -> jobs.firstOrNull { it.id == id } }
            val selectedClientId = selectedJob?.clientId ?: optionalClientId
            _uiState.update {
                recalculate(
                    it.copy(
                        quoteNumber = nextQuoteNumber(profile),
                        clientId = selectedClientId,
                        jobId = selectedJob?.id ?: optionalJobId,
                        taxRatePercent = (profile?.taxRatePercent ?: 0.0).stripZero()
                    )
                )
            }
        }
    }

    fun loadClientsAndJobs() {
        viewModelScope.launch {
            runCatching {
                combinePickers(clientRepository.observeClients().first(), jobRepository.observeJobs().first())
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to load clients and jobs") }
            }
        }
    }

    fun loadQuote(quoteId: String) {
        if (_uiState.value.id == quoteId && _uiState.value.isEditing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditing = true) }
            runCatching {
                val quote = quoteRepository.getQuoteByIdOnce(quoteId)
                val lineItems = lineItemRepository.observeLineItems(quoteId, LineItemParentType.QUOTE).first()
                quote to lineItems
            }.onSuccess { (quote, items) ->
                if (quote == null) {
                    _uiState.update { it.copy(isLoading = false, quoteNotFound = true, errorMessage = "Quote not found") }
                } else {
                    _uiState.update {
                        recalculate(
                            it.copy(
                                id = quote.id,
                                quoteNumber = quote.quoteNumber,
                                clientId = quote.clientId,
                                jobId = quote.jobId,
                                status = quote.status,
                                issueDateText = epochToDate(quote.issueDate),
                                validUntilText = quote.validUntil?.let(::epochToDate).orEmpty(),
                                issueDate = quote.issueDate,
                                validUntil = quote.validUntil,
                                lineItems = items.map { item -> item.toForm() }.ifEmpty { listOf(LineItemFormUiModel()) },
                                discountAmount = quote.discountAmount.stripZero(),
                                taxRatePercent = taxRateFromAmounts(quote).stripZero(),
                                notes = quote.notes.orEmpty(),
                                isEditing = true,
                                isLoading = false,
                                quoteNotFound = false
                            )
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load quote") }
            }
        }
    }

    fun onClientSelected(clientId: String?) {
        _uiState.update { state ->
            recalculate(state.copy(clientId = clientId, jobId = state.jobId?.takeIf { jobId -> state.availableJobs.firstOrNull { it.id == jobId }?.clientId == clientId }))
        }
    }

    fun onJobSelected(jobId: String?) {
        _uiState.update { state ->
            val job = state.availableJobs.firstOrNull { it.id == jobId }
            recalculate(state.copy(jobId = jobId, clientId = job?.clientId ?: state.clientId))
        }
    }

    fun onStatusChanged(status: String) = _uiState.update { it.copy(status = status) }
    fun onIssueDateChanged(value: String) = _uiState.update { recalculate(it.copy(issueDateText = value, dateError = null)) }
    fun onValidUntilChanged(value: String) = _uiState.update { recalculate(it.copy(validUntilText = value, dateError = null)) }
    fun onDiscountAmountChanged(value: String) = _uiState.update { recalculate(it.copy(discountAmount = value, discountError = null)) }
    fun onTaxRatePercentChanged(value: String) = _uiState.update { recalculate(it.copy(taxRatePercent = value, taxRateError = null)) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun addLineItem() = _uiState.update { recalculate(it.copy(lineItems = it.lineItems + LineItemFormUiModel())) }
    fun removeLineItem(lineItemId: String) = _uiState.update { recalculate(it.copy(lineItems = it.lineItems.filterNot { item -> item.id == lineItemId })) }
    fun onLineItemDescriptionChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(description = value, descriptionError = null) }
    fun onLineItemQuantityChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(quantity = value, quantityError = null) }
    fun onLineItemUnitPriceChanged(lineItemId: String, value: String) = updateItem(lineItemId) { it.copy(unitPrice = value, unitPriceError = null) }
    fun recalculateTotals() = _uiState.update(::recalculate)
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun saveQuote() {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.hasErrors()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val now = System.currentTimeMillis()
                val id = validated.id ?: UUID.randomUUID().toString()
                val issueDate = parseDate(validated.issueDateText).getOrThrow()
                val validUntil = validated.validUntilText.trim().takeIf { it.isNotBlank() }?.let { parseDate(it).getOrThrow() }
                val quote = QuoteEntity(
                    id = id,
                    clientId = validated.clientId,
                    jobId = validated.jobId,
                    quoteNumber = validated.quoteNumber,
                    status = validated.status,
                    issueDate = issueDate,
                    validUntil = validUntil,
                    subtotal = validated.subtotal,
                    discountAmount = validated.discountAmount.toDoubleOrNull() ?: 0.0,
                    taxAmount = validated.taxAmount,
                    total = validated.total,
                    notes = validated.notes.trim().ifBlank { null },
                    createdAt = if (validated.isEditing) quoteRepository.getQuoteByIdOnce(id)?.createdAt ?: now else now,
                    updatedAt = now
                )
                if (validated.isEditing) quoteRepository.updateQuote(quote) else {
                    quoteRepository.insertQuote(quote)
                    incrementQuoteNumber()
                    activationTracker.trackFirst(
                        ActivationEvents.FIRST_QUOTE_CREATED,
                        mapOf(
                            ActivationParams.SOURCE_SCREEN to "quote_form",
                            ActivationParams.HAS_CLIENT to (quote.clientId != null).toString(),
                            ActivationParams.ITEM_COUNT to validated.lineItems.size.toString(),
                            ActivationParams.VALUE_BUCKET to AnalyticsValueBuckets.fromAmount(quote.total),
                            ActivationParams.CURRENCY to (businessRepository.getBusinessProfileOnce()?.currencyCode ?: "USD")
                        )
                    )
                }
                lineItemRepository.deleteLineItemsForParent(id, LineItemParentType.QUOTE)
                lineItemRepository.insertLineItems(validated.lineItems.mapIndexed { index, item ->
                    val quantity = item.quantity.toDoubleOrNull() ?: 0.0
                    val unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0
                    LineItemEntity(
                        id = UUID.randomUUID().toString(),
                        parentId = id,
                        parentType = LineItemParentType.QUOTE,
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
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Unable to save quote") }
            }
        }
    }

    private fun combinePickers(clients: List<ClientEntity>, jobs: List<JobEntity>) {
        _uiState.update { state ->
            state.copy(
                availableClients = clients.map { QuoteClientPickerUiModel(it.id, it.name) },
                availableJobs = jobs.map { JobPickerUiModel(it.id, it.title, it.clientId) }
            )
        }
    }

    private fun updateItem(lineItemId: String, transform: (LineItemFormUiModel) -> LineItemFormUiModel) {
        _uiState.update { state -> recalculate(state.copy(lineItems = state.lineItems.map { if (it.id == lineItemId) transform(it) else it })) }
    }

    private fun recalculate(state: QuoteFormUiState): QuoteFormUiState {
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

    private fun validate(state: QuoteFormUiState): QuoteFormUiState {
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
        val validUntil = state.validUntilText.trim().takeIf { it.isNotBlank() }?.let { parseDate(it) }
        val dateError = when {
            issue.isFailure -> "Use issue date YYYY-MM-DD"
            validUntil?.isFailure == true -> "Use valid-until date YYYY-MM-DD"
            validUntil?.getOrNull()?.let { it < issue.getOrThrow() } == true -> "Valid until cannot be before issue date"
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

    private fun QuoteFormUiState.hasErrors(): Boolean =
        lineItemsError != null || discountError != null || taxRateError != null || dateError != null

    private fun parseDate(value: String): Result<Long> = try {
        Result.success(LocalDate.parse(value).atStartOfDay(zoneId).toInstant().toEpochMilli())
    } catch (_: DateTimeParseException) {
        Result.failure(IllegalArgumentException("Use YYYY-MM-DD"))
    }

    private fun epochToDate(value: Long): String = Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate().toString()

    private fun nextQuoteNumber(profile: BusinessProfileEntity?): String {
        val prefix = profile?.quotePrefix?.ifBlank { null } ?: "Q-"
        val next = profile?.nextQuoteNumber ?: 1
        return "$prefix${next.toString().padStart(4, '0')}"
    }

    private suspend fun incrementQuoteNumber() {
        val profile = businessRepository.getBusinessProfileOnce() ?: return
        businessRepository.updateBusinessProfile(profile.copy(nextQuoteNumber = profile.nextQuoteNumber + 1, updatedAt = System.currentTimeMillis()))
    }

    private fun taxRateFromAmounts(quote: QuoteEntity): Double {
        val taxable = quote.subtotal - quote.discountAmount
        return if (taxable <= 0.0) 0.0 else quote.taxAmount / taxable * 100.0
    }

    class Factory(
        private val quoteRepository: QuoteRepository,
        private val lineItemRepository: LineItemRepository,
        private val businessRepository: BusinessRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val activationTracker: ActivationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuoteFormViewModel(quoteRepository, lineItemRepository, businessRepository, clientRepository, jobRepository, activationTracker) as T
    }
}

private fun LineItemEntity.toForm(): LineItemFormUiModel = LineItemFormUiModel(
    description = description,
    quantity = quantity.stripZero(),
    unitPrice = unitPrice.stripZero(),
    total = total
)

private fun Double.stripZero(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

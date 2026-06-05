package com.servicesphere.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.BusinessLogoStorage
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.repository.BusinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessProfileUiState(
    val isLoading: Boolean = true,
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val taxNumber: String = "",
    val logoUri: String? = null,
    val currencyCode: String = "USD",
    val taxRatePercent: String = "0",
    val paymentInstructions: String = "",
    val quotePrefix: String = "Q-",
    val invoicePrefix: String = "INV-",
    val nextQuoteNumber: String = "1",
    val nextInvoiceNumber: String = "1",
    val businessNameError: String? = null,
    val emailError: String? = null,
    val websiteError: String? = null,
    val taxRateError: String? = null,
    val nextQuoteNumberError: String? = null,
    val nextInvoiceNumberError: String? = null,
    val prefixError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val quotePreview: String get() = "${quotePrefix.ifBlank { "Q-" }}${(nextQuoteNumber.toIntOrNull() ?: 1).toString().padStart(4, '0')}"
    val invoicePreview: String get() = "${invoicePrefix.ifBlank { "INV-" }}${(nextInvoiceNumber.toIntOrNull() ?: 1).toString().padStart(4, '0')}"
}

class BusinessProfileViewModel(
    private val repository: BusinessRepository,
    private val logoStorage: BusinessLogoStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()
    private var loadedProfile: BusinessProfileEntity? = null

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { repository.createDefaultBusinessProfileIfMissing() }
                .onSuccess { profile ->
                    loadedProfile = profile
                    _uiState.value = profile.toUiState()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load business profile") }
                }
        }
    }

    fun onBusinessNameChanged(value: String) = _uiState.update { it.copy(businessName = value, businessNameError = null) }
    fun onOwnerNameChanged(value: String) = _uiState.update { it.copy(ownerName = value) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value) }
    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onAddressChanged(value: String) = _uiState.update { it.copy(address = value) }
    fun onWebsiteChanged(value: String) = _uiState.update { it.copy(website = value, websiteError = null) }
    fun onTaxNumberChanged(value: String) = _uiState.update { it.copy(taxNumber = value) }
    fun onCurrencyCodeChanged(value: String) = _uiState.update { it.copy(currencyCode = value.uppercase().take(3)) }
    fun onTaxRatePercentChanged(value: String) = _uiState.update { it.copy(taxRatePercent = value, taxRateError = null) }
    fun onPaymentInstructionsChanged(value: String) = _uiState.update { it.copy(paymentInstructions = value) }
    fun onQuotePrefixChanged(value: String) = _uiState.update { it.copy(quotePrefix = value, prefixError = null) }
    fun onInvoicePrefixChanged(value: String) = _uiState.update { it.copy(invoicePrefix = value, prefixError = null) }
    fun onNextQuoteNumberChanged(value: String) = _uiState.update { it.copy(nextQuoteNumber = value, nextQuoteNumberError = null) }
    fun onNextInvoiceNumberChanged(value: String) = _uiState.update { it.copy(nextInvoiceNumber = value, nextInvoiceNumberError = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun onLogoSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            runCatching { logoStorage.copyLogoToAppStorage(uri) }
                .onSuccess { copiedUri -> _uiState.update { it.copy(logoUri = copiedUri) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Couldn't save logo") } }
        }
    }

    fun removeLogo() {
        val logo = _uiState.value.logoUri
        if (logo != null) viewModelScope.launch { logoStorage.deleteStoredLogo(logo) }
        _uiState.update { it.copy(logoUri = null) }
    }

    fun saveProfile() {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.hasErrors()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            runCatching {
                val now = System.currentTimeMillis()
                val existing = loadedProfile ?: repository.createDefaultBusinessProfileIfMissing()
                val profile = existing.copy(
                    businessName = validated.businessName.trim(),
                    ownerName = validated.ownerName.trim().ifBlank { null },
                    phone = validated.phone.trim().ifBlank { null },
                    email = validated.email.trim().ifBlank { null },
                    address = validated.address.trim().ifBlank { null },
                    website = validated.website.trim().ifBlank { null },
                    taxNumber = validated.taxNumber.trim().ifBlank { null },
                    logoUri = validated.logoUri,
                    currencyCode = validated.currencyCode.trim().uppercase().ifBlank { "USD" },
                    taxRatePercent = validated.taxRatePercent.toDoubleOrNull() ?: 0.0,
                    paymentInstructions = validated.paymentInstructions.trim().ifBlank { null },
                    quotePrefix = validated.quotePrefix.trim().ifBlank { "Q-" },
                    invoicePrefix = validated.invoicePrefix.trim().ifBlank { "INV-" },
                    nextQuoteNumber = validated.nextQuoteNumber.toIntOrNull() ?: 1,
                    nextInvoiceNumber = validated.nextInvoiceNumber.toIntOrNull() ?: 1,
                    updatedAt = now
                )
                repository.upsertBusinessProfile(profile)
                profile
            }.onSuccess { profile ->
                loadedProfile = profile
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't save settings") }
            }
        }
    }

    private fun validate(state: BusinessProfileUiState): BusinessProfileUiState {
        val email = state.email.trim()
        val website = state.website.trim()
        val taxRate = state.taxRatePercent.toDoubleOrNull()
        val quoteNumber = state.nextQuoteNumber.toIntOrNull()
        val invoiceNumber = state.nextInvoiceNumber.toIntOrNull()
        return state.copy(
            businessNameError = if (state.businessName.trim().isBlank()) "Business name is required" else null,
            emailError = if (email.isNotBlank() && !email.contains("@")) "Enter a valid email address" else null,
            websiteError = if (website.isNotBlank() && !website.contains(".")) "Enter a valid website or URL" else null,
            taxRateError = if (taxRate == null || taxRate < 0.0) "Tax rate must be zero or higher" else null,
            nextQuoteNumberError = if (quoteNumber == null || quoteNumber <= 0) "Next quote number must be a positive number" else null,
            nextInvoiceNumberError = if (invoiceNumber == null || invoiceNumber <= 0) "Next invoice number must be a positive number" else null,
            prefixError = when {
                state.quotePrefix.trim().isBlank() -> "Quote prefix cannot be blank"
                state.invoicePrefix.trim().isBlank() -> "Invoice prefix cannot be blank"
                else -> null
            }
        )
    }

    private fun BusinessProfileUiState.hasErrors(): Boolean =
        businessNameError != null || emailError != null || websiteError != null || taxRateError != null ||
            nextQuoteNumberError != null || nextInvoiceNumberError != null || prefixError != null

    class Factory(
        private val repository: BusinessRepository,
        private val logoStorage: BusinessLogoStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BusinessProfileViewModel(repository, logoStorage) as T
    }
}

private fun BusinessProfileEntity.toUiState(): BusinessProfileUiState = BusinessProfileUiState(
    isLoading = false,
    businessName = businessName,
    ownerName = ownerName.orEmpty(),
    phone = phone.orEmpty(),
    email = email.orEmpty(),
    address = address.orEmpty(),
    website = website.orEmpty(),
    taxNumber = taxNumber.orEmpty(),
    logoUri = logoUri,
    currencyCode = currencyCode,
    taxRatePercent = taxRatePercent.stripZero(),
    paymentInstructions = paymentInstructions.orEmpty(),
    quotePrefix = quotePrefix,
    invoicePrefix = invoicePrefix,
    nextQuoteNumber = nextQuoteNumber.toString(),
    nextInvoiceNumber = nextInvoiceNumber.toString()
)

private fun Double.stripZero(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

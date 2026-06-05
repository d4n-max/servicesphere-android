package com.servicesphere.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.preferences.UserPreferences
import com.servicesphere.data.repository.BusinessRepository
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val tradeTypeOptions = listOf(
    "Handyman",
    "Cleaning",
    "Plumbing",
    "Electrical",
    "HVAC",
    "Landscaping",
    "Appliance Repair",
    "Locksmith",
    "Other"
)

val currencyQuickOptions = listOf("USD", "EUR", "RON", "GBP", "CAD", "AUD")

data class BusinessSetupUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    val businessName: String = "",
    val ownerName: String = "",
    val tradeType: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val currencyCode: String = defaultCurrencyCode(),
    val taxRatePercent: String = "0",
    val quotePrefix: String = "Q-",
    val invoicePrefix: String = "INV-",
    val paymentInstructions: String = "",
    val useDemoData: Boolean = false,
    val isSaving: Boolean = false,
    val businessNameError: String? = null,
    val tradeTypeError: String? = null,
    val emailError: String? = null,
    val websiteError: String? = null,
    val taxRateError: String? = null,
    val prefixError: String? = null,
    val errorMessage: String? = null,
    val setupComplete: Boolean = false
)

class BusinessSetupViewModel(
    private val businessRepository: BusinessRepository,
    private val preferences: UserPreferences,
    private val seedDemoData: suspend () -> Unit
) : ViewModel() {
    private val _uiState = MutableStateFlow(BusinessSetupUiState())
    val uiState: StateFlow<BusinessSetupUiState> = _uiState.asStateFlow()
    private var loadedProfile: BusinessProfileEntity? = null

    init {
        loadExistingSetup()
    }

    private fun loadExistingSetup() {
        viewModelScope.launch {
            runCatching {
                val profile = businessRepository.getBusinessProfileOnce()
                val tradeType = preferences.selectedTradeType.first().orEmpty()
                loadedProfile = profile
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            businessName = profile.businessName,
                            ownerName = profile.ownerName.orEmpty(),
                            phone = profile.phone.orEmpty(),
                            email = profile.email.orEmpty(),
                            address = profile.address.orEmpty(),
                            website = profile.website.orEmpty(),
                            currencyCode = profile.currencyCode.ifBlank { defaultCurrencyCode() },
                            taxRatePercent = profile.taxRatePercent.stripZero(),
                            quotePrefix = profile.quotePrefix,
                            invoicePrefix = profile.invoicePrefix,
                            paymentInstructions = profile.paymentInstructions.orEmpty(),
                            tradeType = tradeType.ifBlank { it.tradeType }
                        )
                    }
                } else if (tradeType.isNotBlank()) {
                    _uiState.update { it.copy(tradeType = tradeType) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Couldn't load setup details") }
            }
        }
    }

    fun onBusinessNameChanged(value: String) = _uiState.update { it.copy(businessName = value, businessNameError = null) }
    fun onOwnerNameChanged(value: String) = _uiState.update { it.copy(ownerName = value) }
    fun onTradeTypeSelected(value: String) = _uiState.update { it.copy(tradeType = value, tradeTypeError = null) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value) }
    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onAddressChanged(value: String) = _uiState.update { it.copy(address = value) }
    fun onWebsiteChanged(value: String) = _uiState.update { it.copy(website = value, websiteError = null) }
    fun onCurrencyCodeChanged(value: String) = _uiState.update { it.copy(currencyCode = value.uppercase().take(3)) }
    fun onTaxRatePercentChanged(value: String) = _uiState.update { it.copy(taxRatePercent = value, taxRateError = null) }
    fun onQuotePrefixChanged(value: String) = _uiState.update { it.copy(quotePrefix = value, prefixError = null) }
    fun onInvoicePrefixChanged(value: String) = _uiState.update { it.copy(invoicePrefix = value, prefixError = null) }
    fun onPaymentInstructionsChanged(value: String) = _uiState.update { it.copy(paymentInstructions = value) }
    fun onUseDemoDataChanged(value: Boolean) = _uiState.update { it.copy(useDemoData = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun nextStep() {
        val validated = validateStep(_uiState.value)
        _uiState.value = validated
        if (validated.hasStepErrors()) return
        if (validated.currentStep < validated.totalSteps) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
    }

    fun skipOptionalStepIfAllowed() {
        nextStep()
    }

    fun finishSetup() {
        val validated = validateAll(_uiState.value)
        _uiState.value = validated
        if (validated.hasAnyErrors()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val existing = loadedProfile ?: businessRepository.getBusinessProfileOnce()
                val profile = existing?.copy(
                    businessName = validated.businessName.trim(),
                    ownerName = validated.ownerName.trim().ifBlank { null },
                    phone = validated.phone.trim().ifBlank { null },
                    email = validated.email.trim().ifBlank { null },
                    address = validated.address.trim().ifBlank { null },
                    website = validated.website.trim().ifBlank { null },
                    currencyCode = validated.currencyCode.trim().uppercase().ifBlank { defaultCurrencyCode() },
                    taxRatePercent = validated.taxRatePercent.toDoubleOrNull() ?: 0.0,
                    paymentInstructions = validated.paymentInstructions.trim().ifBlank { null },
                    quotePrefix = validated.quotePrefix.trim().ifBlank { "Q-" },
                    invoicePrefix = validated.invoicePrefix.trim().ifBlank { "INV-" },
                    updatedAt = now
                ) ?: BusinessProfileEntity(
                    businessName = validated.businessName.trim(),
                    ownerName = validated.ownerName.trim().ifBlank { null },
                    phone = validated.phone.trim().ifBlank { null },
                    email = validated.email.trim().ifBlank { null },
                    address = validated.address.trim().ifBlank { null },
                    website = validated.website.trim().ifBlank { null },
                    currencyCode = validated.currencyCode.trim().uppercase().ifBlank { defaultCurrencyCode() },
                    taxRatePercent = validated.taxRatePercent.toDoubleOrNull() ?: 0.0,
                    paymentInstructions = validated.paymentInstructions.trim().ifBlank { null },
                    quotePrefix = validated.quotePrefix.trim().ifBlank { "Q-" },
                    invoicePrefix = validated.invoicePrefix.trim().ifBlank { "INV-" },
                    createdAt = now,
                    updatedAt = now
                )
                businessRepository.upsertBusinessProfile(profile)
                loadedProfile = profile
                preferences.markBusinessSetupComplete(validated.tradeType.trim().ifBlank { "Other" })
                if (validated.useDemoData) seedDemoData()
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, setupComplete = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Couldn't complete setup") }
            }
        }
    }

    private fun validateStep(state: BusinessSetupUiState): BusinessSetupUiState = when (state.currentStep) {
        1 -> state.copy(
            businessNameError = if (state.businessName.trim().isBlank()) "Business name is required" else null,
            tradeTypeError = if (state.tradeType.trim().isBlank()) "Choose a trade type or select Other" else null
        )
        2 -> validateContact(state)
        3 -> validateDocuments(state)
        else -> state
    }

    private fun validateAll(state: BusinessSetupUiState): BusinessSetupUiState =
        validateDocuments(validateContact(state)).copy(
            businessNameError = if (state.businessName.trim().isBlank()) "Business name is required" else null,
            tradeTypeError = if (state.tradeType.trim().isBlank()) "Choose a trade type or select Other" else null
        )

    private fun validateContact(state: BusinessSetupUiState): BusinessSetupUiState {
        val email = state.email.trim()
        val website = state.website.trim()
        return state.copy(
            emailError = if (email.isNotBlank() && !email.contains("@")) "Enter a valid email address" else null,
            websiteError = if (website.isNotBlank() && !website.contains(".")) "Enter a valid website or URL" else null
        )
    }

    private fun validateDocuments(state: BusinessSetupUiState): BusinessSetupUiState {
        val taxRate = state.taxRatePercent.toDoubleOrNull()
        return state.copy(
            taxRateError = if (taxRate == null || taxRate < 0.0) "Tax rate must be zero or higher" else null,
            prefixError = when {
                state.quotePrefix.trim().isBlank() -> "Quote prefix cannot be blank"
                state.invoicePrefix.trim().isBlank() -> "Invoice prefix cannot be blank"
                else -> null
            }
        )
    }

    private fun BusinessSetupUiState.hasStepErrors(): Boolean = when (currentStep) {
        1 -> businessNameError != null || tradeTypeError != null
        2 -> emailError != null || websiteError != null
        3 -> taxRateError != null || prefixError != null
        else -> false
    }

    private fun BusinessSetupUiState.hasAnyErrors(): Boolean =
        businessNameError != null || tradeTypeError != null || emailError != null ||
            websiteError != null || taxRateError != null || prefixError != null

    class Factory(
        private val businessRepository: BusinessRepository,
        private val preferences: UserPreferences,
        private val seedDemoData: suspend () -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BusinessSetupViewModel(businessRepository, preferences, seedDemoData) as T
    }
}

private fun defaultCurrencyCode(): String = runCatching {
    Currency.getInstance(Locale.getDefault()).currencyCode
}.getOrDefault("USD")

private fun Double.stripZero(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

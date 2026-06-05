package com.servicesphere.billing

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.servicesphere.BuildConfig
import com.servicesphere.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.revenuecat.purchases.Package as RevenueCatPackage

private const val PRO_ENTITLEMENT_ID = "pro"
private const val NOT_CONFIGURED_MESSAGE = "Subscriptions are not configured for this build."

class SubscriptionRepository(
    private val preferences: UserPreferences,
    private val revenueCatManager: RevenueCatManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastRevenueCatState = SubscriptionState(
        source = if (revenueCatManager.isConfigured) "revenuecat" else "not_configured",
        errorMessage = if (revenueCatManager.isConfigured) null else NOT_CONFIGURED_MESSAGE
    )
    private var debugPro = false

    private val _subscriptionState = MutableStateFlow(lastRevenueCatState)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _offeringsState = MutableStateFlow(
        OfferingsUiState(
            errorMessage = if (revenueCatManager.isConfigured) null else NOT_CONFIGURED_MESSAGE
        )
    )
    val offeringsState: StateFlow<OfferingsUiState> = _offeringsState

    val debugProEnabled: StateFlow<Boolean> = preferences.debugProEnabled
        .onEach {
            debugPro = BuildConfig.DEBUG && it
            publishSubscriptionState()
        }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            if (!revenueCatManager.isConfigured) {
                lastRevenueCatState = SubscriptionState(
                    source = "not_configured",
                    errorMessage = NOT_CONFIGURED_MESSAGE
                )
                publishSubscriptionState()
                _offeringsState.value = OfferingsUiState(errorMessage = NOT_CONFIGURED_MESSAGE)
                return@launch
            }

            _subscriptionState.value = _subscriptionState.value.copy(isLoading = true, errorMessage = null)
            when (val result = revenueCatManager.getCustomerInfo()) {
                is BillingResult.Success -> {
                    lastRevenueCatState = result.value.toSubscriptionState()
                    publishSubscriptionState()
                }
                is BillingResult.Error -> {
                    lastRevenueCatState = SubscriptionState(source = "revenuecat", errorMessage = result.message)
                    publishSubscriptionState()
                }
                BillingResult.Cancelled -> publishSubscriptionState()
            }
        }
    }

    suspend fun loadOfferings() {
        if (!revenueCatManager.isConfigured) {
            _offeringsState.value = OfferingsUiState(errorMessage = NOT_CONFIGURED_MESSAGE)
            return
        }
        _offeringsState.value = _offeringsState.value.copy(isLoading = true, errorMessage = null)
        when (val result = revenueCatManager.getPackages()) {
            is BillingResult.Success -> {
                _offeringsState.value = OfferingsUiState(
                    packages = result.value,
                    errorMessage = if (result.value.isEmpty()) "No subscription packages are configured." else null
                )
            }
            is BillingResult.Error -> {
                _offeringsState.value = OfferingsUiState(errorMessage = result.message)
            }
            BillingResult.Cancelled -> {
                _offeringsState.value = _offeringsState.value.copy(isLoading = false)
            }
        }
    }

    suspend fun purchase(activity: Activity, packageToPurchase: RevenueCatPackage): BillingResult<SubscriptionState> {
        val result = revenueCatManager.purchase(activity, packageToPurchase)
        return result.toStateResult()
    }

    suspend fun restorePurchases(): BillingResult<SubscriptionState> {
        val result = revenueCatManager.restore()
        return result.toStateResult()
    }

    suspend fun setDebugProEnabled(value: Boolean) {
        if (!BuildConfig.DEBUG) return
        preferences.setDebugProEnabled(value)
    }

    private fun BillingResult<CustomerInfo>.toStateResult(): BillingResult<SubscriptionState> = when (this) {
        is BillingResult.Success -> {
            lastRevenueCatState = value.toSubscriptionState()
            publishSubscriptionState()
            BillingResult.Success(_subscriptionState.value)
        }
        is BillingResult.Error -> BillingResult.Error(message)
        BillingResult.Cancelled -> BillingResult.Cancelled
    }

    private fun CustomerInfo.toSubscriptionState(): SubscriptionState {
        val entitlement = entitlements.active[PRO_ENTITLEMENT_ID]
        return if (entitlement?.isActive == true) {
            SubscriptionState(
                isPro = true,
                entitlementId = entitlement.identifier,
                planName = "ServiceSphere Pro",
                expiresAt = entitlement.expirationDate?.time,
                source = "revenuecat"
            )
        } else {
            SubscriptionState(source = "revenuecat")
        }
    }

    private fun publishSubscriptionState() {
        _subscriptionState.value = if (BuildConfig.DEBUG && debugPro && !lastRevenueCatState.isPro) {
            SubscriptionState(
                isPro = true,
                entitlementId = "debug_preview",
                planName = "ServiceSphere Pro",
                source = "debug_preview",
                errorMessage = lastRevenueCatState.errorMessage
            )
        } else {
            lastRevenueCatState.copy(isLoading = false)
        }
    }
}

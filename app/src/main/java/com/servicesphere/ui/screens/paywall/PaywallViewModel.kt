package com.servicesphere.ui.screens.paywall

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.BuildConfig
import com.servicesphere.analytics.AnalyticsTracker
import com.servicesphere.billing.BillingResult
import com.servicesphere.billing.FeatureGateManager
import com.servicesphere.billing.LimitUsageSnapshot
import com.servicesphere.billing.PaywallTrigger
import com.servicesphere.billing.SubscriptionPackageUiModel
import com.servicesphere.billing.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaywallUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val isPro: Boolean = false,
    val currentPlanName: String = "Free",
    val packages: List<SubscriptionPackageUiModel> = emptyList(),
    val selectedPackageId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRevenueCatConfigured: Boolean = false,
    val debugProEnabled: Boolean = false,
    val usage: LimitUsageSnapshot = LimitUsageSnapshot(),
    val trigger: PaywallTrigger = PaywallTrigger.GENERIC,
)

class PaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val featureGateManager: FeatureGateManager,
    private val analyticsTracker: AnalyticsTracker,
    private val trigger: PaywallTrigger
) : ViewModel() {
    private val usage = MutableStateFlow(LimitUsageSnapshot())
    private val selectedPackageId = MutableStateFlow<String?>(null)
    private val isPurchasing = MutableStateFlow(false)
    private val isRestoring = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PaywallUiState> = combine(
        subscriptionRepository.subscriptionState,
        subscriptionRepository.offeringsState,
        subscriptionRepository.debugProEnabled,
        usage,
        selectedPackageId,
        isPurchasing,
        isRestoring,
        errorMessage,
        successMessage
    ) { values ->
        val sub = values[0] as com.servicesphere.billing.SubscriptionState
        val offerings = values[1] as com.servicesphere.billing.OfferingsUiState
        val debugPro = values[2] as Boolean
        val usageSnapshot = values[3] as LimitUsageSnapshot
        val selectedId = values[4] as String?
        val purchasing = values[5] as Boolean
        val restoring = values[6] as Boolean
        val error = values[7] as String?
        val success = values[8] as String?
        val packageId = selectedId ?: offerings.packages.firstOrNull { it.isBestValue }?.id ?: offerings.packages.firstOrNull()?.id
        PaywallUiState(
            isLoading = sub.isLoading || offerings.isLoading,
            isPurchasing = purchasing,
            isRestoring = restoring,
            isPro = sub.isPro,
            currentPlanName = sub.planName,
            packages = offerings.packages,
            selectedPackageId = packageId,
            errorMessage = error ?: offerings.errorMessage ?: sub.errorMessage,
            successMessage = success,
            isRevenueCatConfigured = sub.source != "not_configured",
            debugProEnabled = debugPro,
            usage = usageSnapshot,
            trigger = trigger
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    init {
        log("paywall_viewed(${trigger.routeValue})")
        analyticsTracker.paywallViewed(
            screen = AnalyticsTracker.Screens.PAYWALL,
            source = trigger.routeValue
        )
        loadPaywall()
        refreshUsage()
    }

    fun loadPaywall() {
        subscriptionRepository.refresh()
        viewModelScope.launch { subscriptionRepository.loadOfferings() }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            usage.value = featureGateManager.usageSnapshot()
        }
    }

    fun selectPackage(packageId: String) {
        selectedPackageId.value = packageId
    }

    fun purchaseSelectedPackage(activity: Activity?) {
        val selectedPlanId = uiState.value.selectedPackageId
        analyticsTracker.premiumCtaClicked(
            screen = AnalyticsTracker.Screens.PAYWALL,
            source = trigger.routeValue,
            plan = selectedPlanId
        )
        if (activity == null) {
            analyticsTracker.purchaseFailed(
                screen = AnalyticsTracker.Screens.PAYWALL,
                source = trigger.routeValue,
                plan = selectedPlanId,
                result = "not_started",
                errorType = "missing_activity"
            )
            errorMessage.value = "Purchase requires an active Android screen."
            return
        }
        val selected = uiState.value.packages.firstOrNull { it.id == uiState.value.selectedPackageId }
        if (selected?.revenueCatPackage == null) {
            analyticsTracker.purchaseFailed(
                screen = AnalyticsTracker.Screens.PAYWALL,
                source = trigger.routeValue,
                plan = selectedPlanId,
                result = "not_started",
                errorType = "no_package_selected"
            )
            errorMessage.value = "Select a subscription package first."
            return
        }
        log("purchase_started(${selected.id})")
        analyticsTracker.purchaseStarted(
            screen = AnalyticsTracker.Screens.PAYWALL,
            source = trigger.routeValue,
            plan = selected.id
        )
        viewModelScope.launch {
            isPurchasing.value = true
            when (val result = subscriptionRepository.purchase(activity, selected.revenueCatPackage)) {
                is BillingResult.Success -> {
                    if (result.value.isPro) {
                        log("purchase_success(${selected.id})")
                        analyticsTracker.purchaseSuccess(
                            screen = AnalyticsTracker.Screens.PAYWALL,
                            source = trigger.routeValue,
                            plan = selected.id
                        )
                        successMessage.value = "ServiceSphere Pro is active."
                    } else {
                        analyticsTracker.purchaseFailed(
                            screen = AnalyticsTracker.Screens.PAYWALL,
                            source = trigger.routeValue,
                            plan = selected.id,
                            result = "completed_not_active",
                            errorType = "entitlement_inactive"
                        )
                        errorMessage.value = "Purchase completed, but Pro is not active yet."
                    }
                    refreshUsage()
                }
                is BillingResult.Error -> {
                    analyticsTracker.purchaseFailed(
                        screen = AnalyticsTracker.Screens.PAYWALL,
                        source = trigger.routeValue,
                        plan = selected.id,
                        result = "failed",
                        errorType = "billing_error"
                    )
                    errorMessage.value = result.message
                }
                BillingResult.Cancelled -> {
                    analyticsTracker.purchaseFailed(
                        screen = AnalyticsTracker.Screens.PAYWALL,
                        source = trigger.routeValue,
                        plan = selected.id,
                        result = "cancelled",
                        errorType = "user_cancelled"
                    )
                }
            }
            isPurchasing.value = false
        }
    }

    fun restorePurchases() {
        log("restore_started")
        viewModelScope.launch {
            isRestoring.value = true
            when (val result = subscriptionRepository.restorePurchases()) {
                is BillingResult.Success -> {
                    log(if (result.value.isPro) "restore_success" else "restore_no_purchase")
                    successMessage.value = if (result.value.isPro) {
                        "ServiceSphere Pro restored."
                    } else {
                        "No active Pro subscription was found."
                    }
                    refreshUsage()
                }
                is BillingResult.Error -> errorMessage.value = result.message
                BillingResult.Cancelled -> Unit
            }
            isRestoring.value = false
        }
    }

    fun maybeLater(onMaybeLater: () -> Unit) {
        log("paywall_dismissed(${trigger.routeValue})")
        onMaybeLater()
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    fun setDebugProEnabled(value: Boolean) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { subscriptionRepository.setDebugProEnabled(value) }
    }

    private fun log(event: String) {
        if (BuildConfig.DEBUG) Log.d("Paywall", event)
    }

    class Factory(
        private val subscriptionRepository: SubscriptionRepository,
        private val featureGateManager: FeatureGateManager,
        private val analyticsTracker: AnalyticsTracker,
        private val trigger: PaywallTrigger
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaywallViewModel(subscriptionRepository, featureGateManager, analyticsTracker, trigger) as T
    }
}

package com.servicesphere.billing

import com.revenuecat.purchases.Package as RevenueCatPackage

enum class SubscriptionPlan(val label: String) {
    FREE("Free plan"),
    PRO("ServiceSphere Pro")
}

data class SubscriptionState(
    val isPro: Boolean = false,
    val entitlementId: String? = null,
    val planName: String = "Free",
    val expiresAt: Long? = null,
    val source: String = "not_configured",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class SubscriptionPackageUiModel(
    val id: String,
    val title: String,
    val priceText: String,
    val periodText: String,
    val description: String,
    val isBestValue: Boolean = false,
    val revenueCatPackage: RevenueCatPackage? = null
)

data class OfferingsUiState(
    val isLoading: Boolean = false,
    val packages: List<SubscriptionPackageUiModel> = emptyList(),
    val errorMessage: String? = null
)

interface SubscriptionService {
    fun currentPlanLabel(): String
}

class MockBillingService : SubscriptionService {
    override fun currentPlanLabel(): String = "Free plan"
}

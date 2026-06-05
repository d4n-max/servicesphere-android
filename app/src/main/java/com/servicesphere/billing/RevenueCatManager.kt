package com.servicesphere.billing

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.Period
import com.servicesphere.BuildConfig
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.revenuecat.purchases.Package as RevenueCatPackage

private const val NOT_CONFIGURED_MESSAGE = "Subscriptions are not configured for this build."

class RevenueCatManager(private val apiKey: String) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && Purchases.isConfigured

    fun initialize(context: Context) {
        if (apiKey.isBlank() || Purchases.isConfigured) return
        if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, apiKey).build()
        )
    }

    suspend fun getCustomerInfo(): BillingResult<CustomerInfo> {
        if (!isConfigured) return BillingResult.Error(NOT_CONFIGURED_MESSAGE)
        return suspendCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfo(
                CacheFetchPolicy.default(),
                object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: CustomerInfo) {
                        continuation.resume(BillingResult.Success(customerInfo))
                    }

                    override fun onError(error: PurchasesError) {
                        continuation.resume(BillingResult.Error(error.message))
                    }
                }
            )
        }
    }

    suspend fun getPackages(): BillingResult<List<SubscriptionPackageUiModel>> {
        if (!isConfigured) return BillingResult.Error(NOT_CONFIGURED_MESSAGE)
        return runCatching {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val offering = offerings.current ?: offerings.all.values.firstOrNull()
            offering?.availablePackages.orEmpty().map { it.toUiModel(offering) }
        }.fold(
            onSuccess = { BillingResult.Success(it) },
            onFailure = { BillingResult.Error(it.billingMessage()) }
        )
    }

    suspend fun purchase(activity: Activity, packageToPurchase: RevenueCatPackage): BillingResult<CustomerInfo> {
        if (!isConfigured) return BillingResult.Error(NOT_CONFIGURED_MESSAGE)
        return runCatching {
            Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, packageToPurchase).build()
            ).customerInfo
        }.fold(
            onSuccess = { BillingResult.Success(it) },
            onFailure = { throwable ->
                if (throwable is PurchasesTransactionException && throwable.userCancelled) {
                    BillingResult.Cancelled
                } else {
                    BillingResult.Error(throwable.billingMessage())
                }
            }
        )
    }

    suspend fun restore(): BillingResult<CustomerInfo> {
        if (!isConfigured) return BillingResult.Error(NOT_CONFIGURED_MESSAGE)
        return runCatching { Purchases.sharedInstance.awaitRestore() }.fold(
            onSuccess = { BillingResult.Success(it) },
            onFailure = { BillingResult.Error(it.billingMessage()) }
        )
    }

    private fun RevenueCatPackage.toUiModel(offering: Offering?): SubscriptionPackageUiModel {
        val period = product.period
        val periodText = period?.toDisplayText() ?: packageType.displayName()
        val isAnnual = packageType == PackageType.ANNUAL || period?.unit == Period.Unit.YEAR
        return SubscriptionPackageUiModel(
            id = identifier,
            title = product.name.ifBlank { packageType.displayName() },
            priceText = product.price.formatted,
            periodText = periodText,
            description = product.description.ifBlank {
                offering?.serverDescription?.takeIf { it.isNotBlank() } ?: "ServiceSphere Pro"
            },
            isBestValue = isAnnual,
            revenueCatPackage = this
        )
    }

    private fun Period.toDisplayText(): String {
        val unitText = when (unit) {
            Period.Unit.DAY -> "day"
            Period.Unit.WEEK -> "week"
            Period.Unit.MONTH -> "month"
            Period.Unit.YEAR -> "year"
            Period.Unit.UNKNOWN -> "period"
        }
        return if (value == 1) unitText else "$value ${unitText}s"
    }

    private fun PackageType.displayName(): String = when (this) {
        PackageType.ANNUAL -> "Annual"
        PackageType.MONTHLY -> "Monthly"
        PackageType.WEEKLY -> "Weekly"
        PackageType.SIX_MONTH -> "Six months"
        PackageType.THREE_MONTH -> "Three months"
        PackageType.TWO_MONTH -> "Two months"
        PackageType.LIFETIME -> "Lifetime"
        PackageType.CUSTOM, PackageType.UNKNOWN -> "Subscription"
    }

    private fun Throwable.billingMessage(): String = when (this) {
        is PurchasesException -> message
        is PurchasesTransactionException -> message
        else -> message ?: "Subscription service is unavailable."
    }
}

sealed interface BillingResult<out T> {
    data class Success<T>(val value: T) : BillingResult<T>
    data class Error(val message: String) : BillingResult<Nothing>
    data object Cancelled : BillingResult<Nothing>
}

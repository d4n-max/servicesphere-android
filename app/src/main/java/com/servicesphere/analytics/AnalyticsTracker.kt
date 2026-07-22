package com.servicesphere.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.servicesphere.BuildConfig

class AnalyticsTracker private constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun onboardingStarted(source: String) {
        logEvent(Events.ONBOARDING_STARTED, Params.SOURCE to source)
    }

    fun onboardingCompleted(source: String, result: String) {
        logEvent(Events.ONBOARDING_COMPLETED, Params.SOURCE to source, Params.RESULT to result)
    }

    fun paywallViewed(screen: String, source: String, plan: String? = null) {
        logEvent(Events.PAYWALL_VIEWED, Params.SCREEN to screen, Params.SOURCE to source, Params.PLAN to plan)
    }

    fun premiumCtaClicked(screen: String, source: String, plan: String?) {
        logEvent(Events.PREMIUM_CTA_CLICKED, Params.SCREEN to screen, Params.SOURCE to source, Params.PLAN to plan)
    }

    fun purchaseStarted(screen: String, source: String, plan: String) {
        logEvent(Events.PURCHASE_STARTED, Params.SCREEN to screen, Params.SOURCE to source, Params.PLAN to plan)
    }

    fun purchaseSuccess(screen: String, source: String, plan: String, result: String = "pro_active") {
        logEvent(
            Events.PURCHASE_SUCCESS,
            Params.SCREEN to screen,
            Params.SOURCE to source,
            Params.PLAN to plan,
            Params.RESULT to result
        )
    }

    fun purchaseFailed(screen: String, source: String, plan: String?, result: String, errorType: String) {
        logEvent(
            Events.PURCHASE_FAILED,
            Params.SCREEN to screen,
            Params.SOURCE to source,
            Params.PLAN to plan,
            Params.RESULT to result,
            Params.ERROR_TYPE to errorType
        )
    }

    fun settingsOpened(source: String) {
        logEvent(Events.SETTINGS_OPENED, Params.SCREEN to Screens.SETTINGS, Params.SOURCE to source)
    }

    fun workflowConversion(event: String, sourceScreen: String, result: String? = null) {
        logEvent(event, Params.SOURCE to sourceScreen, Params.RESULT to result)
    }

    fun screenView(screen: String, source: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen)
            putString(Params.SCREEN, screen)
            source?.let { putString(Params.SOURCE, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        debugLog(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    private fun logEvent(eventName: String, vararg params: Pair<String, String?>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                value?.takeIf { it.isNotBlank() }?.let { putString(key, it.safeAnalyticsValue()) }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
        debugLog(eventName, bundle)
    }

    private fun debugLog(eventName: String, bundle: Bundle) {
        if (BuildConfig.DEBUG) Log.d(TAG, "$eventName ${bundle.toDebugMap()}")
    }

    private fun String.safeAnalyticsValue(): String =
        trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .take(MAX_PARAM_LENGTH)

    private fun Bundle.toDebugMap(): Map<String, String?> =
        keySet().associateWith { key -> getString(key) }

    object Events {
        const val ONBOARDING_STARTED = "onboarding_started"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val PAYWALL_VIEWED = "paywall_viewed"
        const val PREMIUM_CTA_CLICKED = "premium_cta_clicked"
        const val PURCHASE_STARTED = "purchase_started"
        const val PURCHASE_SUCCESS = "purchase_success"
        const val PURCHASE_FAILED = "purchase_failed"
        const val SETTINGS_OPENED = "settings_opened"
        const val QUOTE_TO_JOB_STARTED = "quote_to_job_started"
        const val QUOTE_TO_JOB_COMPLETED = "quote_to_job_completed"
        const val QUOTE_TO_JOB_FAILED = "quote_to_job_failed"
        const val JOB_TO_INVOICE_STARTED = "job_to_invoice_started"
        const val JOB_TO_INVOICE_COMPLETED = "job_to_invoice_completed"
        const val JOB_TO_INVOICE_FAILED = "job_to_invoice_failed"
        const val TODAY_VIEWED = "today_viewed"
        const val TODAY_NEXT_JOB_OPENED = "today_next_job_opened"
        const val TODAY_DIRECTIONS_OPENED = "today_directions_opened"
        const val TODAY_INVOICE_FOLLOWUP_STARTED = "today_invoice_followup_started"
        const val TODAY_QUOTE_FOLLOWUP_STARTED = "today_quote_followup_started"
    }

    object Params {
        const val SCREEN = "screen"
        const val SOURCE = "source"
        const val PLAN = "plan"
        const val RESULT = "result"
        const val ERROR_TYPE = "error_type"
    }

    object Screens {
        const val PAYWALL = "paywall"
        const val SETTINGS = "settings"
    }

    companion object {
        private const val TAG = "AnalyticsTracker"
        private const val MAX_PARAM_LENGTH = 40

        fun create(context: Context): AnalyticsTracker =
            AnalyticsTracker(FirebaseAnalytics.getInstance(context.applicationContext))
    }
}

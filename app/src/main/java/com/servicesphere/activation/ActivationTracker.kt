package com.servicesphere.activation

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.servicesphere.BuildConfig
import com.servicesphere.data.preferences.AnalyticsOnceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface ActivationTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
    fun trackFirst(event: String, params: Map<String, String> = emptyMap())
}

class DebugActivationTracker : ActivationTracker {
    override fun track(event: String, params: Map<String, String>) {
        if (BuildConfig.DEBUG) Log.d(TAG, "$event $params")
    }

    override fun trackFirst(event: String, params: Map<String, String>) {
        track(event, params)
    }
}

class FirebaseActivationTracker(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val onceStore: AnalyticsOnceStore,
    private val debugTracker: ActivationTracker = DebugActivationTracker(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ActivationTracker {
    private val onceGuard = AnalyticsEventOnceGuard(onceStore)

    override fun track(event: String, params: Map<String, String>) {
        scope.launch {
            logEvent(event, params)
        }
    }

    override fun trackFirst(event: String, params: Map<String, String>) {
        scope.launch {
            onceGuard.runIfFirst(event) {
                logEvent(event, params)
            }
        }
    }

    private fun logEvent(event: String, params: Map<String, String>) {
        val eventName = event.safeAnalyticsKey()
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                value.takeIf { it.isNotBlank() }?.let { putString(key.safeAnalyticsKey(), it.safeAnalyticsValue()) }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
        debugTracker.track(eventName, bundle.toDebugMap())
    }

    private fun String.safeAnalyticsKey(): String =
        trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .take(MAX_NAME_LENGTH)

    private fun String.safeAnalyticsValue(): String =
        trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .take(MAX_VALUE_LENGTH)

    private fun Bundle.toDebugMap(): Map<String, String> =
        keySet().associateWith { key -> getString(key).orEmpty() }

    companion object {
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_VALUE_LENGTH = 40

        fun create(context: Context, onceStore: AnalyticsOnceStore): FirebaseActivationTracker =
            FirebaseActivationTracker(FirebaseAnalytics.getInstance(context.applicationContext), onceStore)
    }
}

class AnalyticsEventOnceGuard(private val onceStore: AnalyticsOnceStore) {
    suspend fun runIfFirst(event: String, action: () -> Unit): Boolean {
        if (!onceStore.markAnalyticsEventTrackedOnce(event)) return false
        action()
        return true
    }
}

object ActivationEvents {
    const val ONBOARDING_STARTED = "onboarding_started"
    const val FIRST_JOB_STARTED = "first_job_started"
    const val FIRST_CLIENT_CREATED = "first_client_created"
    const val FIRST_JOB_CREATED = "first_job_created"
    const val FIRST_QUOTE_CREATED = "first_quote_created"
    const val FIRST_JOB_NOTE_ADDED = "first_job_note_added"
    const val FIRST_PHOTO_PROOF_ADDED = "first_photo_proof_added"
    const val FIRST_INVOICE_CREATED = "first_invoice_created"
    const val FIRST_SIGNATURE_CAPTURED = "first_signature_captured"
    const val FIRST_PDF_GENERATED = "first_pdf_generated"
    const val DATA_EXPORT_CREATED = "data_export_created"
    const val ACTIVATION_FIRST_JOB_ORGANIZED = "activation_first_job_organized"
    const val ONBOARDING_DEMO_STARTED = "onboarding_demo_started"
    const val ONBOARDING_DEMO_TO_REAL_JOB_STARTED = "onboarding_demo_to_real_job_started"
    const val BUSINESS_SETUP_PROMPT_SEEN = "business_setup_prompt_seen"
    const val BUSINESS_SETUP_STARTED = "business_setup_started"
    const val BUSINESS_SETUP_SKIPPED = "business_setup_skipped"
}

object ActivationParams {
    const val SOURCE_SCREEN = "source_screen"
    const val HAS_CLIENT = "has_client"
    const val HAS_SCHEDULE = "has_schedule"
    const val HAS_DETAILS = "has_details"
    const val JOB_STATUS = "job_status"
    const val CURRENCY = "currency"
    const val ITEM_COUNT = "item_count"
    const val VALUE_BUCKET = "value_bucket"
    const val DOCUMENT_TYPE = "document_type"
    const val EXPORT_TYPE = "export_type"
    const val SIGNATURE_TARGET = "signature_target"
    const val PHOTO_SOURCE = "photo_source"
}

object AnalyticsValueBuckets {
    fun fromAmount(amount: Double): String = when {
        amount <= 0.0 -> "zero"
        amount < 100.0 -> "under_100"
        amount < 500.0 -> "100_499"
        amount < 1_000.0 -> "500_999"
        amount < 5_000.0 -> "1000_4999"
        else -> "5000_plus"
    }
}

private const val TAG = "ActivationTracker"

package com.servicesphere.activation

import android.util.Log
import com.servicesphere.BuildConfig

interface ActivationTracker {
    fun track(event: String)
}

class DebugActivationTracker : ActivationTracker {
    override fun track(event: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, event)
    }
}

object ActivationEvents {
    const val ONBOARDING_STARTED = "onboarding_started"
    const val FIRST_JOB_STARTED = "first_job_started"
    const val FIRST_CLIENT_CREATED = "first_client_created"
    const val FIRST_JOB_CREATED = "first_job_created"
    const val FIRST_QUOTE_CREATED = "first_quote_created"
    const val FIRST_JOB_NOTE_ADDED = "first_job_note_added"
    const val FIRST_PHOTO_ADDED = "first_photo_added"
    const val FIRST_INVOICE_CREATED = "first_invoice_created"
    const val ACTIVATION_FIRST_JOB_ORGANIZED = "activation_first_job_organized"
    const val ONBOARDING_DEMO_STARTED = "onboarding_demo_started"
    const val ONBOARDING_DEMO_TO_REAL_JOB_STARTED = "onboarding_demo_to_real_job_started"
    const val BUSINESS_SETUP_PROMPT_SEEN = "business_setup_prompt_seen"
    const val BUSINESS_SETUP_STARTED = "business_setup_started"
    const val BUSINESS_SETUP_SKIPPED = "business_setup_skipped"
}

private const val TAG = "ActivationTracker"

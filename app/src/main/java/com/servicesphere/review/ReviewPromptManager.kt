package com.servicesphere.review

import com.servicesphere.data.preferences.ReviewPromptPreferenceSnapshot
import com.servicesphere.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

enum class ReviewSuccessMoment {
    INVOICE_CREATED,
    JOB_COMPLETED,
    SIGNATURE_COLLECTED,
    QUOTE_SHARED
}

data class ReviewPromptRules(
    val minimumSuccessMoments: Int = 2,
    val cooldownMillis: Long = 30L * 24L * 60L * 60L * 1000L,
    val maxDismissals: Int = 3,
    val minimumAppSessions: Int = 2
)

object ReviewPromptEligibility {
    fun isEligible(
        snapshot: ReviewPromptPreferenceSnapshot,
        nowMillis: Long,
        rules: ReviewPromptRules = ReviewPromptRules()
    ): Boolean {
        if (snapshot.appSessionCount < rules.minimumAppSessions) return false
        if (snapshot.completedSuccessMomentsCount < rules.minimumSuccessMoments) return false
        if (snapshot.reviewPromptDismissCount >= rules.maxDismissals) return false
        val lastPromptAt = snapshot.lastReviewPromptAt ?: return true
        return nowMillis - lastPromptAt >= rules.cooldownMillis
    }
}

class ReviewPromptManager(
    private val preferences: UserPreferences,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val rules: ReviewPromptRules = ReviewPromptRules()
) {
    private var sessionRecorded = false

    suspend fun markAppSessionStarted() {
        if (sessionRecorded) return
        sessionRecorded = true
        preferences.incrementAppSessionCount()
    }

    suspend fun recordSuccessMoment(moment: ReviewSuccessMoment): Boolean {
        preferences.incrementReviewSuccessMoments()
        val snapshot = preferences.reviewPromptSnapshot.first()
        return ReviewPromptEligibility.isEligible(snapshot, clockMillis(), rules)
    }

    suspend fun markPositiveAttempt(appVersion: String) {
        preferences.markReviewPromptAttempted(clockMillis(), appVersion)
    }

    suspend fun markPrivateFeedback(appVersion: String) {
        preferences.markReviewPromptAttempted(clockMillis(), appVersion)
    }

    suspend fun dismissPrompt() {
        preferences.dismissReviewPrompt(clockMillis())
    }
}

package com.servicesphere.review

import com.servicesphere.data.preferences.ReviewPromptPreferenceSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptEligibilityTest {
    private val now = 100L * 24L * 60L * 60L * 1000L

    @Test
    fun firstLaunchIsNeverEligible() {
        val snapshot = snapshot(appSessionCount = 1, completedSuccessMomentsCount = 4)

        assertFalse(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun oneSuccessMomentIsNotEligible() {
        val snapshot = snapshot(appSessionCount = 2, completedSuccessMomentsCount = 1)

        assertFalse(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun twoSuccessMomentsCanBecomeEligible() {
        val snapshot = snapshot(appSessionCount = 2, completedSuccessMomentsCount = 2)

        assertTrue(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun thirtyDayCooldownBlocksPrompt() {
        val snapshot = snapshot(
            appSessionCount = 2,
            completedSuccessMomentsCount = 4,
            lastReviewPromptAt = now - 10L * 24L * 60L * 60L * 1000L
        )

        assertFalse(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun promptIsEligibleAfterCooldown() {
        val snapshot = snapshot(
            appSessionCount = 2,
            completedSuccessMomentsCount = 4,
            lastReviewPromptAt = now - 31L * 24L * 60L * 60L * 1000L
        )

        assertTrue(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun threeDismissalsBlockPrompt() {
        val snapshot = snapshot(
            appSessionCount = 2,
            completedSuccessMomentsCount = 4,
            reviewPromptDismissCount = 3
        )

        assertFalse(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    @Test
    fun positiveAttemptCooldownBlocksPrompt() {
        val snapshot = snapshot(
            appSessionCount = 2,
            completedSuccessMomentsCount = 4,
            hasSeenReviewPrompt = true,
            lastReviewPromptAt = now
        )

        assertFalse(ReviewPromptEligibility.isEligible(snapshot, now))
    }

    private fun snapshot(
        hasSeenReviewPrompt: Boolean = false,
        lastReviewPromptAt: Long? = null,
        reviewPromptDismissCount: Int = 0,
        completedSuccessMomentsCount: Int = 0,
        lastPromptedAppVersion: String? = null,
        appSessionCount: Int = 0
    ) = ReviewPromptPreferenceSnapshot(
        hasSeenReviewPrompt = hasSeenReviewPrompt,
        lastReviewPromptAt = lastReviewPromptAt,
        reviewPromptDismissCount = reviewPromptDismissCount,
        completedSuccessMomentsCount = completedSuccessMomentsCount,
        lastPromptedAppVersion = lastPromptedAppVersion,
        appSessionCount = appSessionCount
    )
}

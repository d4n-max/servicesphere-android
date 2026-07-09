package com.servicesphere.activation

import com.servicesphere.data.preferences.AnalyticsOnceStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventOnceGuardTest {
    @Test
    fun `runs action only for first event occurrence`() = runBlocking {
        val guard = AnalyticsEventOnceGuard(InMemoryOnceStore())
        var calls = 0

        val first = guard.runIfFirst(ActivationEvents.FIRST_JOB_CREATED) { calls++ }
        val second = guard.runIfFirst(ActivationEvents.FIRST_JOB_CREATED) { calls++ }

        assertTrue(first)
        assertFalse(second)
        assertEquals(1, calls)
    }

    @Test
    fun `keeps different first events independent`() = runBlocking {
        val guard = AnalyticsEventOnceGuard(InMemoryOnceStore())
        var calls = 0

        assertTrue(guard.runIfFirst(ActivationEvents.FIRST_CLIENT_CREATED) { calls++ })
        assertTrue(guard.runIfFirst(ActivationEvents.FIRST_JOB_CREATED) { calls++ })

        assertEquals(2, calls)
    }
}

private class InMemoryOnceStore : AnalyticsOnceStore {
    private val tracked = mutableSetOf<String>()

    override suspend fun markAnalyticsEventTrackedOnce(eventName: String): Boolean =
        tracked.add(eventName)
}

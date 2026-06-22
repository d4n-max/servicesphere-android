package com.servicesphere.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreePlanPolicyTest {
    @Test
    fun `free users can create records below limits`() {
        assertTrue(FreePlanPolicy.canCreateClient(FreePlanLimits.maxClients - 1))
        assertTrue(FreePlanPolicy.canCreateJob(FreePlanLimits.maxJobs - 1))
        assertTrue(FreePlanPolicy.canCreateQuote(FreePlanLimits.maxQuotes - 1))
        assertTrue(FreePlanPolicy.canCreateInvoice(FreePlanLimits.maxInvoices - 1))
        assertTrue(FreePlanPolicy.canAddPhoto(FreePlanLimits.maxPhotoProofPerJob - 1))
        assertTrue(FreePlanPolicy.canCaptureSignature(FreePlanLimits.maxSignatures - 1))
    }

    @Test
    fun `free users are blocked at limits`() {
        assertFalse(FreePlanPolicy.canCreateClient(FreePlanLimits.maxClients))
        assertFalse(FreePlanPolicy.canCreateJob(FreePlanLimits.maxJobs))
        assertFalse(FreePlanPolicy.canCreateQuote(FreePlanLimits.maxQuotes))
        assertFalse(FreePlanPolicy.canCreateInvoice(FreePlanLimits.maxInvoices))
        assertFalse(FreePlanPolicy.canAddPhoto(FreePlanLimits.maxPhotoProofPerJob))
        assertFalse(FreePlanPolicy.canCaptureSignature(FreePlanLimits.maxSignatures))
    }
}

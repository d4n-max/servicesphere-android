package com.servicesphere.billing

object FreePlanLimits {
    const val maxClients = 5
    const val maxJobs = 10
    const val maxQuotes = 3
    const val maxInvoices = 3
    const val maxPhotoProofPerJob = 3
    const val maxSignatures = 1
    const val freePdfExports = 1
    const val pdfWatermarkEnabled = true
    const val businessLogoOnPdfEnabled = false
    const val premiumPdfTemplatesEnabled = false
}

object FreePlanPolicy {
    fun canCreateClient(currentClients: Int): Boolean = currentClients < FreePlanLimits.maxClients
    fun canCreateJob(currentJobs: Int): Boolean = currentJobs < FreePlanLimits.maxJobs
    fun canCreateQuote(currentQuotes: Int): Boolean = currentQuotes < FreePlanLimits.maxQuotes
    fun canCreateInvoice(currentInvoices: Int): Boolean = currentInvoices < FreePlanLimits.maxInvoices
    fun canAddPhoto(currentPhotosForJob: Int): Boolean = currentPhotosForJob < FreePlanLimits.maxPhotoProofPerJob
    fun canCaptureSignature(currentSignatures: Int): Boolean = currentSignatures < FreePlanLimits.maxSignatures
}

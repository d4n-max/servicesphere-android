package com.servicesphere.billing

import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobPhotoRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.SignatureRepository
import com.servicesphere.domain.model.JobStatus
import kotlinx.coroutines.flow.first

class FeatureGateManager(
    private val subscriptionRepository: SubscriptionRepository,
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val jobPhotoRepository: JobPhotoRepository,
    private val signatureRepository: SignatureRepository
) {
    val subscriptionState = subscriptionRepository.subscriptionState

    suspend fun canCreateClient(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = clientRepository.getClientCountOnce()
        return if (FreePlanPolicy.canCreateClient(count)) allowedGate()
        else blocked(ProFeature.CREATE_CLIENT, PaywallTrigger.CLIENT_LIMIT)
    }

    suspend fun canCreateJob(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val active = jobRepository.observeJobs().first().count { it.status != JobStatus.CANCELLED }
        return if (FreePlanPolicy.canCreateJob(active)) allowedGate()
        else blocked(ProFeature.CREATE_JOB, PaywallTrigger.JOB_LIMIT)
    }

    suspend fun canCreateQuote(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = quoteRepository.observeQuotes().first().size
        return if (FreePlanPolicy.canCreateQuote(count)) allowedGate()
        else blocked(ProFeature.CREATE_QUOTE, PaywallTrigger.QUOTE_LIMIT)
    }

    suspend fun canCreateInvoice(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = invoiceRepository.observeInvoices().first().size
        return if (FreePlanPolicy.canCreateInvoice(count)) allowedGate()
        else blocked(ProFeature.CREATE_INVOICE, PaywallTrigger.INVOICE_LIMIT)
    }

    suspend fun canAddPhotoProof(jobId: String): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = jobPhotoRepository.observePhotosForJob(jobId).first().size
        return if (FreePlanPolicy.canAddPhoto(count)) allowedGate()
        else blocked(ProFeature.ADD_PHOTO_PROOF, PaywallTrigger.PHOTO_LIMIT)
    }

    suspend fun canCaptureSignature(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = signatureRepository.getAllSignaturesOnce().size
        return if (FreePlanPolicy.canCaptureSignature(count)) allowedGate()
        else blocked(ProFeature.CAPTURE_SIGNATURE, PaywallTrigger.SIGNATURE)
    }

    suspend fun canUseBusinessLogoOnPdf(): FeatureGateResult =
        if (isPro()) allowedGate()
        else blocked(ProFeature.USE_BUSINESS_LOGO_ON_PDF, PaywallTrigger.BRANDING)

    suspend fun canExportPdf(): FeatureGateResult =
        if (isPro()) allowedGate()
        else blocked(ProFeature.EXPORT_UNLIMITED_DOCUMENTS, PaywallTrigger.PDF_EXPORT)

    suspend fun shouldShowPdfWatermark(): Boolean = !isPro()
    suspend fun isPremiumPdfTemplateAllowed(): Boolean = isPro()

    suspend fun usageSnapshot(): LimitUsageSnapshot {
        return LimitUsageSnapshot(
            clients = clientRepository.getClientCountOnce(),
            activeJobs = jobRepository.observeJobs().first().count { it.status != JobStatus.CANCELLED },
            quotes = quoteRepository.observeQuotes().first().size,
            invoices = invoiceRepository.observeInvoices().first().size,
            signatures = signatureRepository.getAllSignaturesOnce().size
        )
    }

    private suspend fun isPro(): Boolean = subscriptionRepository.subscriptionState.first().isPro

    private fun blocked(feature: ProFeature, trigger: PaywallTrigger): FeatureGateResult =
        FeatureGateResult(
            allowed = false,
            title = trigger.title,
            message = trigger.body,
            featureName = feature.label,
            trigger = trigger
        )
}

data class LimitUsageSnapshot(
    val clients: Int = 0,
    val activeJobs: Int = 0,
    val quotes: Int = 0,
    val invoices: Int = 0,
    val signatures: Int = 0
)

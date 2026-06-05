package com.servicesphere.billing

import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobPhotoRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.SignatureRepository
import com.servicesphere.domain.model.JobStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

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
        return if (count < FreePlanLimits.maxClients) allowedGate()
        else blocked(ProFeature.CREATE_CLIENT, "Client limit reached", "Free plan includes up to 5 clients. Upgrade to Pro to manage unlimited clients.")
    }

    suspend fun canCreateJob(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val active = jobRepository.observeJobs().first().count { it.status != JobStatus.CANCELLED }
        return if (active < FreePlanLimits.maxJobs) allowedGate()
        else blocked(ProFeature.CREATE_JOB, "Job limit reached", "Free plan includes up to 5 active jobs. Upgrade to Pro for unlimited jobs.")
    }

    suspend fun canCreateQuote(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = quoteRepository.observeQuotes().first().count { it.issueDate in currentMonthRange() }
        return if (count < FreePlanLimits.maxQuotesPerMonth) allowedGate()
        else blocked(ProFeature.CREATE_QUOTE, "Quote limit reached", "Free plan includes 3 quotes per month. Upgrade to Pro for unlimited quotes.")
    }

    suspend fun canCreateInvoice(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = invoiceRepository.observeInvoices().first().count { it.issueDate in currentMonthRange() }
        return if (count < FreePlanLimits.maxInvoicesPerMonth) allowedGate()
        else blocked(ProFeature.CREATE_INVOICE, "Invoice limit reached", "Free plan includes 3 invoices per month. Upgrade to Pro for unlimited invoices.")
    }

    suspend fun canAddPhotoProof(jobId: String): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = jobPhotoRepository.observePhotosForJob(jobId).first().size
        return if (count < FreePlanLimits.maxPhotoProofPerJob) allowedGate()
        else blocked(ProFeature.ADD_PHOTO_PROOF, "Photo proof limit reached", "Free plan includes 3 photos per job. Upgrade to Pro for unlimited photo proof.")
    }

    suspend fun canCaptureSignature(): FeatureGateResult {
        if (isPro()) return allowedGate()
        val count = signatureRepository.observeSignaturesThisMonth(monthStart(), nextMonthStart()).first()
        return if (count < FreePlanLimits.maxSignaturesPerMonth) allowedGate()
        else blocked(ProFeature.CAPTURE_SIGNATURE, "Signature limit reached", "Free plan includes 3 signatures per month. Upgrade to Pro for unlimited signatures.")
    }

    suspend fun canUseBusinessLogoOnPdf(): FeatureGateResult =
        if (isPro()) allowedGate()
        else blocked(ProFeature.USE_BUSINESS_LOGO_ON_PDF, "Business logo is a Pro feature", "Upgrade to Pro to add your logo to quotes and invoices.")

    suspend fun shouldShowPdfWatermark(): Boolean = !isPro()
    suspend fun isPremiumPdfTemplateAllowed(): Boolean = isPro()

    suspend fun usageSnapshot(): LimitUsageSnapshot {
        val range = currentMonthRange()
        return LimitUsageSnapshot(
            clients = clientRepository.getClientCountOnce(),
            activeJobs = jobRepository.observeJobs().first().count { it.status != JobStatus.CANCELLED },
            quotesThisMonth = quoteRepository.observeQuotes().first().count { it.issueDate in range },
            invoicesThisMonth = invoiceRepository.observeInvoices().first().count { it.issueDate in range },
            signaturesThisMonth = signatureRepository.observeSignaturesThisMonth(monthStart(), nextMonthStart()).first()
        )
    }

    private suspend fun isPro(): Boolean = subscriptionRepository.subscriptionState.first().isPro

    private fun blocked(feature: ProFeature, title: String, message: String): FeatureGateResult =
        FeatureGateResult(false, title, message, feature.label)

    private fun currentMonthRange(): LongRange = monthStart() until nextMonthStart()
    private fun monthStart(): Long = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun nextMonthStart(): Long = Instant.ofEpochMilli(monthStart()).atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

data class LimitUsageSnapshot(
    val clients: Int = 0,
    val activeJobs: Int = 0,
    val quotesThisMonth: Int = 0,
    val invoicesThisMonth: Int = 0,
    val signaturesThisMonth: Int = 0
)

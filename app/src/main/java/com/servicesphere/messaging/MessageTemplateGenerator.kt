package com.servicesphere.messaging

object MessageTemplateGenerator {
    fun generate(type: MessageTemplateType, data: ClientMessageData): String {
        val clientName = safeClientName(data.clientName)
        val businessName = safeBusinessName(data.businessName)
        return when (type) {
            MessageTemplateType.QUOTE_FOLLOW_UP -> buildQuoteFollowUp(clientName, businessName, data)
            MessageTemplateType.QUOTE_ACCEPTED_NEXT_STEPS -> buildQuoteAccepted(clientName, data)
            MessageTemplateType.INVOICE_PAYMENT_REMINDER -> buildPaymentReminder(clientName, data)
            MessageTemplateType.INVOICE_OVERDUE_REMINDER -> buildOverdueReminder(clientName, data)
            MessageTemplateType.JOB_COMPLETED -> buildJobCompleted(clientName, businessName, data)
            MessageTemplateType.THANK_YOU_REVIEW_REQUEST -> "Hi $clientName, thank you for working with $businessName. If you're happy with the service, a quick review would really help."
            MessageTemplateType.CUSTOM -> ""
        }
    }

    private fun buildQuoteFollowUp(clientName: String, businessName: String, data: ClientMessageData): String {
        val quote = data.quoteNumber?.takeIf { it.isNotBlank() }?.let { "quote $it" } ?: "the quote"
        return "Hi $clientName, just following up on $quote from $businessName. Let me know if you have any questions or if you'd like to go ahead."
    }

    private fun buildQuoteAccepted(clientName: String, data: ClientMessageData): String {
        val quote = data.quoteNumber?.takeIf { it.isNotBlank() }?.let { "quote $it" } ?: "the quote"
        val job = data.jobTitle?.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()
        return "Hi $clientName, thanks for approving $quote. I'll confirm the next steps and schedule$job."
    }

    private fun buildPaymentReminder(clientName: String, data: ClientMessageData): String {
        val invoice = data.invoiceNumber?.takeIf { it.isNotBlank() }?.let { "invoice $it" } ?: "your invoice"
        val amount = data.total?.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()
        val due = data.dueDate?.takeIf { it.isNotBlank() } ?: "soon"
        return appendPaymentInstructions(
            "Hi $clientName, this is a friendly reminder that $invoice$amount is due on $due.",
            data.paymentInstructions
        )
    }

    private fun buildOverdueReminder(clientName: String, data: ClientMessageData): String {
        val invoice = data.invoiceNumber?.takeIf { it.isNotBlank() }?.let { "invoice $it" } ?: "your invoice"
        val amount = data.total?.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()
        return appendPaymentInstructions(
            "Hi $clientName, $invoice$amount is now overdue. Please let me know when payment has been sent.",
            data.paymentInstructions
        )
    }

    private fun buildJobCompleted(clientName: String, businessName: String, data: ClientMessageData): String {
        val job = data.jobTitle?.takeIf { it.isNotBlank() }?.let { " '$it'" }.orEmpty()
        return "Hi $clientName, the job$job has been completed. Thank you for choosing $businessName."
    }

    private fun appendPaymentInstructions(base: String, instructions: String?): String =
        if (instructions.isNullOrBlank()) base else "$base Payment details: $instructions"

    fun safeClientName(name: String?): String = name?.takeIf { it.isNotBlank() } ?: "there"

    fun safeBusinessName(name: String?): String = name?.takeIf { it.isNotBlank() } ?: "us"
}

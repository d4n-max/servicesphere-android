package com.servicesphere.messaging

enum class MessageTemplateType(val routeValue: String, val title: String) {
    QUOTE_FOLLOW_UP("QUOTE_FOLLOW_UP", "Quote Follow-up"),
    QUOTE_ACCEPTED_NEXT_STEPS("QUOTE_ACCEPTED_NEXT_STEPS", "Next Steps"),
    INVOICE_PAYMENT_REMINDER("INVOICE_PAYMENT_REMINDER", "Payment Reminder"),
    INVOICE_OVERDUE_REMINDER("INVOICE_OVERDUE_REMINDER", "Overdue Reminder"),
    JOB_COMPLETED("JOB_COMPLETED", "Job Completed Message"),
    THANK_YOU_REVIEW_REQUEST("THANK_YOU_REVIEW_REQUEST", "Thank You Message"),
    CUSTOM("CUSTOM", "Message Client");

    companion object {
        fun fromRoute(value: String?): MessageTemplateType =
            entries.firstOrNull { it.routeValue.equals(value, ignoreCase = true) } ?: CUSTOM
    }
}

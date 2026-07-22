package com.servicesphere.domain.model

object JobStatus {
    const val NEW = "NEW"
    const val SCHEDULED = "SCHEDULED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val INVOICED = "INVOICED"
    const val PAID = "PAID"
    const val CANCELLED = "CANCELLED"
}

object QuoteStatus {
    const val DRAFT = "DRAFT"
    const val SENT = "SENT"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
    const val DECLINED = REJECTED
    const val EXPIRED = "EXPIRED"
    const val CONVERTED_TO_INVOICE = "CONVERTED_TO_INVOICE"
}

object InvoiceStatus {
    const val DRAFT = "DRAFT"
    const val SENT = "SENT"
    const val UNPAID = "UNPAID"
    const val PAID = "PAID"
    const val OVERDUE = "OVERDUE"
    const val CANCELLED = "CANCELLED"
    const val VOID = CANCELLED
}

object PaymentMethod {
    const val CASH = "CASH"
    const val BANK_TRANSFER = "BANK_TRANSFER"
    const val CARD = "CARD"
    const val OTHER = "OTHER"
}

object LineItemParentType {
    const val QUOTE = "QUOTE"
    const val INVOICE = "INVOICE"
}

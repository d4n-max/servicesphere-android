package com.servicesphere.documents

import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus
import java.time.LocalDate

fun documentFileName(businessName: String, documentType: String, number: String): String =
    "${safeFilePart(businessName)}-${safeFilePart(documentType)}-${safeFilePart(number)}.pdf"

fun canTransitionQuote(from: String, to: String): Boolean = when (from) {
    QuoteStatus.DRAFT -> to in setOf(QuoteStatus.SENT, QuoteStatus.ACCEPTED, QuoteStatus.REJECTED)
    QuoteStatus.SENT -> to in setOf(QuoteStatus.DRAFT, QuoteStatus.ACCEPTED, QuoteStatus.REJECTED)
    QuoteStatus.ACCEPTED -> to == QuoteStatus.CONVERTED_TO_INVOICE
    QuoteStatus.REJECTED -> to == QuoteStatus.DRAFT
    else -> false
}

fun displayedInvoiceStatus(status: String, dueDate: LocalDate?, today: LocalDate = LocalDate.now()): String =
    if (status !in setOf(InvoiceStatus.PAID, InvoiceStatus.CANCELLED) && dueDate != null && dueDate.isBefore(today)) InvoiceStatus.OVERDUE else status

private fun safeFilePart(value: String): String = value.trim()
    .replace(Regex("[^A-Za-z0-9]+"), "-")
    .trim('-')
    .ifBlank { "Document" }

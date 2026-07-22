package com.servicesphere.documents

import androidx.room.withTransaction
import com.servicesphere.data.local.AppDatabase
import com.servicesphere.data.local.DocumentActivityEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus

object DocumentType { const val QUOTE = "QUOTE"; const val INVOICE = "INVOICE" }
object DocumentEvent {
    const val CREATED = "CREATED"; const val PDF_GENERATED = "PDF_GENERATED"; const val PDF_REGENERATED = "PDF_REGENERATED"
    const val SHARE_STARTED = "SHARE_STARTED"; const val MARKED_SENT = "MARKED_SENT"; const val ACCEPTED = "ACCEPTED"
    const val DECLINED = "DECLINED"; const val CONVERTED = "CONVERTED"; const val PAID = "PAID"; const val VOIDED = "VOIDED"
}

/** Centralizes persisted document mutations so status and timeline cannot drift apart. */
class DocumentLifecycleRepository(private val database: AppDatabase) {
    suspend fun markQuoteSent(id: String): Result<Unit> = changeQuote(id, QuoteStatus.SENT, DocumentEvent.MARKED_SENT)
    suspend fun acceptQuote(id: String): Result<Unit> = changeQuote(id, QuoteStatus.ACCEPTED, DocumentEvent.ACCEPTED)
    suspend fun declineQuote(id: String): Result<Unit> = changeQuote(id, QuoteStatus.REJECTED, DocumentEvent.DECLINED)

    suspend fun markInvoiceSent(id: String): Result<Unit> = changeInvoice(id, InvoiceStatus.SENT, DocumentEvent.MARKED_SENT)
    suspend fun markInvoicePaid(id: String): Result<Unit> = changeInvoice(id, InvoiceStatus.PAID, DocumentEvent.PAID)
    suspend fun voidInvoice(id: String): Result<Unit> = changeInvoice(id, InvoiceStatus.CANCELLED, DocumentEvent.VOIDED)

    suspend fun recordPdf(id: String, type: String, path: String, sourceUpdatedAt: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val now = System.currentTimeMillis()
            when (type) {
                DocumentType.QUOTE -> database.quoteDao().getQuoteByIdOnce(id)?.let { quote ->
                    database.quoteDao().updateQuote(quote.copy(pdfPath = path, pdfGeneratedAt = now, pdfSourceUpdatedAt = sourceUpdatedAt))
                } ?: error("Quote no longer exists")
                DocumentType.INVOICE -> database.invoiceDao().getInvoiceByIdOnce(id)?.let { invoice ->
                    database.invoiceDao().updateInvoice(invoice.copy(pdfPath = path, pdfGeneratedAt = now, pdfSourceUpdatedAt = sourceUpdatedAt))
                } ?: error("Invoice no longer exists")
                else -> error("Unsupported document type")
            }
            addEventOnce(id, type, if (type == DocumentType.QUOTE || type == DocumentType.INVOICE) DocumentEvent.PDF_GENERATED else DocumentEvent.PDF_REGENERATED)
        }
    }

    suspend fun recordShareStarted(id: String, type: String): Result<Unit> = runCatching {
        database.withTransaction { addEventOnce(id, type, DocumentEvent.SHARE_STARTED) }
    }

    suspend fun recordConversion(quoteId: String, invoiceId: String): Result<Unit> = runCatching {
        database.withTransaction {
            val quote = database.quoteDao().getQuoteByIdOnce(quoteId) ?: error("Quote not found")
            database.quoteDao().updateQuote(quote.copy(status = QuoteStatus.CONVERTED_TO_INVOICE, convertedInvoiceId = invoiceId, updatedAt = System.currentTimeMillis()))
            addEventOnce(quoteId, DocumentType.QUOTE, DocumentEvent.CONVERTED, invoiceId)
        }
    }

    private suspend fun changeQuote(id: String, target: String, event: String): Result<Unit> = runCatching {
        database.withTransaction {
            val quote = database.quoteDao().getQuoteByIdOnce(id) ?: error("Quote not found")
            if (quote.status == target) return@withTransaction
            if (!canTransitionQuote(quote.status, target)) error("This quote cannot be marked ${target.lowercase()} from its current status")
            val now = System.currentTimeMillis()
            database.quoteDao().updateQuote(quote.copy(status = target, sentAt = if (target == QuoteStatus.SENT) now else quote.sentAt, acceptedAt = if (target == QuoteStatus.ACCEPTED) now else quote.acceptedAt, declinedAt = if (target == QuoteStatus.REJECTED) now else quote.declinedAt, updatedAt = now))
            addEventOnce(id, DocumentType.QUOTE, event)
        }
    }

    private suspend fun changeInvoice(id: String, target: String, event: String): Result<Unit> = runCatching {
        database.withTransaction {
            val invoice = database.invoiceDao().getInvoiceByIdOnce(id) ?: error("Invoice not found")
            if (invoice.status == target) return@withTransaction
            if (invoice.status == InvoiceStatus.PAID && target != InvoiceStatus.PAID) error("A paid invoice cannot be changed")
            val now = System.currentTimeMillis()
            database.invoiceDao().updateInvoice(invoice.copy(status = target, sentAt = if (target == InvoiceStatus.SENT) now else invoice.sentAt, paidDate = if (target == InvoiceStatus.PAID) now else invoice.paidDate, voidedAt = if (target == InvoiceStatus.CANCELLED) now else invoice.voidedAt, updatedAt = now))
            addEventOnce(id, DocumentType.INVOICE, event)
        }
    }

    private suspend fun addEventOnce(id: String, type: String, event: String, relatedId: String? = null) {
        if (database.documentActivityDao().findEvent(id, type, event) == null) database.documentActivityDao().insert(DocumentActivityEntity(documentId = id, documentType = type, eventType = event, relatedDocumentId = relatedId))
    }
}

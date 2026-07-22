package com.servicesphere.data.repository

import androidx.room.withTransaction
import com.servicesphere.data.local.AppDatabase
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.DocumentActivityEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus
import com.servicesphere.domain.model.LineItemParentType
import java.util.UUID
import kotlinx.coroutines.flow.first

sealed interface ConversionResult<out T> {
    data class Created<T>(val value: T) : ConversionResult<T>
    data class Existing<T>(val value: T) : ConversionResult<T>
    data object SourceNotFound : ConversionResult<Nothing>
    data class Failure(val message: String) : ConversionResult<Nothing>
}

/**
 * The one place that creates connected records.  A Room transaction makes repeated taps
 * idempotent even when the app is offline or the screen is recreated mid-operation.
 */
class WorkflowRepository(private val database: AppDatabase) {
    suspend fun createJobFromQuote(quoteId: String): ConversionResult<JobEntity> = try {
        database.withTransaction {
            database.jobDao().getJobBySourceQuoteIdOnce(quoteId)?.let { return@withTransaction ConversionResult.Existing(it) }
            val quote = database.quoteDao().getQuoteByIdOnce(quoteId) ?: return@withTransaction ConversionResult.SourceNotFound
            val now = System.currentTimeMillis()
            val job = JobEntity(
                id = UUID.randomUUID().toString(),
                clientId = quote.clientId,
                sourceQuoteId = quote.id,
                title = "Quote ${quote.quoteNumber}",
                description = quote.notes,
                estimatedPrice = quote.total,
                createdAt = now,
                updatedAt = now
            )
            database.jobDao().insertJob(job)
            ConversionResult.Created(job)
        }
    } catch (error: Exception) {
        ConversionResult.Failure(error.message ?: "Unable to create job from quote")
    }

    suspend fun createInvoiceFromJob(jobId: String): ConversionResult<InvoiceEntity> = try {
        database.withTransaction {
            database.invoiceDao().getInvoiceBySourceJobIdOnce(jobId)?.let { return@withTransaction ConversionResult.Existing(it) }
            val job = database.jobDao().getJobByIdOnce(jobId) ?: return@withTransaction ConversionResult.SourceNotFound
            val sourceQuote = job.sourceQuoteId?.let { database.quoteDao().getQuoteByIdOnce(it) }
            val profile = database.businessProfileDao().getBusinessProfileOnce()
                ?: return@withTransaction ConversionResult.Failure("Set up your business before creating an invoice")
            val now = System.currentTimeMillis()
            val invoice = InvoiceEntity(
                id = UUID.randomUUID().toString(),
                clientId = job.clientId,
                jobId = job.id,
                quoteId = sourceQuote?.id,
                invoiceNumber = "${profile.invoicePrefix.ifBlank { "INV-" }}${profile.nextInvoiceNumber.toString().padStart(4, '0')}",
                status = InvoiceStatus.DRAFT,
                issueDate = now,
                subtotal = sourceQuote?.subtotal ?: (job.estimatedPrice ?: 0.0),
                discountAmount = sourceQuote?.discountAmount ?: 0.0,
                taxAmount = sourceQuote?.taxAmount ?: 0.0,
                total = sourceQuote?.total ?: (job.estimatedPrice ?: 0.0),
                notes = sourceQuote?.notes ?: job.description,
                createdAt = now,
                updatedAt = now
            )
            database.invoiceDao().insertInvoice(invoice)
            val sourceItems = sourceQuote?.let { database.lineItemDao().observeLineItems(it.id, LineItemParentType.QUOTE).firstValue() }.orEmpty()
            val invoiceItems = sourceItems.mapIndexed { index, item ->
                item.copy(id = UUID.randomUUID().toString(), parentId = invoice.id, parentType = LineItemParentType.INVOICE, sortOrder = index, createdAt = now, updatedAt = now)
            }.ifEmpty {
                job.estimatedPrice?.let { price -> listOf(LineItemEntity(parentId = invoice.id, parentType = LineItemParentType.INVOICE, description = job.title, unitPrice = price, total = price, createdAt = now, updatedAt = now)) }.orEmpty()
            }
            if (invoiceItems.isNotEmpty()) database.lineItemDao().insertLineItems(invoiceItems)
            database.businessProfileDao().updateBusinessProfile(profile.copy(nextInvoiceNumber = profile.nextInvoiceNumber + 1, updatedAt = now))
            ConversionResult.Created(invoice)
        }
    } catch (error: Exception) {
        ConversionResult.Failure(error.message ?: "Unable to create invoice from job")
    }

    suspend fun createInvoiceFromQuote(quoteId: String): ConversionResult<InvoiceEntity> = try {
        database.withTransaction {
            database.invoiceDao().getInvoiceBySourceQuoteIdOnce(quoteId)?.let { return@withTransaction ConversionResult.Existing(it) }
            val quote = database.quoteDao().getQuoteByIdOnce(quoteId) ?: return@withTransaction ConversionResult.SourceNotFound
            if (quote.status != QuoteStatus.ACCEPTED) return@withTransaction ConversionResult.Failure("Accept this quote before converting it to an invoice")
            val profile = database.businessProfileDao().getBusinessProfileOnce()
                ?: return@withTransaction ConversionResult.Failure("Set up your business before creating an invoice")
            val items = database.lineItemDao().observeLineItems(quoteId, LineItemParentType.QUOTE).firstValue()
            if (items.isEmpty()) return@withTransaction ConversionResult.Failure("Add at least one line item before converting this quote")
            val now = System.currentTimeMillis()
            val invoice = InvoiceEntity(
                id = UUID.randomUUID().toString(), clientId = quote.clientId, jobId = quote.jobId, quoteId = quote.id,
                invoiceNumber = "${profile.invoicePrefix.ifBlank { "INV-" }}${profile.nextInvoiceNumber.toString().padStart(4, '0')}", status = InvoiceStatus.DRAFT,
                issueDate = now, subtotal = quote.subtotal, discountAmount = quote.discountAmount, taxAmount = quote.taxAmount, total = quote.total,
                notes = quote.notes, createdAt = now, updatedAt = now
            )
            database.invoiceDao().insertInvoice(invoice)
            database.lineItemDao().insertLineItems(items.map { it.copy(id = UUID.randomUUID().toString(), parentId = invoice.id, parentType = LineItemParentType.INVOICE, createdAt = now, updatedAt = now) })
            database.businessProfileDao().updateBusinessProfile(profile.copy(nextInvoiceNumber = profile.nextInvoiceNumber + 1, updatedAt = now))
            database.quoteDao().updateQuote(quote.copy(status = QuoteStatus.CONVERTED_TO_INVOICE, convertedInvoiceId = invoice.id, updatedAt = now))
            database.documentActivityDao().insert(DocumentActivityEntity(documentId = quote.id, documentType = "QUOTE", eventType = "CONVERTED", relatedDocumentId = invoice.id, createdAt = now))
            ConversionResult.Created(invoice)
        }
    } catch (error: Exception) {
        ConversionResult.Failure(error.message ?: "Unable to convert quote")
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = first()

package com.servicesphere.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.model.QuoteStatus
import java.util.UUID

private fun nowMillis(): Long = System.currentTimeMillis()
private fun uuid(): String = UUID.randomUUID().toString()

@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey val id: String = "default_business",
    val businessName: String,
    val ownerName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val website: String? = null,
    val taxNumber: String? = null,
    val logoUri: String? = null,
    val currencyCode: String = "USD",
    val taxRatePercent: Double = 0.0,
    val paymentInstructions: String? = null,
    val quotePrefix: String = "Q-",
    val invoicePrefix: String = "INV-",
    val nextQuoteNumber: Int = 1,
    val nextInvoiceNumber: Int = 1,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String = uuid(),
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "jobs",
    indices = [
        Index("clientId"),
        Index("status"),
        Index("scheduledAt")
    ]
)
data class JobEntity(
    @PrimaryKey val id: String = uuid(),
    val clientId: String? = null,
    val title: String,
    val description: String? = null,
    val address: String? = null,
    val scheduledAt: Long? = null,
    val status: String = JobStatus.NEW,
    val estimatedPrice: Double? = null,
    val internalNotes: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "job_reminders",
    indices = [
        Index("jobId"),
        Index("reminderTimeMillis"),
        Index("isEnabled"),
        Index("hasFired")
    ]
)
data class JobReminderEntity(
    @PrimaryKey val id: String = uuid(),
    val jobId: String,
    val reminderType: String,
    val reminderTimeMillis: Long,
    val isEnabled: Boolean = true,
    val hasFired: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "quotes",
    indices = [
        Index("clientId"),
        Index("jobId"),
        Index("status")
    ]
)
data class QuoteEntity(
    @PrimaryKey val id: String = uuid(),
    val clientId: String? = null,
    val jobId: String? = null,
    val quoteNumber: String,
    val status: String = QuoteStatus.DRAFT,
    val issueDate: Long = nowMillis(),
    val validUntil: Long? = null,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "invoices",
    indices = [
        Index("clientId"),
        Index("jobId"),
        Index("quoteId"),
        Index("status"),
        Index("dueDate")
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String = uuid(),
    val clientId: String? = null,
    val jobId: String? = null,
    val quoteId: String? = null,
    val invoiceNumber: String,
    val status: String = InvoiceStatus.DRAFT,
    val issueDate: Long = nowMillis(),
    val dueDate: Long? = null,
    val paidDate: Long? = null,
    val paymentMethod: String? = null,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "line_items",
    indices = [
        Index("parentId"),
        Index("parentType")
    ]
)
data class LineItemEntity(
    @PrimaryKey val id: String = uuid(),
    val parentId: String,
    val parentType: String,
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val total: Double = quantity * unitPrice,
    val sortOrder: Int = 0,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

@Entity(
    tableName = "job_photos",
    indices = [Index("jobId")]
)
data class JobPhotoEntity(
    @PrimaryKey val id: String = uuid(),
    val jobId: String,
    val localUri: String,
    val caption: String? = null,
    val createdAt: Long = nowMillis()
)

@Entity(
    tableName = "signatures",
    indices = [
        Index("jobId"),
        Index("invoiceId")
    ]
)
data class SignatureEntity(
    @PrimaryKey val id: String = uuid(),
    val jobId: String? = null,
    val invoiceId: String? = null,
    val localUri: String,
    val signedBy: String? = null,
    val approvalText: String? = null,
    val createdAt: Long = nowMillis()
)

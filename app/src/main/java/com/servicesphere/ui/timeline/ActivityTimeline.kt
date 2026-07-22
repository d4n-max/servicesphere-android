package com.servicesphere.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobPhotoEntity
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereCard
import java.text.DateFormat
import java.util.Date

enum class TimelineTarget { QUOTE, JOB, INVOICE }
enum class ActivityType { QUOTE, JOB, INVOICE, SCHEDULE, COMPLETED, PAID, PHOTO, SIGNATURE, REMINDER }
data class ActivityTimelineItem(val id: String, val timestamp: Long, val type: ActivityType, val title: String, val description: String? = null, val target: TimelineTarget? = null, val targetId: String? = null)

fun buildTimeline(
    quotes: List<QuoteEntity>, jobs: List<JobEntity>, invoices: List<InvoiceEntity>,
    photos: List<JobPhotoEntity> = emptyList(), signatures: List<SignatureEntity> = emptyList(), reminders: List<JobReminderEntity> = emptyList()
): List<ActivityTimelineItem> = buildList {
    quotes.forEach { quote -> add(ActivityTimelineItem("quote-${quote.id}", quote.createdAt, ActivityType.QUOTE, "Quote ${quote.quoteNumber} created", quote.status.lowercase().replace('_', ' '), TimelineTarget.QUOTE, quote.id)) }
    jobs.forEach { job ->
        add(ActivityTimelineItem("job-${job.id}", job.createdAt, ActivityType.JOB, if (job.sourceQuoteId != null) "Job created from quote" else "Job created", job.title, TimelineTarget.JOB, job.id))
        job.scheduledAt?.let { add(ActivityTimelineItem("schedule-${job.id}", it, ActivityType.SCHEDULE, "Job scheduled", job.title, TimelineTarget.JOB, job.id)) }
        if (job.status in setOf(JobStatus.COMPLETED, JobStatus.INVOICED, JobStatus.PAID)) add(ActivityTimelineItem("completed-${job.id}", job.updatedAt, ActivityType.COMPLETED, "Job completed", job.title, TimelineTarget.JOB, job.id))
    }
    invoices.forEach { invoice ->
        add(ActivityTimelineItem("invoice-${invoice.id}", invoice.createdAt, ActivityType.INVOICE, "Invoice ${invoice.invoiceNumber} created", null, TimelineTarget.INVOICE, invoice.id))
        if (invoice.status == InvoiceStatus.PAID) add(ActivityTimelineItem("paid-${invoice.id}", invoice.paidDate ?: invoice.updatedAt, ActivityType.PAID, "Invoice marked paid", invoice.invoiceNumber, TimelineTarget.INVOICE, invoice.id))
    }
    photos.forEach { add(ActivityTimelineItem("photo-${it.id}", it.createdAt, ActivityType.PHOTO, "Photo proof added", it.caption, TimelineTarget.JOB, it.jobId)) }
    signatures.forEach { signature -> add(ActivityTimelineItem("signature-${signature.id}", signature.createdAt, ActivityType.SIGNATURE, "Signature captured", signature.signedBy, signature.jobId?.let { TimelineTarget.JOB } ?: signature.invoiceId?.let { TimelineTarget.INVOICE }, signature.jobId ?: signature.invoiceId)) }
    reminders.forEach { add(ActivityTimelineItem("reminder-${it.id}", it.createdAt, ActivityType.REMINDER, if (it.hasFired) "Reminder completed" else "Reminder scheduled", null, TimelineTarget.JOB, it.jobId)) }
}.sortedByDescending { it.timestamp }

@Composable
fun ActivityTimeline(items: List<ActivityTimelineItem>, onOpen: (TimelineTarget, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Activity")
        items.forEach { item ->
            ServiceSphereCard(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = item.title },
                onClick = item.target?.let { target -> item.targetId?.let { id -> { onOpen(target, id) } } }
            ) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(iconFor(item.type), contentDescription = null)
                    Column {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        item.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.timestamp)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun iconFor(type: ActivityType) = when (type) {
    ActivityType.QUOTE -> Icons.Filled.Description
    ActivityType.JOB -> Icons.Filled.HomeRepairService
    ActivityType.INVOICE -> Icons.AutoMirrored.Filled.ReceiptLong
    ActivityType.SCHEDULE, ActivityType.REMINDER -> Icons.Filled.Schedule
    ActivityType.COMPLETED -> Icons.Filled.TaskAlt
    ActivityType.PAID, ActivityType.SIGNATURE -> Icons.Filled.Verified
    ActivityType.PHOTO -> Icons.Filled.Photo
}

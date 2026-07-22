package com.servicesphere.domain.today

import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.model.QuoteStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure, local-time prioritisation for the field-work home screen. */
class TodayCockpitCalculator(private val zoneId: ZoneId) {
    fun calculate(
        today: LocalDate,
        nowMillis: Long,
        jobs: List<JobEntity>,
        clients: List<ClientEntity>,
        invoices: List<InvoiceEntity>,
        quotes: List<QuoteEntity>,
        reminders: List<JobReminderEntity>
    ): TodayCockpitState {
        val names = clients.associate { it.id to it.name }
        val active = jobs.filter { it.isScheduledOn(today) && it.status !in terminalJobStatuses }
            .sortedWith(compareBy<JobEntity> { it.priority(nowMillis) }.thenBy { it.scheduledAt ?: Long.MAX_VALUE }.thenBy { it.id })
            .map { it.toJobItem(names, reminders.any { reminder -> reminder.jobId == it.id && reminder.isDue(today, nowMillis) }) }
        val completed = jobs.filter { it.isScheduledOn(today) && it.status == JobStatus.COMPLETED }
            .sortedByDescending { it.scheduledAt ?: it.updatedAt }
            .map { it.toJobItem(names, false) }
        val featuredJobIds = active.map { it.id }.toSet()
        val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val overdue = invoices.filter { it.dueDate != null && it.dueDate < startOfToday && it.status !in terminalInvoiceStatuses }
            .sortedWith(compareBy<InvoiceEntity> { it.dueDate }.thenBy { it.id })
            .map { invoice -> TodayInvoiceAlert(invoice.id, invoice.invoiceNumber, names[invoice.clientId], invoice.total, invoice.dueDate!!) }
        val followUps = quotes.filter { it.needsFollowUp(today, nowMillis) }
            .sortedWith(compareBy<QuoteEntity> { it.validUntil ?: it.sentAt ?: it.issueDate }.thenBy { it.id })
            .map { quote -> TodayQuoteFollowUp(quote.id, quote.quoteNumber, names[quote.clientId], quote.total, quote.followUpReason(today, nowMillis)) }
        val dueReminders = reminders.filter { it.isDue(today, nowMillis) && it.jobId !in featuredJobIds }
            .sortedBy { it.reminderTimeMillis }
            .map { reminder -> TodayReminderItem(reminder.id, reminder.jobId, reminder.reminderTimeMillis) }
        return TodayCockpitState(active, active.firstOrNull { it.status == JobStatus.IN_PROGRESS } ?: active.firstOrNull(), completed, overdue, followUps, dueReminders)
    }

    private fun JobEntity.isScheduledOn(today: LocalDate): Boolean = scheduledAt?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() == today } == true
    private fun JobEntity.priority(now: Long): Int = when (status) {
        JobStatus.IN_PROGRESS -> 0
        JobStatus.SCHEDULED -> if ((scheduledAt ?: Long.MAX_VALUE) <= now) 1 else 2
        else -> 3
    }
    private fun JobEntity.toJobItem(names: Map<String, String>, hasReminder: Boolean) = TodayJobItem(id, title, names[clientId], address, scheduledAt, status, internalNotes, hasReminder)
    private fun JobReminderEntity.isDue(today: LocalDate, now: Long): Boolean = isEnabled && !hasFired && reminderTimeMillis <= now && Instant.ofEpochMilli(reminderTimeMillis).atZone(zoneId).toLocalDate() <= today
    private fun QuoteEntity.needsFollowUp(today: LocalDate, now: Long): Boolean {
        if (status !in actionableQuoteStatuses) return false
        return validUntil?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() <= today } == true || (status == QuoteStatus.SENT && (sentAt ?: issueDate) <= now - QUOTE_FOLLOW_UP_DAYS * DAY_MILLIS)
    }
    private fun QuoteEntity.followUpReason(today: LocalDate, now: Long): String = if (validUntil?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() <= today } == true) "Expires today or has expired" else "Sent ${QUOTE_FOLLOW_UP_DAYS}+ days ago"

    companion object {
        const val QUOTE_FOLLOW_UP_DAYS = 7L
        private const val DAY_MILLIS = 86_400_000L
        private val terminalJobStatuses = setOf(JobStatus.CANCELLED, JobStatus.COMPLETED, JobStatus.INVOICED, JobStatus.PAID)
        private val terminalInvoiceStatuses = setOf(InvoiceStatus.PAID, InvoiceStatus.CANCELLED)
        private val actionableQuoteStatuses = setOf(QuoteStatus.SENT, QuoteStatus.DRAFT)
    }
}

data class TodayCockpitState(
    val activeJobs: List<TodayJobItem>,
    val nextJob: TodayJobItem?,
    val completedJobs: List<TodayJobItem>,
    val overdueInvoices: List<TodayInvoiceAlert>,
    val quoteFollowUps: List<TodayQuoteFollowUp>,
    val dueReminders: List<TodayReminderItem>
) {
    val hasAlerts get() = overdueInvoices.isNotEmpty() || quoteFollowUps.isNotEmpty() || dueReminders.isNotEmpty()
    val isCaughtUp get() = activeJobs.isEmpty() && !hasAlerts
}
data class TodayJobItem(val id: String, val title: String, val clientName: String?, val address: String?, val scheduledAt: Long?, val status: String, val notes: String?, val hasDueReminder: Boolean)
data class TodayInvoiceAlert(val id: String, val invoiceNumber: String, val clientName: String?, val total: Double, val dueDate: Long)
data class TodayQuoteFollowUp(val id: String, val quoteNumber: String, val clientName: String?, val total: Double, val reason: String)
data class TodayReminderItem(val id: String, val jobId: String, val dueAt: Long)

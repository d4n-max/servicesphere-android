package com.servicesphere.domain.today

import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.model.QuoteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodayCockpitCalculatorTest {
    private val zone = ZoneId.of("Europe/Bucharest")
    private val date = LocalDate.of(2026, 7, 22)
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun at(hour: Int) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun `in progress job is next job`() {
        val state = calculate(listOf(JobEntity(id = "scheduled", title = "Later", scheduledAt = at(9), status = JobStatus.SCHEDULED), JobEntity(id = "active", title = "Now", scheduledAt = at(14), status = JobStatus.IN_PROGRESS)))
        assertEquals("active", state.nextJob?.id)
    }

    @Test fun `earliest incomplete job is next when none in progress`() {
        val state = calculate(listOf(JobEntity(id = "later", title = "Later", scheduledAt = at(14), status = JobStatus.SCHEDULED), JobEntity(id = "first", title = "First", scheduledAt = at(9), status = JobStatus.SCHEDULED)))
        assertEquals("first", state.nextJob?.id)
    }

    @Test fun `completed and cancelled jobs are excluded from active schedule`() {
        val state = calculate(listOf(JobEntity(id = "done", title = "Done", scheduledAt = at(9), status = JobStatus.COMPLETED), JobEntity(id = "cancelled", title = "Cancelled", scheduledAt = at(10), status = JobStatus.CANCELLED)))
        assertTrue(state.activeJobs.isEmpty())
        assertEquals(listOf("done"), state.completedJobs.map { it.id })
        assertNull(state.nextJob)
    }

    @Test fun `jobs use local date boundaries`() {
        val beforeStart = start - 1
        val state = calculate(listOf(JobEntity(id = "previous", title = "Previous", scheduledAt = beforeStart), JobEntity(id = "today", title = "Today", scheduledAt = start)))
        assertEquals(listOf("today"), state.activeJobs.map { it.id })
    }

    @Test fun `overdue unpaid invoice is included while paid and void are excluded`() {
        val invoices = listOf(InvoiceEntity(id = "due", invoiceNumber = "1", dueDate = start - 1, status = InvoiceStatus.UNPAID), InvoiceEntity(id = "paid", invoiceNumber = "2", dueDate = start - 1, status = InvoiceStatus.PAID), InvoiceEntity(id = "void", invoiceNumber = "3", dueDate = start - 1, status = InvoiceStatus.CANCELLED))
        assertEquals(listOf("due"), calculate(invoices = invoices).overdueInvoices.map { it.id })
    }

    @Test fun `sent quote follows up after configured interval`() {
        val quote = QuoteEntity(id = "quote", quoteNumber = "Q-1", status = QuoteStatus.SENT, sentAt = start - TodayCockpitCalculator.QUOTE_FOLLOW_UP_DAYS * 86_400_000L)
        assertEquals(listOf("quote"), calculate(quotes = listOf(quote)).quoteFollowUps.map { it.id })
    }

    @Test fun `reminder for an already featured job is not duplicated`() {
        val job = JobEntity(id = "job", title = "Today", scheduledAt = at(9), status = JobStatus.SCHEDULED)
        val reminder = JobReminderEntity(id = "reminder", jobId = "job", reminderType = "AT_TIME", reminderTimeMillis = at(8))
        assertTrue(calculate(jobs = listOf(job), reminders = listOf(reminder)).dueReminders.isEmpty())
    }

    @Test fun `empty workspace is caught up`() {
        val state = calculate()
        assertTrue(state.isCaughtUp)
        assertFalse(state.hasAlerts)
    }

    private fun calculate(jobs: List<JobEntity> = emptyList(), invoices: List<InvoiceEntity> = emptyList(), quotes: List<QuoteEntity> = emptyList(), reminders: List<JobReminderEntity> = emptyList()) = TodayCockpitCalculator(zone).calculate(date, at(8), jobs, emptyList<ClientEntity>(), invoices, quotes, reminders)
}

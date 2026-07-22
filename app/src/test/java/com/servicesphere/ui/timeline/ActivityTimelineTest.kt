package com.servicesphere.ui.timeline

import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.QuoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActivityTimelineTest {
    @Test fun `client timeline joins related records newest first`() {
        val quote = QuoteEntity(id = "quote", quoteNumber = "Q-1", clientId = "client", createdAt = 100)
        val job = JobEntity(id = "job", title = "Fan", clientId = "client", sourceQuoteId = "quote", createdAt = 200)
        val invoice = InvoiceEntity(id = "invoice", invoiceNumber = "INV-1", clientId = "client", jobId = "job", quoteId = "quote", createdAt = 300)

        val timeline = buildTimeline(listOf(quote), listOf(job), listOf(invoice))

        assertEquals("invoice-invoice", timeline.first().id)
        assertEquals(TimelineTarget.JOB, timeline.first { it.id == "job-job" }.target)
        assertEquals(TimelineTarget.QUOTE, timeline.first { it.id == "quote-quote" }.target)
    }

    @Test fun `timeline does not manufacture optional events`() {
        val job = JobEntity(id = "job", title = "Independent work", createdAt = 100)

        val timeline = buildTimeline(emptyList(), listOf(job), emptyList())

        assertEquals(listOf("job-job"), timeline.map { it.id })
        assertFalse(timeline.any { it.type == ActivityType.PAID || it.type == ActivityType.SCHEDULE })
    }
}

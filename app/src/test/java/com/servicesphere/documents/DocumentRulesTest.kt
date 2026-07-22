package com.servicesphere.documents

import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DocumentRulesTest {
    @Test fun `safe filename is professional and strips unsafe characters`() {
        assertEquals("Cedar-Stone-Services-Quote-Q-1024.pdf", documentFileName("Cedar & Stone / Services", "Quote", "Q-1024"))
    }

    @Test fun `quote transitions only allow trustworthy lifecycle`() {
        assertTrue(canTransitionQuote(QuoteStatus.DRAFT, QuoteStatus.SENT))
        assertTrue(canTransitionQuote(QuoteStatus.SENT, QuoteStatus.ACCEPTED))
        assertFalse(canTransitionQuote(QuoteStatus.CONVERTED_TO_INVOICE, QuoteStatus.DRAFT))
    }

    @Test fun `invoice overdue is display-only for unpaid invoices`() {
        assertEquals(InvoiceStatus.OVERDUE, displayedInvoiceStatus(InvoiceStatus.SENT, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
        assertEquals(InvoiceStatus.PAID, displayedInvoiceStatus(InvoiceStatus.PAID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
    }
}

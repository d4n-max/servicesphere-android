package com.servicesphere.data.local

import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.model.LineItemParentType
import com.servicesphere.domain.model.PaymentMethod
import com.servicesphere.domain.model.QuoteStatus
import java.time.LocalDate
import java.time.ZoneId

class DemoDataSeeder(
    private val businessProfileDao: BusinessProfileDao,
    private val clientDao: ClientDao,
    private val jobDao: JobDao,
    private val quoteDao: QuoteDao,
    private val invoiceDao: InvoiceDao,
    private val lineItemDao: LineItemDao
) {
    suspend fun seedIfEmpty() {
        val now = System.currentTimeMillis()

        if (businessProfileDao.getBusinessProfileOnce() == null) {
            businessProfileDao.upsertBusinessProfile(
                BusinessProfileEntity(
                    businessName = "ServiceSphere Demo Co.",
                    ownerName = "Alex Morgan",
                    phone = "(555) 014-2038",
                    email = "hello@servicesphere.example",
                    address = "118 Market Street",
                    website = "servicesphere.example",
                    taxNumber = "TAX-1001",
                    currencyCode = "USD",
                    taxRatePercent = 8.25,
                    paymentInstructions = "Payment due by the invoice due date."
                )
            )
        }

        if (clientDao.getClientCountOnce() > 0) return

        val clientA = ClientEntity(
            name = "Ada Johnson",
            phone = "(555) 412-8810",
            email = "ada@example.com",
            address = "44 Cedar Lane",
            notes = "Prefers morning appointments."
        )
        val clientB = ClientEntity(
            name = "Ben Carter",
            phone = "(555) 998-1204",
            email = "ben@example.com",
            address = "9 Oak Terrace",
            notes = "Gate code 2048."
        )
        clientDao.insertClient(clientA)
        clientDao.insertClient(clientB)

        val todayNine = LocalDate.now().atTime(9, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tomorrowOne = LocalDate.now().plusDays(1).atTime(13, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextWeek = LocalDate.now().plusDays(5).atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val jobA = JobEntity(
            clientId = clientA.id,
            title = "Kitchen faucet repair",
            description = "Replace leaking faucet cartridge.",
            address = clientA.address,
            scheduledAt = todayNine,
            status = JobStatus.SCHEDULED,
            estimatedPrice = 185.0
        )
        val jobB = JobEntity(
            clientId = clientB.id,
            title = "Move-out cleaning",
            description = "Two bedroom apartment deep clean.",
            address = clientB.address,
            scheduledAt = tomorrowOne,
            status = JobStatus.IN_PROGRESS,
            estimatedPrice = 260.0
        )
        val jobC = JobEntity(
            clientId = clientA.id,
            title = "Bathroom fan inspection",
            description = "Diagnose intermittent fan noise.",
            address = clientA.address,
            scheduledAt = nextWeek,
            status = JobStatus.NEW,
            estimatedPrice = 95.0
        )
        jobDao.insertJob(jobA)
        jobDao.insertJob(jobB)
        jobDao.insertJob(jobC)

        val quote = QuoteEntity(
            clientId = clientA.id,
            jobId = jobC.id,
            quoteNumber = "Q-0001",
            status = QuoteStatus.DRAFT,
            issueDate = now,
            validUntil = LocalDate.now().plusDays(21).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            subtotal = 320.0,
            discountAmount = 20.0,
            taxAmount = 24.75,
            total = 324.75,
            notes = "Includes labor and standard materials."
        )
        quoteDao.insertQuote(quote)

        val invoice = InvoiceEntity(
            clientId = clientB.id,
            jobId = jobB.id,
            invoiceNumber = "INV-0001",
            status = InvoiceStatus.UNPAID,
            issueDate = now,
            dueDate = LocalDate.now().plusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            subtotal = 260.0,
            discountAmount = 0.0,
            taxAmount = 21.45,
            total = 281.45,
            notes = "Thank you for your business."
        )
        invoiceDao.insertInvoice(invoice)

        lineItemDao.insertLineItems(
            listOf(
                LineItemEntity(
                    parentId = quote.id,
                    parentType = LineItemParentType.QUOTE,
                    description = "Labor",
                    quantity = 3.0,
                    unitPrice = 85.0,
                    total = 255.0,
                    sortOrder = 1
                ),
                LineItemEntity(
                    parentId = invoice.id,
                    parentType = LineItemParentType.INVOICE,
                    description = "Move-out cleaning service",
                    quantity = 1.0,
                    unitPrice = 260.0,
                    total = 260.0,
                    sortOrder = 1
                )
            )
        )
    }
}

package com.servicesphere.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.LineItemRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.data.repository.SignatureRepository
import com.servicesphere.domain.model.LineItemParentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceSpherePdfGenerator(
    private val businessRepository: BusinessRepository,
    private val clientRepository: ClientRepository,
    private val jobRepository: JobRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: LineItemRepository,
    private val signatureRepository: SignatureRepository
) {
    suspend fun generateQuotePdf(context: Context, quoteId: String, includeBusinessLogo: Boolean = false, showFreeWatermark: Boolean = true): PdfResult = withContext(Dispatchers.IO) {
        runCatching {
            val quote = quoteRepository.getQuoteByIdOnce(quoteId) ?: throw IllegalStateException("This quote no longer exists.")
            val profile = businessRepository.getBusinessProfileOnce()
            val client = quote.clientId?.let { clientRepository.getClientByIdOnce(it) }
            val job = quote.jobId?.let { jobRepository.getJobByIdOnce(it) }
            val items = lineItemRepository.observeLineItems(quote.id, LineItemParentType.QUOTE).first()
            require(items.isNotEmpty()) { "Add at least one line item before generating this quote." }
            val signature = quote.jobId?.let { signatureRepository.observeSignaturesForJob(it).first().firstOrNull() }
            val data = quote.toPdfData(profile, client, job, items, signature, includeBusinessLogo, showFreeWatermark)
            writePdf(context, data.businessInfo.businessName, "Quote", data.quoteNumber) { renderer -> renderer.renderQuote(data) }
        }.getOrElse { error -> PdfResult(false, errorMessage = error.message ?: "Couldn't generate quote PDF") }
    }

    suspend fun generateInvoicePdf(context: Context, invoiceId: String, includeBusinessLogo: Boolean = false, showFreeWatermark: Boolean = true): PdfResult = withContext(Dispatchers.IO) {
        runCatching {
            val invoice = invoiceRepository.getInvoiceByIdOnce(invoiceId) ?: throw IllegalStateException("This invoice no longer exists.")
            val profile = businessRepository.getBusinessProfileOnce()
            val client = invoice.clientId?.let { clientRepository.getClientByIdOnce(it) }
            val job = invoice.jobId?.let { jobRepository.getJobByIdOnce(it) }
            val quote = invoice.quoteId?.let { quoteRepository.getQuoteByIdOnce(it) }
            val items = lineItemRepository.observeLineItems(invoice.id, LineItemParentType.INVOICE).first()
            require(items.isNotEmpty()) { "Add at least one line item before generating this invoice." }
            val invoiceSignature = signatureRepository.observeSignaturesForInvoice(invoice.id).first().firstOrNull()
            val jobSignature = invoice.jobId?.let { signatureRepository.observeSignaturesForJob(it).first().firstOrNull() }
            val data = invoice.toPdfData(profile, client, job, quote, items, invoiceSignature ?: jobSignature, includeBusinessLogo, showFreeWatermark)
            writePdf(context, data.businessInfo.businessName, "Invoice", data.invoiceNumber) { renderer -> renderer.renderInvoice(data) }
        }.getOrElse { error -> PdfResult(false, errorMessage = error.message ?: "Couldn't generate invoice PDF") }
    }

    private fun writePdf(context: Context, businessName: String, type: String, number: String, render: (PdfRenderer) -> Unit): PdfResult {
        val directory = File(context.filesDir, "pdfs").apply { mkdirs() }
        val baseName = "${sanitizeFilePart(businessName)}-${sanitizeFilePart(type)}-${sanitizeFilePart(number)}"
        val file = File(directory, "$baseName.pdf")
        val document = PdfDocument()
        try {
            val renderer = PdfRenderer(document, context)
            render(renderer)
            file.outputStream().use { document.writeTo(it) }
            return PdfResult(success = true, filePath = file.absolutePath, fileName = file.name)
        } finally {
            document.close()
        }
    }
}

private class PdfRenderer(private val document: PdfDocument, private val context: Context) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 42f
    private var pageNumber = 0
    private var currentPage: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var y = margin
    private val primary = Color.rgb(124, 58, 237)
    private val secondary = Color.rgb(67, 56, 202)
    private val text = Color.rgb(17, 24, 39)
    private val muted = Color.rgb(107, 114, 128)
    private val border = Color.rgb(229, 231, 235)
    private val light = Color.rgb(248, 250, 252)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun renderQuote(data: QuotePdfData) {
        startPage()
        header(data.businessInfo, "QUOTE", data.quoteNumber, data.status)
        metaRows(listOf("Issue date" to data.issueDate, "Valid until" to (data.validUntil ?: "Not set")))
        partySections(data.clientInfo, data.jobInfo)
        lineItems(data.lineItems, data.currencyCode)
        totals(data.subtotal, data.discountAmount, data.taxAmount, data.total, data.currencyCode, "Total")
        notes(data.notes)
        signature(data.signature)
        finishPage(data.showFreeWatermark)
    }

    fun renderInvoice(data: InvoicePdfData) {
        startPage()
        header(data.businessInfo, "INVOICE", data.invoiceNumber, data.status)
        metaRows(
            listOf(
                "Issue date" to data.issueDate,
                "Due date" to (data.dueDate ?: "Not set"),
                "Paid date" to (data.paidDate ?: "Not paid"),
                "Payment method" to (data.paymentMethod ?: "Not set")
            )
        )
        partySections(data.clientInfo, data.jobInfo)
        data.quoteNumber?.let { section("Quote Reference") { bodyLine(it) } }
        lineItems(data.lineItems, data.currencyCode)
        totals(data.subtotal, data.discountAmount, data.taxAmount, data.total, data.currencyCode, "Total Due")
        data.paymentInstructions?.takeIf { it.isNotBlank() }?.let { section("Payment Instructions") { wrappedText(it) } }
        notes(data.notes)
        signature(data.signature)
        finishPage(data.showFreeWatermark)
    }

    private fun startPage() {
        pageNumber += 1
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        currentPage = page
        canvas = page.canvas
        y = margin
        canvas.drawColor(Color.WHITE)
    }

    private fun finishPage(showFreeWatermark: Boolean = false) {
        footer(showFreeWatermark)
        currentPage?.let(document::finishPage)
        currentPage = null
    }

    private fun ensureSpace(required: Float) {
        if (y + required < pageHeight - 62f) return
        finishPage()
        startPage()
    }

    private fun header(business: PdfBusinessInfo, title: String, number: String, status: String) {
        paint.color = primary
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 72f), 12f, 12f, paint)
        val textStart = drawBusinessLogo(business.logoUri, margin + 16f, y + 14f)
        drawText(business.businessName, textStart, y + 28f, 18f, Color.WHITE, bold = true)
        drawText(listOfNotNull(business.ownerName, business.phone, business.email).joinToString(" · "), textStart, y + 50f, 9.5f, Color.WHITE)
        drawText(title, pageWidth - margin - 150f, y + 28f, 20f, Color.WHITE, bold = true)
        drawText(number, pageWidth - margin - 150f, y + 50f, 10f, Color.WHITE)
        y += 90f
        y -= 26f
        statusChip(status, pageWidth - margin - 118f)
        y += 26f
        listOfNotNull(business.address, business.website, business.taxNumber?.let { "Tax: $it" }).forEach { bodyLine(it) }
        y += 8f
    }

    private fun drawBusinessLogo(logoUri: String?, x: Float, top: Float): Float {
        if (logoUri.isNullOrBlank()) return x + 2f
        return runCatching {
            val uri = Uri.parse(logoUri)
            val path = if (uri.scheme == "file") uri.path else logoUri
            val bitmap = BitmapFactory.decodeFile(path) ?: return@runCatching x + 2f
            val rect = RectF(x, top, x + 44f, top + 44f)
            paint.color = Color.WHITE
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            canvas.drawBitmap(bitmap, null, rect, paint)
            x + 56f
        }.getOrDefault(x + 2f)
    }

    private fun metaRows(rows: List<Pair<String, String>>) {
        ensureSpace(48f)
        val colWidth = (pageWidth - margin * 2) / rows.size.coerceAtLeast(1)
        rows.forEachIndexed { index, row ->
            val left = margin + index * colWidth
            drawText(row.first, left, y, 8.5f, muted, bold = true)
            drawText(row.second, left, y + 16f, 10.5f, text)
        }
        y += 42f
    }

    private fun partySections(client: PdfClientInfo?, job: PdfJobInfo?) {
        section("Client") {
            if (client == null) bodyLine("No client linked") else {
                bodyLine(client.name, bold = true)
                listOfNotNull(client.phone, client.email, client.address).forEach { bodyLine(it) }
            }
        }
        job?.let {
            section("Job") {
                bodyLine(it.title, bold = true)
                listOfNotNull(it.address, it.scheduledDate, it.description).forEach { bodyLine(it) }
            }
        }
    }

    private fun lineItems(items: List<PdfLineItem>, currencyCode: String) {
        section("Line Items") {
            tableHeader()
            if (items.isEmpty()) {
                bodyLine("No line items")
            } else {
                items.forEach { item ->
                    ensureSpace(34f)
                    val rowTop = y - 12f
                    paint.color = if (items.indexOf(item) % 2 == 0) light else Color.WHITE
                    canvas.drawRect(margin, rowTop, pageWidth - margin, rowTop + 30f, paint)
                    drawText(item.description.take(58), margin + 8f, y + 5f, 9.5f, text)
                    drawText(item.quantity.stripZero(), 335f, y + 5f, 9.5f, text)
                    drawText(formatCurrency(item.unitPrice, currencyCode), 395f, y + 5f, 9.5f, text)
                    drawText(formatCurrency(item.total, currencyCode), 495f, y + 5f, 9.5f, text)
                    y += 30f
                }
            }
        }
    }

    private fun tableHeader() {
        ensureSpace(24f)
        paint.color = Color.rgb(243, 244, 246)
        canvas.drawRoundRect(RectF(margin, y - 12f, pageWidth - margin, y + 16f), 6f, 6f, paint)
        drawText("Description", margin + 8f, y + 6f, 9f, muted, bold = true)
        drawText("Qty", 335f, y + 6f, 9f, muted, bold = true)
        drawText("Unit Price", 395f, y + 6f, 9f, muted, bold = true)
        drawText("Total", 495f, y + 6f, 9f, muted, bold = true)
        y += 24f
    }

    private fun totals(subtotal: Double, discount: Double, tax: Double, total: Double, currencyCode: String, totalLabel: String) {
        ensureSpace(100f)
        y += 4f
        val left = 360f
        totalLine("Subtotal", subtotal, currencyCode, left)
        totalLine("Discount", discount, currencyCode, left)
        totalLine("Tax", tax, currencyCode, left)
        paint.color = border
        canvas.drawLine(left, y, pageWidth - margin, y, paint)
        y += 16f
        totalLine(totalLabel, total, currencyCode, left, true)
        y += 8f
    }

    private fun totalLine(label: String, amount: Double, currencyCode: String, left: Float, bold: Boolean = false) {
        drawText(label, left, y, 10f, if (bold) text else muted, bold)
        drawText(formatCurrency(amount, currencyCode), pageWidth - margin - 110f, y, 10f, text, bold)
        y += 18f
    }

    private fun notes(notes: String?) {
        notes?.takeIf { it.isNotBlank() }?.let { section("Notes") { wrappedText(it) } }
    }

    private fun signature(signature: PdfSignatureInfo?) {
        section("Signature") {
            if (signature == null) {
                bodyLine("Client signature: not captured")
            } else {
                drawSignatureImage(signature.localUri)
                bodyLine(signature.signedBy?.let { "Signed by: $it" } ?: "Signed by: not provided", bold = true)
                signature.approvalText?.let { wrappedText(it) }
                bodyLine("Date: ${signature.createdDate}")
            }
        }
    }

    private fun drawSignatureImage(localUri: String) {
        runCatching {
            val uri = Uri.parse(localUri)
            val path = if (uri.scheme == "file") uri.path else localUri
            val bitmap = BitmapFactory.decodeFile(path) ?: return
            val rect = RectF(margin, y, margin + 180f, y + 70f)
            canvas.drawBitmap(bitmap, null, rect, paint)
            y += 80f
        }
    }

    private fun section(title: String, content: () -> Unit) {
        ensureSpace(72f)
        drawText(title, margin, y, 12f, secondary, bold = true)
        y += 18f
        val before = y
        content()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = border
        canvas.drawRoundRect(RectF(margin - 8f, before - 16f, pageWidth - margin + 8f, y + 8f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL
        y += 20f
    }

    private fun bodyLine(value: String, bold: Boolean = false) {
        ensureSpace(18f)
        drawText(value, margin, y, 10f, text, bold)
        y += 16f
    }

    private fun wrappedText(value: String) {
        value.chunked(82).forEach { bodyLine(it) }
    }

    private fun statusChip(status: String, x: Float) {
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(x, y, x + 118f, y + 22f), 8f, 8f, paint)
        drawText(formatStatus(status), x + 10f, y + 15f, 8f, primary, bold = true)
    }

    private fun footer(showFreeWatermark: Boolean) {
        paint.color = muted
        drawText(if (showFreeWatermark) "Generated with ServiceSphere Free" else "Generated with ServiceSphere", margin, pageHeight - 30f, 8.5f, muted)
        drawText("Page $pageNumber", pageWidth - margin - 45f, pageHeight - 30f, 8.5f, muted)
    }

    private fun drawText(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText(value, x, y, paint)
    }
}

private fun QuoteEntity.toPdfData(
    profile: BusinessProfileEntity?,
    client: ClientEntity?,
    job: JobEntity?,
    items: List<LineItemEntity>,
    signature: SignatureEntity?,
    includeBusinessLogo: Boolean,
    showFreeWatermark: Boolean
): QuotePdfData {
    val currency = profile?.currencyCode?.ifBlank { null } ?: "USD"
    return QuotePdfData(
        quoteNumber = quoteNumber,
        issueDate = formatDate(issueDate),
        validUntil = validUntil?.let(::formatDate),
        businessInfo = profile.toBusinessInfo(includeBusinessLogo),
        clientInfo = client?.toPdfClient(),
        jobInfo = job?.toPdfJob(),
        lineItems = items.map { it.toPdfLineItem() },
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        total = total,
        notes = notes,
        status = status,
        signature = signature?.toPdfSignature(),
        currencyCode = currency,
        showFreeWatermark = showFreeWatermark
    )
}

private fun InvoiceEntity.toPdfData(
    profile: BusinessProfileEntity?,
    client: ClientEntity?,
    job: JobEntity?,
    quote: QuoteEntity?,
    items: List<LineItemEntity>,
    signature: SignatureEntity?,
    includeBusinessLogo: Boolean,
    showFreeWatermark: Boolean
): InvoicePdfData {
    val currency = profile?.currencyCode?.ifBlank { null } ?: "USD"
    return InvoicePdfData(
        invoiceNumber = invoiceNumber,
        issueDate = formatDate(issueDate),
        dueDate = dueDate?.let(::formatDate),
        paidDate = paidDate?.let(::formatDate),
        paymentMethod = paymentMethod?.let(::formatStatus),
        paymentInstructions = profile?.paymentInstructions,
        businessInfo = profile.toBusinessInfo(includeBusinessLogo),
        clientInfo = client?.toPdfClient(),
        jobInfo = job?.toPdfJob(),
        quoteNumber = quote?.quoteNumber,
        lineItems = items.map { it.toPdfLineItem() },
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        total = total,
        notes = notes,
        status = status,
        signature = signature?.toPdfSignature(),
        currencyCode = currency,
        showFreeWatermark = showFreeWatermark
    )
}

private fun BusinessProfileEntity?.toBusinessInfo(includeBusinessLogo: Boolean): PdfBusinessInfo = PdfBusinessInfo(
    businessName = this?.businessName?.ifBlank { null } ?: "ServiceSphere Business",
    ownerName = this?.ownerName,
    phone = this?.phone,
    email = this?.email,
    address = this?.address,
    website = this?.website,
    taxNumber = this?.taxNumber,
    logoUri = if (includeBusinessLogo) this?.logoUri else null,
    paymentInstructions = this?.paymentInstructions
)

private fun ClientEntity.toPdfClient(): PdfClientInfo = PdfClientInfo(name, phone, email, address)

private fun JobEntity.toPdfJob(): PdfJobInfo = PdfJobInfo(title, address, scheduledAt?.let(::formatDate), description)

private fun LineItemEntity.toPdfLineItem(): PdfLineItem = PdfLineItem(description, quantity, unitPrice, total)

private fun SignatureEntity.toPdfSignature(): PdfSignatureInfo = PdfSignatureInfo(localUri, signedBy, approvalText, formatDate(createdAt))

fun formatCurrency(amount: Double, currencyCode: String): String = "$currencyCode ${"%.2f".format(Locale.US, amount)}"

fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))

fun formatStatus(status: String): String = status.lowercase(Locale.US).split("_").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.US) } }

fun safeText(value: String?, fallback: String = "Not provided"): String = value?.takeIf { it.isNotBlank() } ?: fallback

private fun sanitizeFilePart(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_").trim('_').ifBlank { "document" }

private fun Double.stripZero(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.2f".format(Locale.US, this)

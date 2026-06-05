package com.servicesphere.pdf

data class PdfBrandHeader(
    val appName: String = "ServiceSphere",
    val tagline: String = "Your Complete Field Service Ecosystem",
    val primaryColorHex: String = "#7C3AED",
    val darkSurfaceHex: String = "#111827"
)

data class PdfResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val errorMessage: String? = null
)

data class PdfBusinessInfo(
    val businessName: String,
    val ownerName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val website: String?,
    val taxNumber: String?,
    val logoUri: String?,
    val paymentInstructions: String?
)

data class PdfClientInfo(
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?
)

data class PdfJobInfo(
    val title: String,
    val address: String?,
    val scheduledDate: String?,
    val description: String?
)

data class PdfLineItem(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

data class PdfSignatureInfo(
    val localUri: String,
    val signedBy: String?,
    val approvalText: String?,
    val createdDate: String
)

data class QuotePdfData(
    val quoteNumber: String,
    val issueDate: String,
    val validUntil: String?,
    val businessInfo: PdfBusinessInfo,
    val clientInfo: PdfClientInfo?,
    val jobInfo: PdfJobInfo?,
    val lineItems: List<PdfLineItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val notes: String?,
    val status: String,
    val signature: PdfSignatureInfo?,
    val currencyCode: String,
    val showFreeWatermark: Boolean
)

data class InvoicePdfData(
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String?,
    val paidDate: String?,
    val paymentMethod: String?,
    val paymentInstructions: String?,
    val businessInfo: PdfBusinessInfo,
    val clientInfo: PdfClientInfo?,
    val jobInfo: PdfJobInfo?,
    val quoteNumber: String?,
    val lineItems: List<PdfLineItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val notes: String?,
    val status: String,
    val signature: PdfSignatureInfo?,
    val currencyCode: String,
    val showFreeWatermark: Boolean
)

interface PdfService {
    val brandHeader: PdfBrandHeader
}

class ServiceSpherePdfService : PdfService {
    override val brandHeader: PdfBrandHeader = PdfBrandHeader()
}

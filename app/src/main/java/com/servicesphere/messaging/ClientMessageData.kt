package com.servicesphere.messaging

data class ClientMessageData(
    val businessName: String? = null,
    val ownerName: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val clientEmail: String? = null,
    val jobTitle: String? = null,
    val jobAddress: String? = null,
    val quoteNumber: String? = null,
    val invoiceNumber: String? = null,
    val total: String? = null,
    val dueDate: String? = null,
    val paymentInstructions: String? = null,
    val currencyCode: String? = null
)

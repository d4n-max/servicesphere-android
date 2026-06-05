package com.servicesphere.data.model

data class DashboardMetricDto(
    val label: String,
    val value: String
)

data class JobPreviewDto(
    val title: String,
    val clientName: String,
    val scheduledTime: String,
    val status: String
)

data class ClientPreviewDto(
    val name: String,
    val detail: String
)

data class InvoicePreviewDto(
    val number: String,
    val clientName: String,
    val amount: String,
    val status: String
)

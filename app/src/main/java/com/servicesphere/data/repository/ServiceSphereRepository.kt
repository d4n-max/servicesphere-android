package com.servicesphere.data.repository

import com.servicesphere.data.model.ClientPreviewDto
import com.servicesphere.data.model.DashboardMetricDto
import com.servicesphere.data.model.InvoicePreviewDto
import com.servicesphere.data.model.JobPreviewDto

interface ServiceSphereRepository {
    fun dashboardMetrics(): List<DashboardMetricDto>
    fun jobPreviews(): List<JobPreviewDto>
    fun clientPreviews(): List<ClientPreviewDto>
    fun invoicePreviews(): List<InvoicePreviewDto>
}

class FakeServiceSphereRepository : ServiceSphereRepository {
    override fun dashboardMetrics(): List<DashboardMetricDto> = listOf(
        DashboardMetricDto("Today's Jobs", "3"),
        DashboardMetricDto("Unpaid Invoices", "$2.4k"),
        DashboardMetricDto("Draft Quotes", "5"),
        DashboardMetricDto("This Month", "$18.6k")
    )

    override fun jobPreviews(): List<JobPreviewDto> = listOf(
        JobPreviewDto("Kitchen faucet repair", "Ada Johnson", "Today, 9:30 AM", "Scheduled")
    )

    override fun clientPreviews(): List<ClientPreviewDto> = listOf(
        ClientPreviewDto("Ada Johnson", "44 Cedar Lane - 2 open jobs")
    )

    override fun invoicePreviews(): List<InvoicePreviewDto> = listOf(
        InvoicePreviewDto("INV-1007", "Ben Carter", "$485.00", "Unpaid")
    )
}

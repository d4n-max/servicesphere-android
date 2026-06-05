package com.servicesphere.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.repository.BusinessRepository
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.QuoteStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class DashboardUiState(
    val isLoading: Boolean = true,
    val businessName: String? = null,
    val currencyCode: String = "USD",
    val totalClients: Int = 0,
    val totalJobs: Int = 0,
    val todayJobsCount: Int = 0,
    val unpaidInvoicesCount: Int = 0,
    val overdueInvoicesCount: Int = 0,
    val draftQuotesCount: Int = 0,
    val thisMonthRevenue: Double = 0.0,
    val recentJobs: List<DashboardJobPreview> = emptyList(),
    val recentInvoices: List<DashboardInvoicePreview> = emptyList(),
    val errorMessage: String? = null
) {
    val formattedRevenue: String
        get() = formatMoney(thisMonthRevenue, currencyCode)
}

data class DashboardJobPreview(
    val title: String,
    val clientName: String?,
    val status: String,
    val scheduledDate: String?,
    val estimatedPrice: String?
)

data class DashboardInvoicePreview(
    val invoiceNumber: String,
    val clientName: String?,
    val status: String,
    val dueDate: String?,
    val total: String
)

class DashboardViewModel(
    businessRepository: BusinessRepository,
    clientRepository: ClientRepository,
    jobRepository: JobRepository,
    quoteRepository: QuoteRepository,
    invoiceRepository: InvoiceRepository
) : ViewModel() {
    private val zoneId = ZoneId.systemDefault()

    val uiState: StateFlow<DashboardUiState> = combine(
        businessRepository.observeBusinessProfile(),
        clientRepository.observeClients(),
        jobRepository.observeJobs(),
        quoteRepository.observeQuotes(),
        invoiceRepository.observeInvoices()
    ) { businessProfile, clients, jobs, quotes, invoices ->
        val today = LocalDate.now(zoneId)
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextMonthStart = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val currencyCode = businessProfile?.currencyCode?.ifBlank { null } ?: "USD"
        val clientNames = clients.associate { it.id to it.name }

        DashboardUiState(
            isLoading = false,
            businessName = businessProfile?.businessName,
            currencyCode = currencyCode,
            totalClients = clients.size,
            totalJobs = jobs.size,
            todayJobsCount = jobs.count { it.scheduledAt?.let { value -> dateFromMillis(value) == today } == true },
            unpaidInvoicesCount = invoices.count { it.status in unpaidStatuses },
            overdueInvoicesCount = invoices.count { it.status != InvoiceStatus.PAID && it.dueDate != null && it.dueDate < now },
            draftQuotesCount = quotes.count { it.status == QuoteStatus.DRAFT },
            thisMonthRevenue = invoices
                .filter { it.status == InvoiceStatus.PAID && it.paidDate != null && it.paidDate >= monthStart && it.paidDate < nextMonthStart }
                .sumOf { it.total },
            recentJobs = jobs
                .sortedByDescending { it.scheduledAt ?: it.createdAt }
                .take(3)
                .map { it.toPreview(clientNames, currencyCode) },
            recentInvoices = invoices
                .sortedByDescending { it.issueDate }
                .take(3)
                .map { it.toPreview(clientNames, currencyCode) }
        )
    }
        .catch { error ->
            emit(DashboardUiState(isLoading = false, errorMessage = error.message ?: "Dashboard data could not be loaded."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    private fun JobEntity.toPreview(
        clientNames: Map<String, String>,
        currencyCode: String
    ): DashboardJobPreview = DashboardJobPreview(
        title = title,
        clientName = clientId?.let(clientNames::get),
        status = status.toDisplayStatus(),
        scheduledDate = scheduledAt?.let(::formatDateTime),
        estimatedPrice = estimatedPrice?.let { formatMoney(it, currencyCode) }
    )

    private fun InvoiceEntity.toPreview(
        clientNames: Map<String, String>,
        currencyCode: String
    ): DashboardInvoicePreview = DashboardInvoicePreview(
        invoiceNumber = invoiceNumber,
        clientName = clientId?.let(clientNames::get),
        status = status.toDisplayStatus(),
        dueDate = dueDate?.let(::formatDate),
        total = formatMoney(total, currencyCode)
    )

    private fun dateFromMillis(value: Long): LocalDate =
        Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate()

    private fun formatDateTime(value: Long): String =
        Instant.ofEpochMilli(value)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

    private fun formatDate(value: Long): String =
        Instant.ofEpochMilli(value)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("MMM d"))

    class Factory(
        private val businessRepository: BusinessRepository,
        private val clientRepository: ClientRepository,
        private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository,
        private val invoiceRepository: InvoiceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(
                    businessRepository = businessRepository,
                    clientRepository = clientRepository,
                    jobRepository = jobRepository,
                    quoteRepository = quoteRepository,
                    invoiceRepository = invoiceRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private val unpaidStatuses = setOf(InvoiceStatus.SENT, InvoiceStatus.UNPAID, InvoiceStatus.OVERDUE)

private fun String.toDisplayStatus(): String =
    lowercase()
        .split("_")
        .joinToString(" ") { part -> part.replaceFirstChar { char -> char.titlecase(Locale.US) } }

private fun formatMoney(value: Double, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance()
    formatter.currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance("USD"))
    return formatter.format(value)
}

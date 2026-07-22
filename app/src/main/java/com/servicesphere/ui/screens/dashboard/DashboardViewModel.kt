package com.servicesphere.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.servicesphere.data.repository.ClientRepository
import com.servicesphere.data.repository.InvoiceRepository
import com.servicesphere.data.repository.JobReminderRepository
import com.servicesphere.data.repository.JobRepository
import com.servicesphere.data.repository.QuoteRepository
import com.servicesphere.domain.today.TodayCockpitCalculator
import com.servicesphere.domain.today.TodayCockpitState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class DashboardUiState(val isLoading: Boolean = true, val cockpit: TodayCockpitState? = null, val errorMessage: String? = null)

class DashboardViewModel(
    clientRepository: ClientRepository,
    jobRepository: JobRepository,
    quoteRepository: QuoteRepository,
    invoiceRepository: InvoiceRepository,
    reminderRepository: JobReminderRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        jobRepository.observeJobs(), clientRepository.observeClients(), invoiceRepository.observeInvoices(),
        quoteRepository.observeQuotes(), reminderRepository.observeAllReminders()
    ) { jobs, clients, invoices, quotes, reminders ->
        DashboardUiState(false, TodayCockpitCalculator(zoneId).calculate(LocalDate.now(zoneId), clock(), jobs, clients, invoices, quotes, reminders))
    }.catch { emit(DashboardUiState(false, errorMessage = "Today could not be refreshed. Please try again.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    class Factory(
        private val clientRepository: ClientRepository, private val jobRepository: JobRepository,
        private val quoteRepository: QuoteRepository, private val invoiceRepository: InvoiceRepository,
        private val reminderRepository: JobReminderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(clientRepository, jobRepository, quoteRepository, invoiceRepository, reminderRepository) as T
    }
}

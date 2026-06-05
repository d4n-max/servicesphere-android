package com.servicesphere.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.InvoiceCard
import com.servicesphere.ui.components.JobCard
import com.servicesphere.ui.components.QuickActionButton
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereMetricCard
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.components.SummaryMetricCard
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun DashboardScreen(
    onNewJob: () -> Unit,
    onNewClient: () -> Unit,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onViewCalendar: () -> Unit,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            businessRepository = ServiceLocator.businessRepository,
            clientRepository = ServiceLocator.clientRepository,
            jobRepository = ServiceLocator.jobRepository,
            quoteRepository = ServiceLocator.quoteRepository,
            invoiceRepository = ServiceLocator.invoiceRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> DashboardLoading()
        else -> DashboardContent(
            uiState = uiState,
            onNewJob = onNewJob,
            onNewClient = onNewClient,
            onNewQuote = onNewQuote,
            onNewInvoice = onNewInvoice,
            onViewCalendar = onViewCalendar
        )
    }
}

@Composable
private fun DashboardLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(14.dp))
        Text("Loading your workspace", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onNewJob: () -> Unit,
    onNewClient: () -> Unit,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onViewCalendar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeader(businessName = uiState.businessName)
        }

        uiState.errorMessage?.let { message ->
            item {
                ServiceSphereCard {
                    Text(message, color = ServiceSphereDanger, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            QuickActions(
                onNewJob = onNewJob,
                onNewClient = onNewClient,
                onNewQuote = onNewQuote,
                onNewInvoice = onNewInvoice
            )
        }

        if (uiState.totalClients == 0 && uiState.totalJobs == 0 && uiState.recentInvoices.isEmpty()) {
            item {
                FirstWorkspaceActionCard(
                    onNewClient = onNewClient,
                    onNewJob = onNewJob,
                    onNewQuote = onNewQuote
                )
            }
        }

        item {
            ServiceSphereMetricCard(
                label = "Monthly Revenue",
                value = uiState.formattedRevenue,
                icon = Icons.Filled.Payments,
                badge = "This Month",
                tone = StatusTone.Primary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryMetricCard("Today's Jobs", uiState.todayJobsCount.toString(), Modifier.weight(1f))
                SummaryMetricCard("Unpaid", uiState.unpaidInvoicesCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryMetricCard("Draft Quotes", uiState.draftQuotesCount.toString(), Modifier.weight(1f))
                SummaryMetricCard("Overdue", uiState.overdueInvoicesCount.toString(), Modifier.weight(1f))
            }
        }

        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatusChip("Clients ${uiState.totalClients}", StatusTone.Info)
                        StatusChip("Jobs ${uiState.totalJobs}", StatusTone.Neutral)
                        StatusChip("Overdue ${uiState.overdueInvoicesCount}", if (uiState.overdueInvoicesCount > 0) StatusTone.Danger else StatusTone.Success)
                    }
                }
            }
        }

        item { SectionHeader("Today's Jobs", "View Calendar", onViewCalendar) }
        if (uiState.recentJobs.isEmpty()) {
            item {
                EmptyState(
                    title = "No jobs yet",
                    message = "New and scheduled jobs will appear here as soon as you add them.",
                    icon = Icons.Filled.HomeRepairService,
                    actionLabel = "New Job",
                    onAction = onNewJob
                )
            }
        } else {
            items(uiState.recentJobs) { job ->
                JobCard(
                    title = job.title,
                    client = job.clientName ?: "No client linked",
                    status = job.status,
                    schedule = job.scheduledDate,
                    price = job.estimatedPrice,
                    tone = toneForJob(job.status)
                )
            }
        }

        item { SectionHeader("Recent Invoices") }
        if (uiState.recentInvoices.isEmpty()) {
            item {
                EmptyState(
                    title = "No invoices yet",
                    message = "Invoices and payment status will show here once billing begins.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    actionLabel = "New Invoice",
                    onAction = onNewInvoice
                )
            }
        } else {
            items(uiState.recentInvoices) { invoice ->
                InvoiceCard(
                    number = invoice.invoiceNumber,
                    client = invoice.clientName ?: "No client linked",
                    status = invoice.status,
                    dueDate = invoice.dueDate,
                    total = invoice.total,
                    tone = toneForInvoice(invoice.status)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(businessName: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            businessName ?: "ServiceSphere workspace",
            style = MaterialTheme.typography.bodyLarge,
            color = ServiceSphereTextSecondary
        )
    }
}

@Composable
private fun FirstWorkspaceActionCard(
    onNewClient: () -> Unit,
    onNewJob: () -> Unit,
    onNewQuote: () -> Unit
) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set up your first workflow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Start with a client, then add jobs and quotes as your work comes in.",
                color = ServiceSphereTextSecondary
            )
            ServiceSphereOutlinedButton("Create your first client", modifier = Modifier.fillMaxWidth(), onClick = onNewClient)
            ServiceSphereOutlinedButton("Create your first job", modifier = Modifier.fillMaxWidth(), onClick = onNewJob)
            ServiceSphereOutlinedButton("Create your first quote", modifier = Modifier.fillMaxWidth(), onClick = onNewQuote)
        }
    }
}

@Composable
private fun QuickActions(
    onNewJob: () -> Unit,
    onNewClient: () -> Unit,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionButton("New Job", Icons.Filled.HomeRepairService, primary = true, onClick = onNewJob)
        QuickActionButton("Client", Icons.Filled.Groups, onClick = onNewClient)
        QuickActionButton("Quote", Icons.Filled.Description, onClick = onNewQuote)
        QuickActionButton("Invoice", Icons.AutoMirrored.Filled.ReceiptLong, onClick = onNewInvoice)
    }
}

private fun toneForJob(status: String): StatusTone = when (status) {
    "Completed", "Paid" -> StatusTone.Success
    "In Progress", "Scheduled" -> StatusTone.Info
    "Invoiced" -> StatusTone.Warning
    "Cancelled" -> StatusTone.Danger
    else -> StatusTone.Neutral
}

private fun toneForInvoice(status: String): StatusTone = when (status) {
    "Paid" -> StatusTone.Success
    "Overdue", "Cancelled" -> StatusTone.Danger
    "Unpaid", "Sent" -> StatusTone.Warning
    else -> StatusTone.Neutral
}

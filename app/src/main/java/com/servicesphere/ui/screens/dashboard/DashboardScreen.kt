package com.servicesphere.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.domain.today.TodayJobItem
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(onNewJob: () -> Unit, onNewClient: () -> Unit, onNewQuote: () -> Unit, onNewInvoice: () -> Unit, onViewCalendar: () -> Unit, onOpenJob: (String) -> Unit, onOpenInvoice: (String) -> Unit, onOpenQuote: (String) -> Unit, viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(ServiceLocator.clientRepository, ServiceLocator.jobRepository, ServiceLocator.quoteRepository, ServiceLocator.invoiceRepository, ServiceLocator.jobReminderRepository))) {
    val state by viewModel.uiState.collectAsState()
    if (state.isLoading) { Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }; return }
    val cockpit = state.cockpit ?: return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Column { Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date()), color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusChip("${cockpit.activeJobs.size} jobs", StatusTone.Info); StatusChip("${cockpit.overdueInvoices.size} overdue", if (cockpit.overdueInvoices.isEmpty()) StatusTone.Success else StatusTone.Danger); StatusChip("${cockpit.quoteFollowUps.size} follow-ups", StatusTone.Warning) } } }
        state.errorMessage?.let { message -> item { ServiceSphereCard { Text(message) } } }
        cockpit.nextJob?.let { job -> item { NextJobCard(job, onOpenJob) } }
        item { SectionHeader("Today's schedule", "Calendar", onViewCalendar) }
        if (cockpit.activeJobs.isEmpty()) item { EmptyState(title = "No jobs scheduled today.", message = "Create a job when new work comes in.", actionLabel = "Create job", onAction = onNewJob) }
        else items(cockpit.activeJobs) { job -> TodayJobCard(job, onOpenJob) }
        if (cockpit.overdueInvoices.isNotEmpty()) { item { SectionHeader("Money requiring attention") }; items(cockpit.overdueInvoices) { invoice -> ServiceSphereCard(onClick = { onOpenInvoice(invoice.id) }) { Text("${invoice.invoiceNumber} • ${invoice.clientName ?: "No client"}", fontWeight = FontWeight.SemiBold); Text("Overdue — ${invoice.total}") } } }
        if (cockpit.quoteFollowUps.isNotEmpty()) { item { SectionHeader("Quotes requiring follow-up") }; items(cockpit.quoteFollowUps) { quote -> ServiceSphereCard(onClick = { onOpenQuote(quote.id) }) { Text("${quote.quoteNumber} • ${quote.clientName ?: "No client"}", fontWeight = FontWeight.SemiBold); Text(quote.reason) } } }
        if (cockpit.dueReminders.isNotEmpty()) { item { SectionHeader("Due reminders") }; items(cockpit.dueReminders) { reminder -> ServiceSphereCard(onClick = { onOpenJob(reminder.jobId) }) { Text("Job reminder due", fontWeight = FontWeight.SemiBold) } } }
        if (cockpit.completedJobs.isNotEmpty()) { item { SectionHeader("Completed today") }; items(cockpit.completedJobs) { job -> TodayJobCard(job, onOpenJob) } }
        if (cockpit.isCaughtUp) item { ServiceSphereCard { Text("You're caught up for today.", fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable private fun NextJobCard(job: TodayJobItem, onOpenJob: (String) -> Unit) {
    val context = LocalContext.current
    ServiceSphereCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Next job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(job.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(listOfNotNull(job.clientName, job.address, job.scheduledAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }).joinToString(" • ")); if (!job.notes.isNullOrBlank()) Text(job.notes, maxLines = 2); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ServiceSphereButton(if (job.status == JobStatus.IN_PROGRESS) "Continue job" else "Start job", Modifier.weight(1f)) { onOpenJob(job.id) }; if (!job.address.isNullOrBlank()) ServiceSphereOutlinedButton("Directions", Modifier.weight(1f)) { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(job.address)}")); runCatching { context.startActivity(intent) }.onFailure { Toast.makeText(context, "No maps app available for directions.", Toast.LENGTH_SHORT).show() } } } } }
}
@Composable private fun TodayJobCard(job: TodayJobItem, onOpenJob: (String) -> Unit) = ServiceSphereCard(onClick = { onOpenJob(job.id) }) { Column { Text(job.title, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(job.clientName, job.scheduledAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }, job.status.replace('_', ' ')).joinToString(" • ")) } }

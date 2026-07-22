package com.servicesphere.ui.screens.clients

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.messaging.MessageTemplateType
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.QuickActionButton
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import com.servicesphere.ui.timeline.ActivityTimeline
import com.servicesphere.ui.timeline.TimelineTarget
import java.text.DateFormat
import java.util.Date

@Composable
fun ClientDetailScreen(
    clientId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onNewJob: () -> Unit,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onComposeMessage: (MessageTemplateType) -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    viewModel: ClientDetailViewModel = viewModel(
        factory = ClientDetailViewModel.Factory(clientId, ServiceLocator.clientRepository, ServiceLocator.jobRepository, ServiceLocator.quoteRepository, ServiceLocator.invoiceRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onDeleted()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.isLoading -> item { DetailLoading() }
            uiState.client == null -> item {
                EmptyState(
                    title = "Client not found",
                    message = "This local client record is missing or was deleted.",
                    icon = Icons.Filled.PersonOff,
                    actionLabel = "Back",
                    onAction = onBack
                )
            }
            else -> {
                val client = uiState.client!!
                item {
                    ScreenHeader(client.name, "Local client record")
                }
                uiState.errorMessage?.let { message ->
                    item {
                        ServiceSphereCard {
                            Text(message, color = ServiceSphereDanger)
                        }
                    }
                }
                item {
                    ClientDetailsCard(client)
                }
                item {
                    ClientQuickActions(
                        client = client,
                        onNewJob = onNewJob,
                        onNewQuote = onNewQuote,
                        onNewInvoice = onNewInvoice,
                        onComposeMessage = onComposeMessage
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton(label = "Back", modifier = Modifier.weight(1f), onClick = onBack)
                        ServiceSphereButton(label = "Edit Client", modifier = Modifier.weight(1f), onClick = { onEdit(client.id) })
                    }
                }
                item {
                    ServiceSphereOutlinedButton(label = "Delete Client", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = true })
                }
                item {
                    if (uiState.timeline.isEmpty()) {
                        ServiceSphereCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("No activity yet", fontWeight = FontWeight.Bold)
                                Text("Create a quote or job to start this client’s history.", color = ServiceSphereTextSecondary)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ServiceSphereOutlinedButton("Create quote", Modifier.weight(1f), onClick = onNewQuote)
                                    ServiceSphereButton("Create job", Modifier.weight(1f), onClick = onNewJob)
                                }
                            }
                        }
                    } else ActivityTimeline(uiState.timeline) { target, id ->
                        when (target) { TimelineTarget.QUOTE -> onOpenQuote(id); TimelineTarget.JOB -> onOpenJob(id); TimelineTarget.INVOICE -> onOpenInvoice(id) }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteClientDialog(
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteClient()
            }
        )
    }
}

@Composable
private fun DetailLoading() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading client", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun ClientDetailsCard(client: ClientUiModel) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailLine("Phone", client.phone)
            DetailLine("Email", client.email)
            DetailLine("Address", client.address)
            DetailLine("Notes", client.notes)
            DetailLine("Created", formatDate(client.createdAt))
            DetailLine("Updated", formatDate(client.updatedAt))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = ServiceSphereTextSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ClientQuickActions(
    client: ClientUiModel,
    onNewJob: () -> Unit,
    onNewQuote: () -> Unit,
    onNewInvoice: () -> Unit,
    onComposeMessage: (MessageTemplateType) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionButton("Call", Icons.Filled.Call, primary = !client.phone.isNullOrBlank()) {
                client.phone?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")), "No phone app found")
                }
            }
            QuickActionButton("Email", Icons.Filled.Email, primary = false) {
                client.email?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it")), "No email app found")
                }
            }
            QuickActionButton("Maps", Icons.Filled.LocationOn, primary = false) {
                client.address?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(it)}")), "No maps app found")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionButton("New Job", Icons.Filled.HomeRepairService, onClick = onNewJob)
            QuickActionButton("Quote", Icons.Filled.Description, onClick = onNewQuote)
            QuickActionButton("Invoice", Icons.AutoMirrored.Filled.ReceiptLong, onClick = onNewInvoice)
        }
        ServiceSphereCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Message Client", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton("Thank You", Modifier.weight(1f)) {
                        onComposeMessage(MessageTemplateType.THANK_YOU_REVIEW_REQUEST)
                    }
                    ServiceSphereOutlinedButton("Custom", Modifier.weight(1f)) {
                        onComposeMessage(MessageTemplateType.CUSTOM)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
}

private fun android.content.Context.tryStartActivity(intent: Intent, failureMessage: String) {
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show() }
}

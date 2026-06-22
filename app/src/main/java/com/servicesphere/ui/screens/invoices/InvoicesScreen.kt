package com.servicesphere.ui.screens.invoices

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.billing.FreePlanLimits
import com.servicesphere.data.ServiceLocator
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.domain.model.PaymentMethod
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereSearchBar
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun InvoicesScreen(
    onAddInvoice: () -> Unit,
    onInvoiceClick: (String) -> Unit,
    onEditInvoice: (String) -> Unit,
    viewModel: InvoicesViewModel = viewModel(
        factory = InvoicesViewModel.Factory(
            ServiceLocator.invoiceRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository,
            ServiceLocator.quoteRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var invoiceToDelete by remember { mutableStateOf<InvoiceUiModel?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<InvoiceUiModel?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Invoices", "Create, send, and track payment status.") }
        uiState.errorMessage?.let { message -> item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } } }
        item {
            ServiceSphereSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = "Search invoices"
            )
        }
        item { InvoiceFilterRow(uiState.selectedStatusFilter, viewModel::onStatusFilterChanged) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${uiState.invoices.size} invoices", fontWeight = FontWeight.Bold)
                    Text("${uiState.totalInvoices} of ${FreePlanLimits.maxInvoices} free invoices used", color = ServiceSphereTextSecondary)
                }
                ServiceSphereButton("Create Invoice", Modifier.weight(1f), onClick = onAddInvoice)
            }
        }
        when {
            uiState.isLoading -> item { LoadingInvoices() }
            uiState.invoices.isEmpty() && uiState.searchQuery.isBlank() && uiState.selectedStatusFilter == null -> item {
                EmptyState(
                    title = "No invoices yet",
                    message = "When a job is ready to bill, turn the job details into an invoice.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    actionLabel = "Create invoice",
                    onAction = onAddInvoice
                )
            }
            uiState.invoices.isEmpty() -> item {
                EmptyState(
                    title = "No matching invoices",
                    message = "Try adjusting your search or status filter.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong
                )
            }
            else -> items(uiState.invoices, key = { it.id }) { invoice ->
                InvoiceListCard(
                    invoice = invoice,
                    onClick = { onInvoiceClick(invoice.id) },
                    onEdit = { onEditInvoice(invoice.id) },
                    onDelete = { invoiceToDelete = invoice },
                    onMarkPaid = { invoiceToMarkPaid = invoice }
                )
            }
        }
    }

    invoiceToDelete?.let { invoice ->
        DeleteInvoiceDialog(
            onDismiss = { invoiceToDelete = null },
            onConfirm = {
                invoiceToDelete = null
                viewModel.deleteInvoice(invoice.id)
            }
        )
    }
    invoiceToMarkPaid?.let { invoice ->
        MarkPaidDialog(
            onDismiss = { invoiceToMarkPaid = null },
            onConfirm = {
                invoiceToMarkPaid = null
                viewModel.markInvoicePaid(invoice.id, it)
            }
        )
    }
}

@Composable
private fun InvoiceFilterRow(selected: String?, onSelected: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = { onSelected(null) }) {
            StatusChip("All", if (selected == null) StatusTone.Primary else StatusTone.Neutral)
        }
        invoiceStatuses.forEach { status ->
            TextButton(onClick = { onSelected(status) }) {
                StatusChip(status.toDisplayStatus(), if (selected == status) StatusTone.Primary else toneForInvoice(status))
            }
        }
    }
}

@Composable
private fun LoadingInvoices() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading invoices", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun InvoiceListCard(
    invoice: InvoiceUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkPaid: () -> Unit
) {
    ServiceSphereCard(accentColor = toneColorForInvoice(invoice.effectiveStatus()), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold)
                    Text(invoice.clientName ?: "No client linked", color = ServiceSphereTextSecondary)
                    invoice.jobTitle?.let { Text(it, color = ServiceSphereTextSecondary) }
                    invoice.quoteNumber?.let { Text("From $it", color = ServiceSphereTextSecondary) }
                }
                StatusChip(invoice.displayStatus, toneForInvoice(invoice.effectiveStatus()))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Issued", color = ServiceSphereTextSecondary)
                    Text(invoice.displayIssueDate, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Due", color = ServiceSphereTextSecondary)
                    Text(invoice.displayDueDate ?: "Not set", fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(invoice.displayTotal, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    if (invoice.status != InvoiceStatus.PAID) TextButton(onClick = onMarkPaid) { Text("Paid") }
                    TextButton(onClick = onDelete) { Text("Delete", color = ServiceSphereDanger) }
                }
            }
        }
    }
}

@Composable
fun DeleteInvoiceDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete invoice?") },
        text = { Text("This will remove the invoice and its line items from your local records.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = ServiceSphereDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MarkPaidDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark invoice as paid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                paymentMethods.forEach { method ->
                    TextButton(onClick = { selectedMethod = method }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(method.toDisplayStatus(), if (selectedMethod == method) StatusTone.Primary else StatusTone.Neutral)
                            if (selectedMethod == method) Text("Selected", color = ServiceSphereTextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedMethod) }) { Text("Mark Paid") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

val paymentMethods = listOf(PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD, PaymentMethod.OTHER)

fun toneForInvoice(status: String): StatusTone = when (status) {
    InvoiceStatus.PAID -> StatusTone.Success
    InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED -> StatusTone.Danger
    InvoiceStatus.UNPAID -> StatusTone.Warning
    InvoiceStatus.SENT -> StatusTone.Info
    InvoiceStatus.DRAFT -> StatusTone.Neutral
    else -> StatusTone.Neutral
}

fun toneColorForInvoice(status: String) = com.servicesphere.ui.components.toneColor(toneForInvoice(status))

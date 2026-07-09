package com.servicesphere.ui.screens.invoices

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun InvoiceFormScreen(
    invoiceId: String?,
    preselectedClientId: String?,
    preselectedJobId: String?,
    preselectedQuoteId: String?,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: InvoiceFormViewModel = viewModel(
        factory = InvoiceFormViewModel.Factory(
            ServiceLocator.invoiceRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.businessRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository,
            ServiceLocator.quoteRepository,
            ServiceLocator.activationTracker
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClientDialog by remember { mutableStateOf(false) }
    var showJobDialog by remember { mutableStateOf(false) }
    var showQuoteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(invoiceId, preselectedClientId, preselectedJobId, preselectedQuoteId) {
        if (invoiceId == null) viewModel.initializeForCreate(preselectedClientId, preselectedJobId, preselectedQuoteId) else viewModel.loadInvoice(invoiceId)
    }
    LaunchedEffect(uiState.saveSuccess, uiState.id) {
        val savedId = uiState.id
        if (uiState.saveSuccess && savedId != null) {
            viewModel.resetSaveSuccess()
            onSaved(savedId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { hideKeyboardAndClearFocus() })
            },
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader(if (invoiceId == null) "New Invoice" else "Edit Invoice", "Bill work with itemized totals, tax, and payment tracking.") }
        uiState.errorMessage?.let { message -> item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } } }
        if (uiState.invoiceNotFound) {
            item {
                EmptyState(
                    "Invoice not found",
                    "This local invoice record is missing or was deleted.",
                    actionLabel = "Back",
                    onAction = {
                        hideKeyboardAndClearFocus()
                        onCancel()
                    }
                )
            }
        } else if (uiState.isLoading) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading invoice", color = ServiceSphereTextSecondary)
                }
            }
        } else {
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton("Client: ${uiState.selectedClientName}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showClientDialog = true })
                        ServiceSphereOutlinedButton("Job: ${uiState.selectedJobTitle}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showJobDialog = true })
                        ServiceSphereOutlinedButton("Quote: ${uiState.selectedQuoteNumber}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showQuoteDialog = true })
                        ServiceSphereTextField(uiState.invoiceNumber, {}, "Invoice number")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereTextField(
                                uiState.issueDateText,
                                viewModel::onIssueDateChanged,
                                "Issue date",
                                Modifier.weight(1f),
                                supportingText = uiState.dateError,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            ServiceSphereTextField(
                                uiState.dueDateText,
                                viewModel::onDueDateChanged,
                                "Due date",
                                Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { hideKeyboardAndClearFocus() })
                            )
                        }
                        ServiceSphereOutlinedButton("Status: ${uiState.status.toDisplayStatus()}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showStatusDialog = true })
                    }
                }
            }
            item { SectionHeader("Line Items", "Add Item") { hideKeyboardAndClearFocus(); viewModel.addLineItem() } }
            items(uiState.lineItems, key = { it.id }) { item ->
                InvoiceLineItemEditor(item, viewModel, ::hideKeyboardAndClearFocus)
            }
            uiState.lineItemsError?.let { message -> item { Text(message, color = ServiceSphereDanger) } }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereTextField(
                                uiState.discountAmount,
                                viewModel::onDiscountAmountChanged,
                                "Discount",
                                Modifier.weight(1f),
                                isError = uiState.discountError != null,
                                supportingText = uiState.discountError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                            )
                            ServiceSphereTextField(
                                uiState.taxRatePercent,
                                viewModel::onTaxRatePercentChanged,
                                "Tax rate",
                                Modifier.weight(1f),
                                isError = uiState.taxRateError != null,
                                supportingText = uiState.taxRateError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { hideKeyboardAndClearFocus() })
                            )
                        }
                        ServiceSphereTextField(uiState.notes, viewModel::onNotesChanged, "Notes", minLines = 3, maxLines = 6)
                        InvoiceTotalsSummary(uiState)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton("Cancel", Modifier.weight(1f), onClick = { hideKeyboardAndClearFocus(); onCancel() })
                    ServiceSphereButton(if (uiState.isEditing) "Save Changes" else "Save Invoice", Modifier.weight(1f), enabled = !uiState.isSaving, onClick = { hideKeyboardAndClearFocus(); viewModel.saveInvoice() })
                }
            }
        }
    }

    if (showClientDialog) InvoiceClientSelectorDialog(uiState.availableClients, { showClientDialog = false }) { viewModel.onClientSelected(it); showClientDialog = false }
    if (showJobDialog) InvoiceJobSelectorDialog(uiState.availableJobs.filter { uiState.clientId == null || it.clientId == uiState.clientId }, { showJobDialog = false }) { viewModel.onJobSelected(it); showJobDialog = false }
    if (showQuoteDialog) InvoiceQuoteSelectorDialog(uiState.availableQuotes.filter { uiState.clientId == null || it.clientId == uiState.clientId }, { showQuoteDialog = false }) { viewModel.onQuoteSelected(it); showQuoteDialog = false }
    if (showStatusDialog) InvoiceStatusDialog(uiState.status, { showStatusDialog = false }) { viewModel.onStatusChanged(it); showStatusDialog = false }
}

@Composable
private fun InvoiceLineItemEditor(item: InvoiceLineItemFormUiModel, viewModel: InvoiceFormViewModel, onDone: () -> Unit) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceSphereTextField(
                item.description,
                { viewModel.onLineItemDescriptionChanged(item.id, it) },
                "Item description",
                isError = item.descriptionError != null,
                supportingText = item.descriptionError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceSphereTextField(item.quantity, { viewModel.onLineItemQuantityChanged(item.id, it) }, "Quantity", Modifier.weight(1f), isError = item.quantityError != null, supportingText = item.quantityError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
                ServiceSphereTextField(item.unitPrice, { viewModel.onLineItemUnitPriceChanged(item.id, it) }, "Unit price", Modifier.weight(1f), isError = item.unitPriceError != null, supportingText = item.unitPriceError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onDone() }))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total ${formatInvoiceMoney(item.total)}", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { viewModel.removeLineItem(item.id) }) { Text("Delete", color = ServiceSphereDanger) }
            }
        }
    }
}

@Composable
private fun InvoiceTotalsSummary(uiState: InvoiceFormUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InvoiceTotalLine("Subtotal", uiState.subtotal)
        InvoiceTotalLine("Discount", uiState.discountAmount.toDoubleOrNull() ?: 0.0)
        InvoiceTotalLine("Tax", uiState.taxAmount)
        InvoiceTotalLine("Total", uiState.total, bold = true)
    }
}

@Composable
fun InvoiceTotalLine(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ServiceSphereTextSecondary)
        Text(formatInvoiceMoney(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun InvoiceClientSelectorDialog(clients: List<InvoiceClientPickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select client") }, text = {
        Column {
            TextButton(onClick = { onSelect(null) }) { Text("No client selected") }
            clients.forEach { TextButton(onClick = { onSelect(it.id) }) { Text(it.name) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun InvoiceJobSelectorDialog(jobs: List<InvoiceJobPickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select job") }, text = {
        Column {
            TextButton(onClick = { onSelect(null) }) { Text("No job selected") }
            jobs.forEach { TextButton(onClick = { onSelect(it.id) }) { Text(it.title) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun InvoiceQuoteSelectorDialog(quotes: List<QuotePickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select quote") }, text = {
        Column {
            TextButton(onClick = { onSelect(null) }) { Text("No quote selected") }
            quotes.forEach { quote ->
                TextButton(onClick = { onSelect(quote.id) }) {
                    Text("${quote.quoteNumber} · ${formatInvoiceMoney(quote.total)}")
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun InvoiceStatusDialog(selectedStatus: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Change status") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            invoiceStatuses.forEach { status ->
                TextButton(onClick = { onSelect(status) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status.toDisplayStatus(), toneForInvoice(status))
                        if (status == selectedStatus) Text("Selected", color = ServiceSphereTextSecondary)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

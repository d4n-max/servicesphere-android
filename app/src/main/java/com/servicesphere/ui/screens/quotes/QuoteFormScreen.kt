package com.servicesphere.ui.screens.quotes

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
import androidx.compose.material3.MaterialTheme
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
fun QuoteFormScreen(
    quoteId: String?,
    preselectedClientId: String?,
    preselectedJobId: String?,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: QuoteFormViewModel = viewModel(
        factory = QuoteFormViewModel.Factory(
            ServiceLocator.quoteRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.businessRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClientDialog by remember { mutableStateOf(false) }
    var showJobDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(quoteId, preselectedClientId, preselectedJobId) {
        if (quoteId == null) viewModel.initializeForCreate(preselectedClientId, preselectedJobId) else viewModel.loadQuote(quoteId)
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
        item { ScreenHeader(if (quoteId == null) "New Quote" else "Edit Quote", "Build line items, discounts, tax, and totals.") }
        uiState.errorMessage?.let { message -> item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } } }
        if (uiState.quoteNotFound) {
            item {
                EmptyState(
                    "Quote not found",
                    "This local quote record is missing or was deleted.",
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
                    Text("Loading quote", color = ServiceSphereTextSecondary)
                }
            }
        } else {
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton("Client: ${uiState.selectedClientName}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showClientDialog = true })
                        ServiceSphereOutlinedButton("Job: ${uiState.selectedJobTitle}", Modifier.fillMaxWidth(), onClick = { hideKeyboardAndClearFocus(); showJobDialog = true })
                        ServiceSphereTextField(uiState.quoteNumber, {}, "Quote number")
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
                                uiState.validUntilText,
                                viewModel::onValidUntilChanged,
                                "Valid until",
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
                LineItemEditor(item, viewModel, ::hideKeyboardAndClearFocus)
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
                        TotalsSummary(uiState)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton("Cancel", Modifier.weight(1f), onClick = { hideKeyboardAndClearFocus(); onCancel() })
                    ServiceSphereButton(if (uiState.isEditing) "Save Changes" else "Save Quote", Modifier.weight(1f), enabled = !uiState.isSaving, onClick = { hideKeyboardAndClearFocus(); viewModel.saveQuote() })
                }
            }
        }
    }

    if (showClientDialog) ClientSelectorDialog(uiState.availableClients, { showClientDialog = false }) { viewModel.onClientSelected(it); showClientDialog = false }
    if (showJobDialog) JobSelectorDialog(uiState.availableJobs.filter { uiState.clientId == null || it.clientId == uiState.clientId }, { showJobDialog = false }) { viewModel.onJobSelected(it); showJobDialog = false }
    if (showStatusDialog) QuoteStatusDialog(uiState.status, { showStatusDialog = false }) { viewModel.onStatusChanged(it); showStatusDialog = false }
}

@Composable
private fun LineItemEditor(item: LineItemFormUiModel, viewModel: QuoteFormViewModel, onDone: () -> Unit) {
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
                Text("Total ${formatMoney(item.total)}", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { viewModel.removeLineItem(item.id) }) { Text("Delete", color = ServiceSphereDanger) }
            }
        }
    }
}

@Composable
private fun TotalsSummary(uiState: QuoteFormUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TotalLine("Subtotal", uiState.subtotal)
        TotalLine("Tax", uiState.taxAmount)
        TotalLine("Total", uiState.total, bold = true)
    }
}

@Composable
private fun TotalLine(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ServiceSphereTextSecondary)
        Text(formatMoney(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ClientSelectorDialog(clients: List<QuoteClientPickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select client") }, text = {
        Column {
            TextButton(onClick = { onSelect(null) }) { Text("No client selected") }
            clients.forEach { TextButton(onClick = { onSelect(it.id) }) { Text(it.name) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun JobSelectorDialog(jobs: List<JobPickerUiModel>, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select job") }, text = {
        Column {
            TextButton(onClick = { onSelect(null) }) { Text("No job selected") }
            jobs.forEach { TextButton(onClick = { onSelect(it.id) }) { Text(it.title) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun QuoteStatusDialog(selectedStatus: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Change status") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quoteStatuses.forEach { status ->
                TextButton(onClick = { onSelect(status) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status.toDisplayStatus(), toneForQuote(status))
                        if (status == selectedStatus) Text("Selected", color = ServiceSphereTextSecondary)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

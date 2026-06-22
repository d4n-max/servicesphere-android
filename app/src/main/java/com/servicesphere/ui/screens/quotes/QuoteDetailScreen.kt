package com.servicesphere.ui.screens.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.messaging.MessageTemplateType
import com.servicesphere.pdf.PdfShareManager
import com.servicesphere.pdf.ServiceSpherePdfGenerator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.screens.invoices.ConvertQuoteToInvoiceViewModel
import com.servicesphere.ui.screens.pdf.QuotePdfActionViewModel
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun QuoteDetailScreen(
    quoteId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onConvertedToInvoice: (String) -> Unit,
    onComposeMessage: (MessageTemplateType) -> Unit,
    onGateBlocked: (FeatureGateResult) -> Unit,
    onQuoteShared: () -> Unit,
    viewModel: QuoteDetailViewModel = viewModel(
        factory = QuoteDetailViewModel.Factory(
            quoteId,
            ServiceLocator.quoteRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository
        )
    ),
    conversionViewModel: ConvertQuoteToInvoiceViewModel = viewModel(
        factory = ConvertQuoteToInvoiceViewModel.Factory(
            quoteId,
            ServiceLocator.quoteRepository,
            ServiceLocator.invoiceRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.businessRepository
        )
    ),
    pdfViewModel: QuotePdfActionViewModel = viewModel(
        factory = QuotePdfActionViewModel.Factory(
            quoteId,
            LocalContext.current.applicationContext,
            ServiceSpherePdfGenerator(
                ServiceLocator.businessRepository,
                ServiceLocator.clientRepository,
                ServiceLocator.jobRepository,
                ServiceLocator.quoteRepository,
                ServiceLocator.invoiceRepository,
                ServiceLocator.lineItemRepository,
                ServiceLocator.signatureRepository
            ),
            PdfShareManager(LocalContext.current.applicationContext),
            ServiceLocator.featureGateManager
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversionState by conversionViewModel.uiState.collectAsState()
    val pdfState by pdfViewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onDeleted()
    }
    LaunchedEffect(conversionState.convertedInvoiceId) {
        conversionState.convertedInvoiceId?.let {
            conversionViewModel.clearConvertedInvoice()
            onConvertedToInvoice(it)
        }
    }
    LaunchedEffect(conversionState.errorMessage) {
        conversionState.errorMessage?.let {
            snackbar.showSnackbar(it)
            conversionViewModel.clearError()
        }
    }
    LaunchedEffect(pdfState.pdfSuccessMessage, pdfState.pdfErrorMessage) {
        val message = pdfState.pdfSuccessMessage ?: pdfState.pdfErrorMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            pdfViewModel.clearPdfMessage()
        }
    }
    LaunchedEffect(pdfState.quoteShareSuccessEventId) {
        if (pdfState.quoteShareSuccessEventId != null) onQuoteShared()
    }

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> item { LoadingQuoteDetail() }
                uiState.quote == null -> item { EmptyState("Quote not found", "This local quote record is missing or was deleted.", Icons.Filled.Description, "Back", onBack) }
                else -> {
                    val quote = uiState.quote!!
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScreenHeader(quote.quoteNumber, quote.clientName ?: "No client linked")
                            StatusChip(quote.displayStatus, toneForQuote(quote.status))
                        }
                    }
                    uiState.errorMessage?.let { message -> item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } } }
                    item { QuoteInfoCard(quote, uiState.lineItems) }
                    item { QuoteMessageActions(onComposeMessage) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Back", Modifier.weight(1f), onClick = onBack)
                            ServiceSphereButton("Edit Quote", Modifier.weight(1f), onClick = { onEdit(quote.id) })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Change Status", Modifier.weight(1f), onClick = { showStatusDialog = true })
                            ServiceSphereOutlinedButton("Delete Quote", Modifier.weight(1f), onClick = { showDeleteDialog = true })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton(
                                if (pdfState.isGeneratingPdf) "Generating..." else "Generate PDF",
                                Modifier.weight(1f),
                                onClick = {
                                    if (!pdfState.isGeneratingPdf) {
                                        scope.launch {
                                            val gate = ServiceLocator.featureGateManager.canExportPdf()
                                            if (gate.allowed) pdfViewModel.generateQuotePdf() else onGateBlocked(gate)
                                        }
                                    }
                                }
                            )
                            ServiceSphereOutlinedButton(
                                "Share PDF",
                                Modifier.weight(1f),
                                onClick = {
                                    if (!pdfState.isGeneratingPdf) {
                                        scope.launch {
                                            val gate = ServiceLocator.featureGateManager.canExportPdf()
                                            if (gate.allowed) pdfViewModel.shareQuotePdf() else onGateBlocked(gate)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Convert", Modifier.weight(1f), onClick = { showConvertDialog = true })
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteQuoteDialog({ showDeleteDialog = false }) {
            showDeleteDialog = false
            viewModel.deleteQuote()
        }
    }
    if (showStatusDialog) {
        QuoteStatusDialog(uiState.quote?.status.orEmpty(), { showStatusDialog = false }) {
            viewModel.updateStatus(it)
            showStatusDialog = false
        }
    }
    if (showConvertDialog) {
        ConvertQuoteDialog(
            onDismiss = { showConvertDialog = false },
            onConfirm = {
                showConvertDialog = false
                scope.launch {
                    val gate = ServiceLocator.featureGateManager.canCreateInvoice()
                    if (gate.allowed) conversionViewModel.convertQuoteToInvoice() else onGateBlocked(gate)
                }
            }
        )
    }
}

@Composable
private fun QuoteMessageActions(onComposeMessage: (MessageTemplateType) -> Unit) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Client Follow-up", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceSphereOutlinedButton("Follow Up", Modifier.weight(1f)) {
                    onComposeMessage(MessageTemplateType.QUOTE_FOLLOW_UP)
                }
                ServiceSphereOutlinedButton("Next Steps", Modifier.weight(1f)) {
                    onComposeMessage(MessageTemplateType.QUOTE_ACCEPTED_NEXT_STEPS)
                }
            }
        }
    }
}

@Composable
private fun LoadingQuoteDetail() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading quote", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun QuoteInfoCard(quote: QuoteUiModel, items: List<QuoteLineItemUiModel>) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailLine("Client", quote.clientName)
            DetailLine("Job", quote.jobTitle)
            DetailLine("Issue date", quote.displayIssueDate)
            DetailLine("Valid until", quote.displayValidUntil)
            items.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(it.description, fontWeight = FontWeight.SemiBold)
                        Text("${it.quantity} x ${formatMoney(it.unitPrice)}", color = ServiceSphereTextSecondary)
                    }
                    Text(formatMoney(it.total), fontWeight = FontWeight.SemiBold)
                }
            }
            TotalLine("Subtotal", quote.subtotal)
            TotalLine("Discount", quote.discountAmount)
            TotalLine("Tax", quote.taxAmount)
            TotalLine("Total", quote.total, bold = true)
            DetailLine("Notes", quote.notes)
            DetailLine("Created", formatSimpleDate(quote.createdAt))
            DetailLine("Updated", formatSimpleDate(quote.updatedAt))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = ServiceSphereTextSecondary)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TotalLine(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ServiceSphereTextSecondary)
        Text(formatMoney(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun formatSimpleDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

@Composable
private fun ConvertQuoteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Convert quote to invoice?") },
        text = { Text("This will create a new invoice using this quote's client, job, line items, totals, and notes.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onConfirm) { Text("Convert") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

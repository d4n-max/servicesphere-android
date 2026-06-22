package com.servicesphere.ui.screens.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.data.local.SignatureImageStorage
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.domain.model.InvoiceStatus
import com.servicesphere.messaging.MessageTemplateType
import com.servicesphere.pdf.PdfShareManager
import com.servicesphere.pdf.ServiceSpherePdfGenerator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.screens.signatures.DeleteSignatureDialog
import com.servicesphere.ui.screens.signatures.EditSignatureDialog
import com.servicesphere.ui.screens.signatures.SignaturePreviewDialog
import com.servicesphere.ui.screens.signatures.SignatureSection
import com.servicesphere.ui.screens.signatures.SignatureUiModel
import com.servicesphere.ui.screens.signatures.SignaturesViewModel
import com.servicesphere.ui.screens.pdf.InvoicePdfActionViewModel
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onComposeMessage: (MessageTemplateType) -> Unit,
    onCaptureSignature: () -> Unit,
    onGateBlocked: (FeatureGateResult) -> Unit,
    viewModel: InvoiceDetailViewModel = viewModel(
        factory = InvoiceDetailViewModel.Factory(
            invoiceId,
            ServiceLocator.invoiceRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository,
            ServiceLocator.quoteRepository
        )
    ),
    pdfViewModel: InvoicePdfActionViewModel = viewModel(
        factory = InvoicePdfActionViewModel.Factory(
            invoiceId,
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pdfState by pdfViewModel.uiState.collectAsState()
    val signaturesViewModel: SignaturesViewModel = viewModel(
        key = "invoice_signatures_$invoiceId",
        factory = SignaturesViewModel.Factory(
            ServiceLocator.signatureRepository.observeSignaturesForInvoice(invoiceId),
            ServiceLocator.signatureRepository,
            SignatureImageStorage(context.applicationContext)
        )
    )
    val signaturesUiState by signaturesViewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPaidDialog by remember { mutableStateOf(false) }
    var signatureToDelete by remember { mutableStateOf<SignatureUiModel?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onDeleted()
    }
    LaunchedEffect(signaturesUiState.errorMessage) {
        signaturesUiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            signaturesViewModel.clearError()
        }
    }
    LaunchedEffect(signaturesUiState.successMessage) {
        signaturesUiState.successMessage?.let {
            snackbar.showSnackbar(it)
            signaturesViewModel.clearSuccess()
        }
    }
    LaunchedEffect(pdfState.pdfSuccessMessage, pdfState.pdfErrorMessage) {
        val message = pdfState.pdfSuccessMessage ?: pdfState.pdfErrorMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            pdfViewModel.clearPdfMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> item { LoadingInvoiceDetail() }
                uiState.invoice == null -> item {
                    EmptyState(
                        "Invoice not found",
                        "This local invoice record is missing or was deleted.",
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        "Back",
                        onBack
                    )
                }
                else -> {
                    val invoice = uiState.invoice!!
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScreenHeader(invoice.invoiceNumber, invoice.clientName ?: "No client linked")
                            StatusChip(invoice.displayStatus, toneForInvoice(invoice.effectiveStatus()))
                        }
                    }
                    uiState.errorMessage?.let { message -> item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } } }
                    item { InvoiceInfoCard(invoice, uiState.lineItems) }
                    item { InvoiceMessageActions(invoice, onComposeMessage) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Back", Modifier.weight(1f), onClick = onBack)
                            ServiceSphereButton("Edit Invoice", Modifier.weight(1f), onClick = { onEdit(invoice.id) })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Change Status", Modifier.weight(1f), onClick = { showStatusDialog = true })
                            ServiceSphereOutlinedButton("Mark Paid", Modifier.weight(1f), onClick = { showPaidDialog = true })
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
                                            if (gate.allowed) pdfViewModel.generateInvoicePdf() else onGateBlocked(gate)
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
                                            if (gate.allowed) pdfViewModel.shareInvoicePdf() else onGateBlocked(gate)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Delete Invoice", Modifier.weight(1f), onClick = { showDeleteDialog = true })
                        }
                    }
                    item {
                        SignatureSection(
                            title = "Client Approval",
                            emptyTitle = "No client approval yet",
                            emptyMessage = "Capture a signature to confirm the invoice was reviewed.",
                            uiState = signaturesUiState,
                            onCaptureSignature = onCaptureSignature,
                            onPreview = signaturesViewModel::onSignatureSelected,
                            onEdit = signaturesViewModel::startEditSignature,
                            onDelete = { signatureToDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteInvoiceDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteInvoice()
            }
        )
    }
    if (showPaidDialog) {
        MarkPaidDialog(
            onDismiss = { showPaidDialog = false },
            onConfirm = {
                showPaidDialog = false
                viewModel.markPaid(it)
            }
        )
    }
    if (showStatusDialog) {
        InvoiceStatusDialog(uiState.invoice?.status.orEmpty(), { showStatusDialog = false }) {
            viewModel.updateStatus(it)
            showStatusDialog = false
        }
    }
    signaturesUiState.selectedSignature?.let { signature ->
        SignaturePreviewDialog(
            signature = signature,
            onDismiss = signaturesViewModel::closePreview,
            onEdit = { signaturesViewModel.startEditSignature(signature.id) },
            onDelete = { signatureToDelete = signature }
        )
    }
    if (signaturesUiState.editSignatureId != null) {
        EditSignatureDialog(
            signedBy = signaturesUiState.signedByDraft,
            approvalText = signaturesUiState.approvalTextDraft,
            onSignedByChange = signaturesViewModel::onSignedByChanged,
            onApprovalTextChange = signaturesViewModel::onApprovalTextChanged,
            onDismiss = signaturesViewModel::cancelEdit,
            onSave = signaturesViewModel::saveSignatureMetadata
        )
    }
    signatureToDelete?.let { signature ->
        DeleteSignatureDialog(
            onDismiss = { signatureToDelete = null },
            onConfirm = {
                signatureToDelete = null
                signaturesViewModel.deleteSignature(signature.id)
            }
        )
    }
}

@Composable
private fun InvoiceMessageActions(invoice: InvoiceUiModel, onComposeMessage: (MessageTemplateType) -> Unit) {
    val isPaid = invoice.effectiveStatus() == InvoiceStatus.PAID
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Payment Follow-up", fontWeight = FontWeight.Bold)
            if (isPaid) {
                Text("Invoice is paid. Reminders are usually not needed.", color = ServiceSphereTextSecondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceSphereOutlinedButton("Reminder", Modifier.weight(1f)) {
                    onComposeMessage(MessageTemplateType.INVOICE_PAYMENT_REMINDER)
                }
                ServiceSphereOutlinedButton(
                    "Overdue",
                    Modifier.weight(1f)
                ) {
                    onComposeMessage(MessageTemplateType.INVOICE_OVERDUE_REMINDER)
                }
            }
        }
    }
}

@Composable
private fun LoadingInvoiceDetail() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading invoice", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun InvoiceInfoCard(invoice: InvoiceUiModel, items: List<InvoiceLineItemUiModel>) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailLine("Client", invoice.clientName)
            DetailLine("Phone", invoice.clientPhone)
            DetailLine("Email", invoice.clientEmail)
            DetailLine("Job", invoice.jobTitle)
            DetailLine("Quote", invoice.quoteNumber)
            DetailLine("Issue date", invoice.displayIssueDate)
            DetailLine("Due date", invoice.displayDueDate)
            DetailLine("Paid date", invoice.displayPaidDate)
            DetailLine("Payment method", invoice.paymentMethod?.toDisplayStatus())
            items.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(it.description, fontWeight = FontWeight.SemiBold)
                        Text("${it.quantity} x ${formatInvoiceMoney(it.unitPrice)}", color = ServiceSphereTextSecondary)
                    }
                    Text(formatInvoiceMoney(it.total), fontWeight = FontWeight.SemiBold)
                }
            }
            InvoiceTotalLine("Subtotal", invoice.subtotal)
            InvoiceTotalLine("Discount", invoice.discountAmount)
            InvoiceTotalLine("Tax", invoice.taxAmount)
            InvoiceTotalLine("Total", invoice.total, bold = true)
            DetailLine("Notes", invoice.notes)
            DetailLine("Created", formatInvoiceDate(invoice.createdAt))
            DetailLine("Updated", formatInvoiceDate(invoice.updatedAt))
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

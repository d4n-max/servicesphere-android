package com.servicesphere.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.data.ServiceLocator
import com.servicesphere.data.local.BusinessLogoStorage
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

@Composable
fun BusinessProfileScreen(
    onBack: () -> Unit,
    onGateBlocked: (FeatureGateResult) -> Unit,
    viewModel: BusinessProfileViewModel = businessProfileViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberKeyboardDismissAction()
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onLogoSelected(uri)
    }
    ProfileMessages(state, snackbar, viewModel, "Business profile saved")

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.settingsFormKeyboardDismiss(dismissKeyboard),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Business Profile", "This information appears on quotes, invoices, and PDFs.") }
            state.errorMessage?.let { item { ServiceSphereCard { Text(it, color = ServiceSphereDanger) } } }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LogoPicker(
                            logoUri = state.logoUri,
                            onChoose = {
                                dismissKeyboard()
                                scope.launch {
                                    val gate = ServiceLocator.featureGateManager.canUseBusinessLogoOnPdf()
                                    if (gate.allowed) logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    else onGateBlocked(gate)
                                }
                            },
                            onRemove = {
                                dismissKeyboard()
                                viewModel.removeLogo()
                            }
                        )
                        ServiceSphereTextField(state.businessName, viewModel::onBusinessNameChanged, "Business name", isError = state.businessNameError != null, supportingText = state.businessNameError, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        ServiceSphereTextField(state.ownerName, viewModel::onOwnerNameChanged, "Owner name", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        ServiceSphereTextField(state.phone, viewModel::onPhoneChanged, "Phone", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next))
                        ServiceSphereTextField(state.email, viewModel::onEmailChanged, "Email", isError = state.emailError != null, supportingText = state.emailError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                        ServiceSphereTextField(state.address, viewModel::onAddressChanged, "Address", minLines = 2, maxLines = 4)
                        ServiceSphereTextField(state.website, viewModel::onWebsiteChanged, "Website", isError = state.websiteError != null, supportingText = state.websiteError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next))
                        ServiceSphereTextField(state.taxNumber, viewModel::onTaxNumberChanged, "Tax/VAT number", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                    }
                }
            }
            item { SaveBackRow(onBack, state.isSaving, viewModel::saveProfile, dismissKeyboard) }
        }
    }
}

@Composable
fun CurrencyTaxSettingsScreen(
    onBack: () -> Unit,
    viewModel: BusinessProfileViewModel = businessProfileViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val dismissKeyboard = rememberKeyboardDismissAction()
    ProfileMessages(state, snackbar, viewModel, "Currency and tax settings saved")

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.settingsFormKeyboardDismiss(dismissKeyboard),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Currency & Tax", "Set the defaults used for new quotes and invoices.") }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton("Currency: ${state.currencyCode}", Modifier.fillMaxWidth(), onClick = { dismissKeyboard(); showCurrencyDialog = true })
                        ServiceSphereTextField(
                            value = state.currencyCode,
                            onValueChange = viewModel::onCurrencyCodeChanged,
                            label = "Currency code",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        ServiceSphereTextField(
                            value = state.taxRatePercent,
                            onValueChange = viewModel::onTaxRatePercentChanged,
                            label = "Default tax rate percent",
                            isError = state.taxRateError != null,
                            supportingText = state.taxRateError ?: "Used as the default tax rate for new quotes and invoices.",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                        )
                    }
                }
            }
            item { SaveBackRow(onBack, state.isSaving, viewModel::saveProfile, dismissKeyboard) }
        }
    }

    if (showCurrencyDialog) {
        CurrencyDialog(
            onDismiss = { showCurrencyDialog = false },
            onSelect = {
                viewModel.onCurrencyCodeChanged(it)
                showCurrencyDialog = false
            }
        )
    }
}

@Composable
fun DocumentSettingsScreen(
    onBack: () -> Unit,
    viewModel: BusinessProfileViewModel = businessProfileViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val dismissKeyboard = rememberKeyboardDismissAction()
    ProfileMessages(state, snackbar, viewModel, "Document settings saved")

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.settingsFormKeyboardDismiss(dismissKeyboard),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Document Settings", "Control quote and invoice numbering plus payment details.") }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Quote Settings")
                        ServiceSphereTextField(state.quotePrefix, viewModel::onQuotePrefixChanged, "Quote prefix", supportingText = state.prefixError, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        ServiceSphereTextField(
                            state.nextQuoteNumber,
                            viewModel::onNextQuoteNumberChanged,
                            "Next quote number",
                            isError = state.nextQuoteNumberError != null,
                            supportingText = state.nextQuoteNumberError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        Text("Preview: ${state.quotePreview}", color = ServiceSphereTextSecondary)
                    }
                }
            }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Invoice Settings")
                        ServiceSphereTextField(state.invoicePrefix, viewModel::onInvoicePrefixChanged, "Invoice prefix", supportingText = state.prefixError, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        ServiceSphereTextField(
                            state.nextInvoiceNumber,
                            viewModel::onNextInvoiceNumberChanged,
                            "Next invoice number",
                            isError = state.nextInvoiceNumberError != null,
                            supportingText = state.nextInvoiceNumberError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                        Text("Preview: ${state.invoicePreview}", color = ServiceSphereTextSecondary)
                    }
                }
            }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Payment Instructions")
                        ServiceSphereTextField(
                            value = state.paymentInstructions,
                            onValueChange = viewModel::onPaymentInstructionsChanged,
                            label = "Bank transfer details, payment deadline, cash/card notes...",
                            minLines = 4,
                            maxLines = 8
                        )
                    }
                }
            }
            item { SaveBackRow(onBack, state.isSaving, viewModel::saveProfile, dismissKeyboard) }
        }
    }
}

@Composable
private fun businessProfileViewModel(): BusinessProfileViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(
        factory = BusinessProfileViewModel.Factory(
            ServiceLocator.businessRepository,
            BusinessLogoStorage(context)
        )
    )
}

@Composable
private fun ProfileMessages(state: BusinessProfileUiState, snackbar: SnackbarHostState, viewModel: BusinessProfileViewModel, successMessage: String) {
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbar.showSnackbar(successMessage)
            viewModel.resetSaveSuccess()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
}

@Composable
private fun LogoPicker(logoUri: String?, onChoose: () -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ServiceSphereSecondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (logoUri == null) {
                Text("Business logo", color = ServiceSphereTextSecondary)
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(logoUri).crossfade(true).build(),
                    contentDescription = "Business logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceSphereOutlinedButton("Choose Logo", Modifier.weight(1f), onClick = onChoose)
            if (logoUri != null) ServiceSphereOutlinedButton("Remove Logo", Modifier.weight(1f), onClick = onRemove)
        }
    }
}

@Composable
private fun SaveBackRow(onBack: () -> Unit, isSaving: Boolean, onSave: () -> Unit, dismissKeyboard: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ServiceSphereOutlinedButton("Back", Modifier.weight(1f), onClick = { dismissKeyboard(); onBack() })
        ServiceSphereButton(if (isSaving) "Saving..." else "Save", Modifier.weight(1f), enabled = !isSaving, onClick = { dismissKeyboard(); onSave() })
    }
}

@Composable
private fun rememberKeyboardDismissAction(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}

private fun Modifier.settingsFormKeyboardDismiss(onDismiss: () -> Unit): Modifier =
    fillMaxSize()
        .imePadding()
        .navigationBarsPadding()
        .pointerInput(Unit) {
            detectTapGestures(onTap = { onDismiss() })
        }

@Composable
private fun CurrencyDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "RON", "PLN", "HUF", "CZK")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select currency") },
        text = {
            Column {
                currencies.forEach { currency ->
                    TextButton(onClick = { onSelect(currency) }) { Text(currency) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

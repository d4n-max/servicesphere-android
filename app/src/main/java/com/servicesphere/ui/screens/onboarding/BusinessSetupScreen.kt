package com.servicesphere.ui.screens.onboarding

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.components.ServiceSphereTextButton
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessSetupScreen(
    onFinished: () -> Unit,
    viewModel: BusinessSetupViewModel = viewModel(
        factory = BusinessSetupViewModel.Factory(
            businessRepository = ServiceLocator.businessRepository,
            preferences = ServiceLocator.preferences,
            seedDemoData = { ServiceLocator.seedDemoDataIfNeeded() }
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.setupComplete) {
        if (state.setupComplete) onFinished()
    }

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { hideKeyboardAndClearFocus() })
                },
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Set up ServiceSphere",
                    subtitle = "A few details make your jobs, quotes, invoices, and PDFs feel ready from day one."
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Step ${state.currentStep} of ${state.totalSteps}", color = ServiceSphereTextSecondary)
                    LinearProgressIndicator(
                        progress = { state.currentStep / state.totalSteps.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = ServiceSpherePrimary
                    )
                }
            }
            item {
                when (state.currentStep) {
                    1 -> BusinessBasicsStep(state, viewModel)
                    2 -> ContactDetailsStep(state, viewModel)
                    3 -> DocumentsMoneyStep(state, viewModel)
                    else -> StartOptionStep(state, viewModel)
                }
            }
            item {
                NavigationButtons(
                    state = state,
                    onBack = {
                        hideKeyboardAndClearFocus()
                        viewModel.previousStep()
                    },
                    onContinue = {
                        hideKeyboardAndClearFocus()
                        viewModel.nextStep()
                    },
                    onFinish = {
                        hideKeyboardAndClearFocus()
                        viewModel.finishSetup()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessBasicsStep(state: BusinessSetupUiState, viewModel: BusinessSetupViewModel) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Business basics")
            ServiceSphereTextField(
                value = state.businessName,
                onValueChange = viewModel::onBusinessNameChanged,
                label = "Business name",
                isError = state.businessNameError != null,
                supportingText = state.businessNameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(state.ownerName, viewModel::onOwnerNameChanged, "Owner name", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            Text("Trade type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tradeTypeOptions.forEach { option ->
                    FilterChip(
                        selected = state.tradeType == option,
                        onClick = { viewModel.onTradeTypeSelected(option) },
                        label = { Text(option) }
                    )
                }
            }
            state.tradeTypeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ContactDetailsStep(state: BusinessSetupUiState, viewModel: BusinessSetupViewModel) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Contact details")
            ServiceSphereTextField(
                state.phone,
                viewModel::onPhoneChanged,
                "Phone",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                state.email,
                viewModel::onEmailChanged,
                "Email",
                isError = state.emailError != null,
                supportingText = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                state.address,
                viewModel::onAddressChanged,
                "Service address",
                minLines = 2,
                maxLines = 4
            )
            ServiceSphereTextField(
                state.website,
                viewModel::onWebsiteChanged,
                "Website",
                isError = state.websiteError != null,
                supportingText = state.websiteError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentsMoneyStep(state: BusinessSetupUiState, viewModel: BusinessSetupViewModel) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Documents & money")
            Text("Currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currencyQuickOptions.forEach { option ->
                    FilterChip(
                        selected = state.currencyCode == option,
                        onClick = { viewModel.onCurrencyCodeChanged(option) },
                        label = { Text(option) }
                    )
                }
            }
            ServiceSphereTextField(state.currencyCode, viewModel::onCurrencyCodeChanged, "Currency code", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            ServiceSphereTextField(
                state.taxRatePercent,
                viewModel::onTaxRatePercentChanged,
                "Tax/VAT rate",
                isError = state.taxRateError != null,
                supportingText = state.taxRateError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                state.quotePrefix,
                viewModel::onQuotePrefixChanged,
                "Quote prefix",
                isError = state.prefixError != null,
                supportingText = state.prefixError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                state.invoicePrefix,
                viewModel::onInvoicePrefixChanged,
                "Invoice prefix",
                isError = state.prefixError != null,
                supportingText = state.prefixError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            ServiceSphereTextField(
                state.paymentInstructions,
                viewModel::onPaymentInstructionsChanged,
                "Payment instructions",
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
private fun StartOptionStep(
    state: BusinessSetupUiState,
    viewModel: BusinessSetupViewModel
) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Choose your starting point")
            SetupChoice(
                title = "Start empty",
                body = "Open a clean workspace and add real clients when you are ready.",
                selected = !state.useDemoData,
                onClick = { viewModel.onUseDemoDataChanged(false) }
            )
            SetupChoice(
                title = "Start with demo data",
                body = "Add sample clients, jobs, a quote, and an invoice for development and walkthroughs.",
                selected = state.useDemoData,
                onClick = { viewModel.onUseDemoDataChanged(true) }
            )
            Text("After setup, use the Dashboard quick actions to create your first client, job, quote, or invoice.")
        }
    }
}

@Composable
private fun SetupChoice(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    ServiceSphereCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = ServiceSphereTextSecondary)
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    state: BusinessSetupUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.currentStep == state.totalSteps) {
            ServiceSphereButton(
                label = if (state.isSaving) "Finishing..." else "Go to Dashboard",
                enabled = !state.isSaving,
                onClick = onFinish
            )
        } else {
            ServiceSphereButton("Continue", onClick = onContinue)
        }
        if (state.currentStep > 1) {
            ServiceSphereOutlinedButton("Back", modifier = Modifier.fillMaxWidth(), onClick = onBack)
        }
    }
}

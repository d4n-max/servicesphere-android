package com.servicesphere.ui.screens.onboarding

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun FirstJobScreen(
    startWithSample: Boolean,
    onJobCreated: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: FirstJobViewModel = viewModel(
        factory = FirstJobViewModel.Factory(
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository,
            ServiceLocator.quoteRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.preferences,
            ServiceLocator.activationTracker
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(startWithSample) {
        if (startWithSample) viewModel.createSampleJob()
    }
    LaunchedEffect(state.createdJobId, state.createdSampleJobId) {
        state.createdJobId?.let {
            viewModel.resetCreatedJob()
            onJobCreated(it, false)
        }
        state.createdSampleJobId?.let {
            viewModel.resetCreatedJob()
            onJobCreated(it, true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { hideKeyboard() }) },
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (startWithSample) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Preparing sample job", color = ServiceSphereTextSecondary)
                }
            }
        } else {
            item {
                ScreenHeader(
                    title = "What job do you want to organize first?",
                    subtitle = "You only need a client and job title to begin."
                )
            }
            state.errorMessage?.let { item { ServiceSphereCard { Text(it, color = ServiceSphereDanger) } } }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereTextField(
                            value = state.clientName,
                            onValueChange = viewModel::onClientNameChanged,
                            label = "Client name",
                            isError = state.clientNameError != null,
                            supportingText = state.clientNameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        ServiceSphereTextField(
                            value = state.jobTitle,
                            onValueChange = viewModel::onJobTitleChanged,
                            label = "Job title",
                            isError = state.jobTitleError != null,
                            supportingText = state.jobTitleError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Text("Optional. Useful for routes, quotes, and invoices later.", color = ServiceSphereTextSecondary)
                        ServiceSphereTextField(state.address, viewModel::onAddressChanged, "Address", minLines = 2, maxLines = 3)
                        ServiceSphereTextField(
                            value = state.dateText,
                            onValueChange = viewModel::onDateChanged,
                            label = "Date",
                            isError = state.dateError != null,
                            supportingText = state.dateError ?: "Defaults to today.",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        ServiceSphereTextField(
                            value = state.estimatedPrice,
                            onValueChange = viewModel::onEstimatedPriceChanged,
                            label = "Estimated price",
                            isError = state.priceError != null,
                            supportingText = state.priceError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { hideKeyboard() })
                        )
                        ServiceSphereTextField(state.notes, viewModel::onNotesChanged, "Notes", minLines = 3, maxLines = 5)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceSphereButton(
                        label = if (state.isSaving) "Creating..." else "Save first job",
                        enabled = !state.isSaving,
                        onClick = {
                            hideKeyboard()
                            viewModel.saveFirstJob()
                        }
                    )
                    ServiceSphereOutlinedButton("Back", onClick = {
                        hideKeyboard()
                        onBack()
                    })
                }
            }
        }
    }
}

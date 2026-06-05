package com.servicesphere.ui.screens.clients

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
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun ClientFormScreen(
    clientId: String?,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: ClientFormViewModel = viewModel(
        factory = ClientFormViewModel.Factory(ServiceLocator.clientRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(clientId) {
        if (clientId != null) viewModel.loadClient(clientId)
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
        item {
            ScreenHeader(
                title = if (clientId == null) "Add Client" else "Edit Client",
                subtitle = "Store customer details for faster jobs, quotes, and invoices."
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                ServiceSphereCard {
                    Text(message, color = ServiceSphereDanger)
                }
            }
        }

        if (uiState.clientNotFound) {
            item {
                EmptyState(
                    title = "Client not found",
                    message = "This local client record is missing or was deleted.",
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
                    Text("Loading client", color = ServiceSphereTextSecondary)
                }
            }
        } else {
            item {
                ClientFormFields(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDone = ::hideKeyboardAndClearFocus
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton(
                        label = "Cancel",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hideKeyboardAndClearFocus()
                            onCancel()
                        }
                    )
                    ServiceSphereButton(
                        label = if (uiState.isEditing) "Save Changes" else "Save Client",
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                        onClick = {
                            hideKeyboardAndClearFocus()
                            viewModel.saveClient()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientFormFields(
    uiState: ClientFormUiState,
    viewModel: ClientFormViewModel,
    onDone: () -> Unit
) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceSphereTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = "Client name",
                isError = uiState.nameError != null,
                supportingText = uiState.nameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = "Phone number",
                isError = uiState.phoneError != null,
                supportingText = uiState.phoneError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )
            ServiceSphereTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email address",
                isError = uiState.emailError != null,
                supportingText = uiState.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() })
            )
            ServiceSphereTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChanged,
                label = "Service address",
                minLines = 2,
                maxLines = 4
            )
            ServiceSphereTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = "Notes",
                minLines = 3,
                maxLines = 6
            )
        }
    }
}

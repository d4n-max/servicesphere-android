package com.servicesphere.ui.screens.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.messaging.MessageShareManager
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun MessageComposerScreen(
    type: String?,
    clientId: String?,
    jobId: String?,
    quoteId: String?,
    invoiceId: String?,
    onBack: () -> Unit,
    viewModel: MessageComposerViewModel = viewModel(
        factory = MessageComposerViewModel.Factory(
            ServiceLocator.businessRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository,
            ServiceLocator.quoteRepository,
            ServiceLocator.invoiceRepository,
            MessageShareManager(LocalContext.current.applicationContext)
        )
    )
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    fun hideKeyboardAndClearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(type, clientId, jobId, quoteId, invoiceId) {
        viewModel.initialize(type, clientId, jobId, quoteId, invoiceId)
    }
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.clearMessages()
        }
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
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Preparing message", color = ServiceSphereTextSecondary)
                    }
                }
            } else {
                item {
                    ScreenHeader(uiState.type.title, "Review and edit before sending.")
                }
                item { RecipientCard(uiState) }
                item {
                    ServiceSphereTextField(
                        value = uiState.message,
                        onValueChange = viewModel::onMessageChanged,
                        label = "Message",
                        minLines = 8,
                        maxLines = 12,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }
                uiState.errorMessage?.let { message ->
                    item { Text(message, color = ServiceSphereDanger) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereButton("Share", Modifier.weight(1f)) {
                            hideKeyboardAndClearFocus()
                            viewModel.shareMessage()
                        }
                        ServiceSphereOutlinedButton("Copy", Modifier.weight(1f)) {
                            hideKeyboardAndClearFocus()
                            viewModel.copyToClipboard()
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ServiceSphereOutlinedButton("SMS", Modifier.weight(1f)) {
                            hideKeyboardAndClearFocus()
                            viewModel.sendSms()
                        }
                        ServiceSphereOutlinedButton("Email", Modifier.weight(1f)) {
                            hideKeyboardAndClearFocus()
                            viewModel.sendEmail()
                        }
                    }
                }
                item {
                    ServiceSphereOutlinedButton("Cancel", Modifier.fillMaxWidth()) {
                        hideKeyboardAndClearFocus()
                        onBack()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientCard(uiState: MessageComposerUiState) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                Text("Recipient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(uiState.clientName ?: "No linked client", fontWeight = FontWeight.SemiBold)
            Text(uiState.clientPhone ?: "No phone number", color = ServiceSphereTextSecondary)
            Text(uiState.clientEmail ?: "No email address", color = ServiceSphereTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = ServiceSphereTextSecondary)
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = ServiceSphereTextSecondary)
                Icon(Icons.Filled.Email, contentDescription = null, tint = ServiceSphereTextSecondary)
            }
        }
    }
}

package com.servicesphere.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun DataExportScreen(
    onBack: () -> Unit,
    onDeleteComplete: (resetSetup: Boolean) -> Unit,
    viewModel: DataExportViewModel = viewModel(factory = DataExportViewModel.Factory(ServiceLocator.dataExportManager))
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.deleteComplete) {
        if (state.deleteComplete) {
            val resetSetup = state.resetSetupAfterDelete
            viewModel.markDeleteHandled()
            onDeleteComplete(resetSetup)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Data & Privacy", "Export local records or permanently clear this device.") }

        state.successMessage?.let { message ->
            item { ServiceSphereCard { Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) } }
        }
        state.errorMessage?.let { message ->
            item { ServiceSphereCard { Text(message, color = ServiceSphereDanger, fontWeight = FontWeight.SemiBold) } }
        }

        item { SectionHeader("Export Data") }
        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Exports are saved locally and can be shared using apps on your device.", color = ServiceSphereTextSecondary)
                    ExportButton("Export Full Backup", state.isExporting, viewModel::exportBackupJson)
                    ExportButton("Export Clients CSV", state.isExporting, viewModel::exportClientsCsv)
                    ExportButton("Export Jobs CSV", state.isExporting, viewModel::exportJobsCsv)
                    ExportButton("Export Quotes CSV", state.isExporting, viewModel::exportQuotesCsv)
                    ExportButton("Export Invoices CSV", state.isExporting, viewModel::exportInvoicesCsv)
                    state.lastExportFileName?.let { fileName ->
                        Text("Last export: $fileName", color = ServiceSphereTextSecondary)
                        ServiceSphereOutlinedButton("Share Last Export", onClick = viewModel::shareLastExport)
                    }
                }
            }
        }

        item { SectionHeader("Local Files") }
        item {
            ServiceSphereCard {
                Text(
                    "Exports are created locally on this device. ServiceSphere does not upload your data.",
                    color = ServiceSphereTextSecondary
                )
            }
        }

        item { SectionHeader("Delete Local Data") }
        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danger zone", color = ServiceSphereDanger, fontWeight = FontWeight.Bold)
                    Text(
                        "This removes clients, jobs, quotes, invoices, photos, signatures, reminders, and generated local records from this device.",
                        color = ServiceSphereTextSecondary
                    )
                    androidx.compose.material3.Button(
                        onClick = viewModel::requestDeleteAllData,
                        enabled = !state.isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ServiceSphereDanger)
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Delete All Local Data", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item { ServiceSphereOutlinedButton("Back", onClick = onBack) }
    }

    if (state.showDeleteConfirmation) {
        DeleteDataDialog(
            state = state,
            onConfirmationChanged = viewModel::onDeleteConfirmationTextChanged,
            onResetSetupChanged = viewModel::onResetSetupChanged,
            onDismiss = viewModel::cancelDeleteAllData,
            onConfirm = viewModel::confirmDeleteAllData
        )
    }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        if (state.successMessage != null || state.errorMessage != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun ExportButton(label: String, isExporting: Boolean, onClick: () -> Unit) {
    ServiceSphereButton(
        label = if (isExporting) "Exporting..." else label,
        enabled = !isExporting,
        onClick = onClick
    )
}

@Composable
private fun DeleteDataDialog(
    state: DataExportUiState,
    onConfirmationChanged: (String) -> Unit,
    onResetSetupChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete all local data?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This will permanently remove your ServiceSphere records from this device. This cannot be undone.")
                ServiceSphereTextField(
                    value = state.deleteConfirmationText,
                    onValueChange = onConfirmationChanged,
                    label = "Type DELETE to confirm"
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = state.resetSetupAfterDelete, onCheckedChange = onResetSetupChanged)
                    Text("Also reset onboarding and business setup")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.isDeleting && state.deleteConfirmationText.isDeleteConfirmation(),
                onClick = onConfirm
            ) {
                Text(if (state.isDeleting) "Deleting..." else "Delete", color = ServiceSphereDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) { Text("Cancel") }
        }
    )
}

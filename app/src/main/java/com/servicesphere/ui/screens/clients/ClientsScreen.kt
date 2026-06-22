package com.servicesphere.ui.screens.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.billing.FreePlanLimits
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereSearchBar
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun ClientsScreen(
    onAddClient: () -> Unit,
    onClientClick: (String) -> Unit,
    viewModel: ClientsViewModel = viewModel(
        factory = ClientsViewModel.Factory(ServiceLocator.clientRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<ClientUiModel?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Clients", "Keep customers, addresses, and job context close.") }
            item {
                ServiceSphereSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = "Search clients"
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${uiState.clients.size} clients", style = MaterialTheme.typography.labelLarge, color = ServiceSphereTextSecondary)
                        Text("${uiState.totalClients} of ${FreePlanLimits.maxClients} free clients used", style = MaterialTheme.typography.bodySmall, color = ServiceSphereTextSecondary)
                    }
                    StatusChip(if (uiState.searchQuery.isBlank()) "All clients" else "Search", StatusTone.Primary)
                }
            }
            uiState.errorMessage?.let { message ->
                item {
                    ServiceSphereCard {
                        Text(message, color = ServiceSphereDanger)
                    }
                }
            }
            when {
                uiState.isLoading -> item { LoadingClients() }
                uiState.clients.isEmpty() && uiState.searchQuery.isBlank() -> item {
                    EmptyState(
                        title = "No clients yet",
                        message = "Add a client once, then attach jobs, quotes, notes, and invoices to them.",
                        icon = Icons.Filled.Groups,
                        actionLabel = "Add client",
                        onAction = onAddClient
                    )
                }
                uiState.clients.isEmpty() -> item {
                    EmptyState(
                        title = "No matching clients",
                        message = "Try searching by name, phone, email, or address.",
                        icon = Icons.Filled.Groups
                    )
                }
                else -> items(uiState.clients, key = { it.id }) { client ->
                    ClientListCard(
                        client = client,
                        onClick = { onClientClick(client.id) },
                        onDelete = { pendingDelete = client }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClient,
            containerColor = ServiceSpherePrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Client")
        }
    }

    pendingDelete?.let { client ->
        DeleteClientDialog(
            onDismiss = { pendingDelete = null },
            onDelete = {
                viewModel.deleteClient(client.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun LoadingClients() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading clients", modifier = Modifier.padding(top = 12.dp), color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun ClientListCard(
    client: ClientUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ServiceSphereCard(accentColor = ServiceSpherePrimary) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(client.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OptionalLine(client.phone)
                    OptionalLine(client.email)
                    OptionalLine(client.address)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = ServiceSphereDanger)
                }
            }
            StatusChip("Local record", StatusTone.Info)
        }
    }
}

@Composable
private fun OptionalLine(value: String?) {
    if (!value.isNullOrBlank()) {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ServiceSphereTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DeleteClientDialog(
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete client?") },
        text = { Text("This will remove the client from your local records. Existing linked jobs and invoices will remain but may no longer show client details.") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = ServiceSphereDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

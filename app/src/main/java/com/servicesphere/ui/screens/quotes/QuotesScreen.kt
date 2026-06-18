package com.servicesphere.ui.screens.quotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun QuotesScreen(
    onAddQuote: () -> Unit,
    onQuoteClick: (String) -> Unit,
    viewModel: QuotesViewModel = viewModel(
        factory = QuotesViewModel.Factory(
            ServiceLocator.quoteRepository,
            ServiceLocator.lineItemRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<QuoteUiModel?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenHeader("Quotes", "Build professional estimates with line items and tax.") }
            item { ServiceSphereSearchBar(uiState.searchQuery, viewModel::onSearchQueryChanged, "Search quotes") }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuoteFilterChip("All", uiState.selectedStatusFilter == null) { viewModel.onStatusFilterChanged(null) }
                    quoteStatuses.forEach { status ->
                        QuoteFilterChip(status.toDisplayStatus(), uiState.selectedStatusFilter == status) { viewModel.onStatusFilterChanged(status) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${uiState.quotes.size} quotes", style = MaterialTheme.typography.labelLarge, color = ServiceSphereTextSecondary)
                    StatusChip(uiState.selectedStatusFilter?.toDisplayStatus() ?: "All", StatusTone.Info)
                }
            }
            uiState.errorMessage?.let { message ->
                item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } }
            }
            when {
                uiState.isLoading -> item { LoadingQuotes() }
                uiState.quotes.isEmpty() && uiState.searchQuery.isBlank() && uiState.selectedStatusFilter == null -> item {
                    EmptyState(
                        title = "No quotes yet",
                        message = "Create a quote first, then convert it into an invoice when approved.",
                        icon = Icons.Filled.Description,
                        actionLabel = "Create Quote",
                        onAction = onAddQuote
                    )
                }
                uiState.quotes.isEmpty() -> item {
                    EmptyState("No matching quotes", "Try adjusting your search or status filter.", Icons.Filled.Description)
                }
                else -> items(uiState.quotes, key = { it.id }) { quote ->
                    QuoteListCard(quote, onClick = { onQuoteClick(quote.id) }, onDelete = { pendingDelete = quote })
                }
            }
        }
        FloatingActionButton(
            onClick = onAddQuote,
            containerColor = ServiceSpherePrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Quote")
        }
    }

    pendingDelete?.let { quote ->
        DeleteQuoteDialog(
            onDismiss = { pendingDelete = null },
            onDelete = {
                viewModel.deleteQuote(quote.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun QuoteFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick)) {
        StatusChip(label, if (selected) StatusTone.Primary else StatusTone.Neutral)
    }
}

@Composable
private fun QuoteListCard(quote: QuoteUiModel, onClick: () -> Unit, onDelete: () -> Unit) {
    ServiceSphereCard(accentColor = toneForQuote(quote.status).let(::quoteToneColor)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(quote.quoteNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(quote.clientName ?: "No client linked", color = ServiceSphereTextSecondary)
                    quote.jobTitle?.let { Text(it, color = ServiceSphereTextSecondary) }
                }
                StatusChip(quote.displayStatus, toneForQuote(quote.status))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Issued ${quote.displayIssueDate}", color = ServiceSphereTextSecondary)
                    quote.displayValidUntil?.let { Text("Valid until $it", color = ServiceSphereTextSecondary) }
                }
                Text(quote.displayTotal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) { Text("Delete", color = ServiceSphereDanger) }
            }
        }
    }
}

@Composable
private fun LoadingQuotes() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading quotes", modifier = Modifier.padding(top = 12.dp), color = ServiceSphereTextSecondary)
    }
}

@Composable
fun DeleteQuoteDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete quote?") },
        text = { Text("This will remove the quote and its line items from your local records.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = ServiceSphereDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun toneForQuote(status: String): StatusTone = when (status) {
    "SENT" -> StatusTone.Primary
    "ACCEPTED" -> StatusTone.Success
    "REJECTED" -> StatusTone.Danger
    "CONVERTED_TO_INVOICE" -> StatusTone.Warning
    else -> StatusTone.Neutral
}

private fun quoteToneColor(tone: StatusTone) = com.servicesphere.ui.components.toneColor(tone)

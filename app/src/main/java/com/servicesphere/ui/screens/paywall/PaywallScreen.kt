package com.servicesphere.ui.screens.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.BuildConfig
import com.servicesphere.billing.FreePlanLimits
import com.servicesphere.billing.PaywallTrigger
import com.servicesphere.billing.SubscriptionPackageUiModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.LimitProgressCard
import com.servicesphere.ui.components.PremiumFeatureRow
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun PaywallScreen(
    source: String?,
    onMaybeLater: () -> Unit,
    viewModel: PaywallViewModel = viewModel(
        factory = PaywallViewModel.Factory(
            ServiceLocator.subscriptionRepository,
            ServiceLocator.featureGateManager,
            PaywallTrigger.fromRouteValue(source)
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(state.errorMessage, state.successMessage) {
        (state.errorMessage ?: state.successMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Column {
        SnackbarHost(snackbar)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ScreenHeader(
                    title = state.trigger.title,
                    subtitle = if (state.isPro) {
                        "${state.currentPlanName} is active."
                    } else {
                        state.trigger.body
                    }
                )
            }
            if (!state.isRevenueCatConfigured) {
                item {
                    ServiceSphereCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Subscriptions are not configured for this build.", fontWeight = FontWeight.SemiBold)
                            Text("You can keep using the Free plan. Pro checkout will appear here once RevenueCat and Google Play products are configured.", color = ServiceSphereTextSecondary)
                        }
                    }
                }
            }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumFeatureRow("Unlimited clients and jobs", "Grow without Free plan limits.")
                        PremiumFeatureRow("Unlimited quotes and invoices", "Send as many documents as your work requires.")
                        PremiumFeatureRow("Professional PDF exports", "Share polished quotes and invoices.")
                        PremiumFeatureRow("Customer signatures", "Document approvals on jobs and invoices.")
                        PremiumFeatureRow("Job photo history", "Keep full site proof with every job.")
                        PremiumFeatureRow("Business branding", "Add your logo and business details to PDFs.")
                        PremiumFeatureRow("Advanced reports", "Use deeper business reporting as it becomes available.")
                    }
                }
            }
            if (state.isLoading && state.packages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val packages = state.packages.ifEmpty { fallbackPackages() }
                items(packages, key = { it.id }) { packageUi ->
                    SubscriptionPackageCard(
                        packageUi = packageUi,
                        selected = packageUi.id == state.selectedPackageId,
                        onSelect = { viewModel.selectPackage(packageUi.id) }
                    )
                }
                item {
                    ServiceSphereButton(
                        label = when {
                            state.isPro -> "Pro Active"
                            state.isPurchasing -> "Starting Pro..."
                            else -> "Try Pro free for 14 days"
                        },
                        enabled = !state.isPro && !state.isPurchasing && state.isRevenueCatConfigured && state.packages.isNotEmpty(),
                        onClick = { viewModel.purchaseSelectedPackage(activity) }
                    )
                }
                item {
                    ServiceSphereOutlinedButton(
                        label = if (state.isRestoring) "Restoring..." else "Restore Purchases",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::restorePurchases
                    )
                }
                if (state.packages.isEmpty() && state.isRevenueCatConfigured) {
                    item {
                        ServiceSphereCard {
                            Text("No subscription packages are available yet.")
                        }
                    }
                }
            }
            item { LimitProgressCard("Clients", state.usage.clients, FreePlanLimits.maxClients) }
            item { LimitProgressCard("Jobs", state.usage.activeJobs, FreePlanLimits.maxJobs) }
            item { LimitProgressCard("Quotes", state.usage.quotes, FreePlanLimits.maxQuotes) }
            item { LimitProgressCard("Invoices", state.usage.invoices, FreePlanLimits.maxInvoices) }
            item { LimitProgressCard("Signatures", state.usage.signatures, FreePlanLimits.maxSignatures) }
            if (BuildConfig.DEBUG) {
                item {
                    ServiceSphereCard {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Developer Pro Preview", fontWeight = FontWeight.SemiBold)
                                Text("Debug-only local entitlement. Not a real purchase.")
                            }
                            Switch(checked = state.debugProEnabled, onCheckedChange = viewModel::setDebugProEnabled)
                        }
                    }
                }
            }
            item { ServiceSphereOutlinedButton("Continue with Free", modifier = Modifier.fillMaxWidth(), onClick = { viewModel.maybeLater(onMaybeLater) }) }
        }
    }
}

@Composable
private fun SubscriptionPackageCard(
    packageUi: SubscriptionPackageUiModel,
    selected: Boolean,
    onSelect: () -> Unit
) {
    ServiceSphereCard(onClick = onSelect) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = ServiceSpherePrimary, modifier = Modifier.size(24.dp))
                    Column {
                        Text(packageUi.displayTitle(), fontWeight = FontWeight.Bold)
                        Text(packageUi.displayDescription(), color = ServiceSphereTextSecondary)
                    }
                }
                if (packageUi.isBestValue) StatusChip("Best value", StatusTone.Primary)
            }
            Text("${packageUi.priceText} / ${packageUi.periodText}", fontWeight = FontWeight.SemiBold)
            Text(if (selected) "Selected" else "Tap to select")
        }
    }
}

private fun SubscriptionPackageUiModel.displayTitle(): String = when {
    isBestValue -> "Pro Yearly"
    id.contains("month", ignoreCase = true) || periodText.contains("month", ignoreCase = true) -> "Pro Monthly"
    else -> title
}

private fun SubscriptionPackageUiModel.displayDescription(): String = when {
    isBestValue -> "About ${'$'}4.17/month, billed yearly"
    id.contains("month", ignoreCase = true) || periodText.contains("month", ignoreCase = true) -> "${'$'}6.99/month"
    else -> description
}

private fun fallbackPackages(): List<SubscriptionPackageUiModel> = listOf(
    SubscriptionPackageUiModel(
        id = "servicesphere_pro_monthly",
        title = "Pro Monthly",
        priceText = "${'$'}6.99",
        periodText = "month",
        description = "${'$'}6.99/month"
    ),
    SubscriptionPackageUiModel(
        id = "servicesphere_pro_yearly",
        title = "Pro Yearly",
        priceText = "${'$'}49.99",
        periodText = "year",
        description = "About ${'$'}4.17/month, billed yearly",
        isBestValue = true
    )
)

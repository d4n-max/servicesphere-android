package com.servicesphere.ui.screens.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.servicesphere.billing.SubscriptionPackageUiModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.LimitProgressCard
import com.servicesphere.ui.components.PremiumFeatureRow
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton

@Composable
fun PaywallScreen(
    source: String?,
    onMaybeLater: () -> Unit,
    viewModel: PaywallViewModel = viewModel(
        factory = PaywallViewModel.Factory(ServiceLocator.subscriptionRepository, ServiceLocator.featureGateManager)
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
                    title = "Run your service business without limits",
                    subtitle = if (state.isPro) {
                        "${state.currentPlanName} is active."
                    } else {
                        "Unlock unlimited jobs, quotes, invoices, photo proof, signatures, and professional PDFs."
                    }
                )
            }
            if (!state.isRevenueCatConfigured) {
                item {
                    ServiceSphereCard {
                        Text(
                            "Subscriptions are not configured for this build.",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item {
                ServiceSphereCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumFeatureRow("Unlimited clients and jobs", "Grow without Free plan limits.")
                        PremiumFeatureRow("Unlimited quotes and invoices", "Send as many documents as your work requires.")
                        PremiumFeatureRow("Professional PDFs", "Remove the Free watermark and add your business logo.")
                        PremiumFeatureRow("Unlimited proof and approvals", "Capture unlimited photos and signatures.")
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
                items(state.packages, key = { it.id }) { packageUi ->
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
                            else -> "Start Pro"
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
            item { LimitProgressCard("Active Jobs", state.usage.activeJobs, FreePlanLimits.maxJobs) }
            item { LimitProgressCard("Quotes this month", state.usage.quotesThisMonth, FreePlanLimits.maxQuotesPerMonth) }
            item { LimitProgressCard("Invoices this month", state.usage.invoicesThisMonth, FreePlanLimits.maxInvoicesPerMonth) }
            item { LimitProgressCard("Signatures this month", state.usage.signaturesThisMonth, FreePlanLimits.maxSignaturesPerMonth) }
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
            item { ServiceSphereOutlinedButton("Maybe Later", modifier = Modifier.fillMaxWidth(), onClick = { viewModel.maybeLater(onMaybeLater) }) }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(packageUi.title, fontWeight = FontWeight.Bold)
                    Text(packageUi.description)
                }
                if (packageUi.isBestValue) Text("Best Value", fontWeight = FontWeight.SemiBold)
            }
            Text("${packageUi.priceText} / ${packageUi.periodText}", fontWeight = FontWeight.SemiBold)
            Text(if (selected) "Selected" else "Tap to select")
        }
    }
}

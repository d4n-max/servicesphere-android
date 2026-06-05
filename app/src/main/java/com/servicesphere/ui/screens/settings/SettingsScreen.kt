package com.servicesphere.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.servicesphere.BuildConfig
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.PaywallBenefitRow
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.SettingsRow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBusinessProfile: () -> Unit,
    onBusinessSetup: () -> Unit,
    onCurrencyTax: () -> Unit,
    onDocumentSettings: () -> Unit,
    onReminderSettings: () -> Unit,
    onSubscription: () -> Unit,
    onDataPrivacy: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val subscriptionState by ServiceLocator.subscriptionRepository.subscriptionState.collectAsState(initial = com.servicesphere.billing.SubscriptionState())

    androidx.compose.foundation.layout.Column {
        SnackbarHost(snackbar)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ScreenHeader("Settings", "Configure the business details behind your workspace.") }
            item { SettingsRow("Business Profile", "Name, owner, phone, email, logo", onClick = onBusinessProfile) }
            item { SettingsRow("Run Setup Wizard", "Review first-run business setup without resetting data", onClick = onBusinessSetup) }
            item { SettingsRow("Currency & Tax", "Default currency and tax rate", onClick = onCurrencyTax) }
            item { SettingsRow("Document Settings", "Numbering, payment terms, and default notes", onClick = onDocumentSettings) }
            item { SettingsRow("Notifications & Reminders", "Default job reminders and notification access", onClick = onReminderSettings) }
            item { SettingsRow("Subscription", if (subscriptionState.isPro) "ServiceSphere Pro" else "Free plan", onClick = onSubscription) }
            item {
                ServiceSphereCard {
                    PaywallBenefitRow(
                        icon = Icons.Filled.WorkspacePremium,
                        title = "ServiceSphere Pro",
                        body = "Unlimited clients, jobs, invoices, logo branding, and clean PDFs."
                    )
                }
            }
            item {
                SettingsRow("Privacy Policy", "Draft ready for hosted release page") {
                    scope.launch { snackbar.showSnackbar("Privacy Policy draft is in docs/PRIVACY_POLICY.md.") }
                }
            }
            item {
                SettingsRow("Terms", "Draft ready for hosted release page") {
                    scope.launch { snackbar.showSnackbar("Terms draft is in docs/TERMS_OF_USE.md.") }
                }
            }
            item {
                SettingsRow("Export Data", "Create local JSON and CSV exports", onClick = onDataPrivacy)
            }
            item {
                SettingsRow("Delete Data", "Clear local records from this device", danger = true, onClick = onDataPrivacy)
            }
            item {
                SettingsRow("App Version", BuildConfig.VERSION_NAME)
            }
            item {
                ServiceSphereCard {
                    Text("Your business profile powers quote and invoice PDFs generated offline on this device.")
                }
            }
        }
    }
}

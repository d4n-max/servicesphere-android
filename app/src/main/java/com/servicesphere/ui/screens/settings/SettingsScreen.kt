package com.servicesphere.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servicesphere.BuildConfig
import com.servicesphere.data.ServiceLocator
import com.servicesphere.ui.components.PaywallBenefitRow
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.SettingsRow
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBusinessProfile: () -> Unit,
    onBusinessSetup: () -> Unit,
    onCurrencyTax: () -> Unit,
    onDocumentSettings: () -> Unit,
    onReminderSettings: () -> Unit,
    onSubscription: () -> Unit,
    onDataPrivacy: () -> Unit,
    onReplayWalkthrough: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
            item { SettingsRow("Replay walkthrough", "Review the main ServiceSphere workflows", onClick = onReplayWalkthrough) }
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
                SettingsRow("Rate ServiceSphere", "Share your feedback on Google Play") {
                    scope.launch {
                        if (!openPlayStoreListing(context)) {
                            snackbar.showSnackbar("No app is available to open the ServiceSphere Play Store page.")
                        }
                    }
                }
            }
            item { HelpAndSupportSection() }
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

@Composable
private fun HelpAndSupportSection() {
    var expandedQuestion by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Help & Support")
        supportFaqItems.forEachIndexed { index, item ->
            val expanded = expandedQuestion == item.question
            ServiceSphereCard(
                accentColor = if (index % 2 == 0) ServiceSpherePrimary else ServiceSphereSecondary,
                onClick = { expandedQuestion = if (expanded) null else item.question }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.question,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse answer" else "Expand answer",
                            tint = ServiceSpherePrimary
                        )
                    }
                    if (expanded) {
                        Text(
                            text = item.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ServiceSphereTextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun openPlayStoreListing(context: Context): Boolean {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET_URI))
    if (context.tryStartActivity(marketIntent)) return true

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URI))
    return context.tryStartActivity(webIntent)
}

private fun Context.tryStartActivity(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

private data class SupportFaqItem(
    val question: String,
    val answer: String
)

private const val PLAY_STORE_MARKET_URI = "market://details?id=com.servicesphere.app"
private const val PLAY_STORE_WEB_URI = "https://play.google.com/store/apps/details?id=com.servicesphere.app"

private val supportFaqItems = listOf(
    SupportFaqItem(
        question = "Where is my data stored?",
        answer = "ServiceSphere stores your core business records locally on this device. Exports, PDFs, photos, and shared files are only sent elsewhere when you choose to share or save them with another app."
    ),
    SupportFaqItem(
        question = "Can I use ServiceSphere offline?",
        answer = "Yes. Clients, jobs, quotes, invoices, photo proof, signatures, and local exports are designed to work without cloud sync. Some Android share targets, subscription checks, or Play Store links may still need connectivity."
    ),
    SupportFaqItem(
        question = "How do I export my data?",
        answer = "Open Settings, choose Export Data, then create a local JSON and CSV export. You can share the export through Android's share sheet when you are ready."
    ),
    SupportFaqItem(
        question = "How do I create a quote or invoice?",
        answer = "Use the bottom navigation to open Quotes or Invoices, then tap the add button. You can also create quotes or invoices from related job and client detail screens when available."
    ),
    SupportFaqItem(
        question = "How do I add photo proof or signatures?",
        answer = "Open a job or invoice detail screen and use the photo proof or signature actions. Photos and signatures stay tied to your local records and can be included in supported PDFs."
    ),
    SupportFaqItem(
        question = "How do I contact support?",
        answer = "Use the support contact listed on the Google Play testing page or your closed-testing invite. Include your app version, device model, and the steps that led to the issue."
    )
)

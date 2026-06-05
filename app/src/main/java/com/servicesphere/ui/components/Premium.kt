package com.servicesphere.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun ProBadge() {
    StatusChip("Pro", StatusTone.Primary)
}

@Composable
fun UpgradeToProButton(label: String = "Upgrade to Pro", modifier: Modifier = Modifier, onClick: () -> Unit) {
    ServiceSphereButton(label, modifier, onClick = onClick)
}

@Composable
fun PremiumFeatureRow(title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = ServiceSpherePrimary)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = ServiceSphereTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PremiumGateDialog(gate: FeatureGateResult, onDismiss: () -> Unit, onUpgrade: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(gate.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(gate.message)
                Text("Pro unlocks unlimited jobs, quotes, invoices, photo proof, signatures, and professional PDFs.", color = ServiceSphereTextSecondary)
            }
        },
        confirmButton = { TextButton(onClick = onUpgrade) { Text(gate.suggestedActionLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Maybe Later") } }
    )
}

@Composable
fun LimitProgressCard(label: String, used: Int, limit: Int, modifier: Modifier = Modifier) {
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    ServiceSphereCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text("$used / $limit", color = ServiceSphereTextSecondary)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

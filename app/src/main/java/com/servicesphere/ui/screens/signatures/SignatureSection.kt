package com.servicesphere.ui.screens.signatures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
fun SignatureSection(
    title: String,
    emptyTitle: String,
    emptyMessage: String,
    uiState: SignaturesUiState,
    onCaptureSignature: () -> Unit,
    onPreview: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (SignatureUiModel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("$title (${uiState.signatures.size})", "Capture Signature", onCaptureSignature)
        when {
            uiState.isLoading -> ServiceSphereCard { Text("Loading signatures", color = ServiceSphereTextSecondary) }
            uiState.signatures.isEmpty() -> EmptyState(
                title = emptyTitle,
                message = emptyMessage,
                icon = Icons.Filled.Draw,
                actionLabel = "Capture Signature",
                onAction = onCaptureSignature
            )
            else -> uiState.signatures.forEach { signature ->
                SignatureCard(
                    signature = signature,
                    onClick = { onPreview(signature.id) },
                    onEdit = { onEdit(signature.id) },
                    onDelete = { onDelete(signature) }
                )
            }
        }
    }
}

@Composable
private fun SignatureCard(signature: SignatureUiModel, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ServiceSphereCard(accentColor = ServiceSpherePrimary, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SignatureImage(signature.localUri, Modifier.size(104.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(signature.signedBy ?: "Unsigned name", fontWeight = FontWeight.SemiBold)
                signature.approvalText?.let { Text(it, color = ServiceSphereTextSecondary, maxLines = 2) }
                Text(formatSignatureDate(signature.createdAt), color = ServiceSphereTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Text("Edit Details")
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp), tint = ServiceSphereDanger)
                        Text("Delete", color = ServiceSphereDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureImage(localUri: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ServiceSphereSecondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(localUri)
                .crossfade(true)
                .build(),
            contentDescription = "Client signature",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SignaturePreviewDialog(signature: SignatureUiModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(signature.signedBy ?: "Client signature") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SignatureImage(signature.localUri, Modifier.fillMaxWidth().height(260.dp))
                Text(signature.signedBy ?: "No signer name", fontWeight = FontWeight.SemiBold)
                Text(signature.approvalText ?: "No approval text", style = MaterialTheme.typography.bodyLarge)
                Text(formatSignatureDate(signature.createdAt), color = ServiceSphereTextSecondary)
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Edit Details") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = ServiceSphereDanger) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun EditSignatureDialog(
    signedBy: String,
    approvalText: String,
    onSignedByChange: (String) -> Unit,
    onApprovalTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceSphereTextField(signedBy, onSignedByChange, "Signed by")
                ServiceSphereTextField(approvalText, onApprovalTextChange, "Approval text", minLines = 3, maxLines = 5)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteSignatureDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete signature?") },
        text = { Text("This will remove the saved client signature from this record.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = ServiceSphereDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun formatSignatureDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

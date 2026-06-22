package com.servicesphere.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun ReviewPromptDialog(
    onPositive: () -> Unit,
    onFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How is ServiceSphere working for you?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A quick rating helps us understand what is working well for your service business.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServiceSphereTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onPositive) {
                Text("It's working well")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onFeedback) {
                    Text("I have feedback")
                }
                TextButton(onClick = onDismiss) {
                    Text("Maybe later")
                }
            }
        }
    )
}

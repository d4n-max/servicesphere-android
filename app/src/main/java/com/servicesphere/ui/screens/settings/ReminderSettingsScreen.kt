package com.servicesphere.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servicesphere.data.ServiceLocator
import com.servicesphere.reminders.NotificationPermissionManager
import com.servicesphere.reminders.ReminderTypes
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.components.StatusTone
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ReminderSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultReminder by ServiceLocator.preferences.defaultJobReminderType.collectAsState(initial = ReminderTypes.NONE)
    val autoDisable by ServiceLocator.preferences.autoDisableCompletedJobReminders.collectAsState(initial = true)
    var showDefaultDialog by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(NotificationPermissionManager.hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Notifications & Reminders", "Control local reminders for scheduled jobs.") }
        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notifications status", fontWeight = FontWeight.SemiBold)
                    StatusChip(if (permissionGranted) "Enabled" else "Permission needed", if (permissionGranted) StatusTone.Success else StatusTone.Warning)
                    if (!permissionGranted) {
                        Text("Notification permission is needed to receive reminders", color = ServiceSphereTextSecondary)
                        ServiceSphereButton("Enable Notifications") {
                            if (NotificationPermissionManager.needsRuntimePermission()) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }
            }
        }
        item {
            ServiceSphereCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Default reminder", fontWeight = FontWeight.SemiBold)
                    Text(ReminderTypes.label(defaultReminder), color = ServiceSphereTextSecondary)
                    ServiceSphereOutlinedButton("Change Default", modifier = Modifier.fillMaxWidth()) { showDefaultDialog = true }
                }
            }
        }
        item {
            ServiceSphereCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-disable completed jobs", fontWeight = FontWeight.SemiBold)
                        Text("Turn off reminders when a job is completed or cancelled.", color = ServiceSphereTextSecondary)
                    }
                    Switch(
                        checked = autoDisable,
                        onCheckedChange = { value ->
                            scope.launch { ServiceLocator.preferences.setAutoDisableCompletedJobReminders(value) }
                        }
                    )
                }
            }
        }
        item { ServiceSphereOutlinedButton("Back", modifier = Modifier.fillMaxWidth(), onClick = onBack) }
    }

    if (showDefaultDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultDialog = false },
            title = { Text("Default reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderTypes.formOptions.forEach { type ->
                        TextButton(onClick = {
                            scope.launch { ServiceLocator.preferences.setDefaultJobReminderType(type) }
                            showDefaultDialog = false
                        }) {
                            Text(ReminderTypes.label(type))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDefaultDialog = false }) { Text("Cancel") } }
        )
    }
}

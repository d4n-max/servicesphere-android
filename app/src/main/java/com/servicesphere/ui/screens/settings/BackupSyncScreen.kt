package com.servicesphere.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.servicesphere.data.ServiceLocator
import com.servicesphere.sync.CloudBackupService
import com.servicesphere.sync.FirebaseAuthRepository
import com.servicesphere.sync.SyncWorkScheduler
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); val auth = remember { FirebaseAuthRepository() }; val backup = remember { CloudBackupService() }
    val syncEnabled by ServiceLocator.preferences.cloudSyncEnabled.collectAsState(initial = false)
    val wifiOnly by ServiceLocator.preferences.cloudSyncWifiOnly.collectAsState(initial = false)
    val lastBackup by ServiceLocator.preferences.cloudBackupLastSuccessAt.collectAsState(initial = null)
    val lastSize by ServiceLocator.preferences.cloudBackupLastSizeBytes.collectAsState(initial = null)
    var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    val signIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch { auth.completeGoogleSignIn(result.data).onSuccess { message = "Signed in. Your local records remain available offline." }.onFailure { message = "Sign-in needs attention. Your local records are safe on this device." } }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup & Sync") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Optional cloud protection", style = MaterialTheme.typography.headlineSmall); Text("ServiceSphere works offline. Enable cloud backup only when you choose.") }
            item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Account", style = MaterialTheme.typography.titleMedium)
                Text(auth.signedInEmail ?: "Not signed in")
                if (auth.isSignedIn) OutlinedButton(onClick = { auth.signOut(); scope.launch { ServiceLocator.preferences.setCloudSyncEnabled(false); SyncWorkScheduler.cancel(ServiceLocator.appContext) } }) { Text("Sign out") }
                else Button(onClick = { runCatching { signIn.launch(auth.signInIntent(ServiceLocator.appContext)) }.onFailure { message = "Google sign-in is not configured for this build. Your local records are safe on this device." } }) { Text("Sign in to back up") }
                Text("Signing out stops cloud sync. Local records stay on this device.", style = MaterialTheme.typography.bodySmall)
            } } }
            item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Automatic sync", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Sync across devices"); Switch(checked = syncEnabled, enabled = auth.isSignedIn, onCheckedChange = { enabled -> scope.launch { ServiceLocator.preferences.setCloudSyncEnabled(enabled); if (enabled) SyncWorkScheduler.enqueue(ServiceLocator.appContext) else SyncWorkScheduler.cancel(ServiceLocator.appContext) } }) }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Wi-Fi only"); Switch(checked = wifiOnly, enabled = auth.isSignedIn, onCheckedChange = { scope.launch { ServiceLocator.preferences.setCloudSyncWifiOnly(it) } }) }
                Text(if (syncEnabled) "Changes are saved locally first and upload when a connection is available." else "Sync is disabled. Local records are safe on this device.", style = MaterialTheme.typography.bodySmall)
            } } }
            item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cloud backup", style = MaterialTheme.typography.titleMedium)
                Text(lastBackup?.let { "Last backup: ${DateFormat.getDateTimeInstance().format(Date(it))}${lastSize?.let { size -> " · ${size / 1024} KB" }.orEmpty()}" } ?: "No cloud backup yet")
                Button(enabled = auth.isSignedIn && !busy, onClick = { scope.launch { busy = true; backup.backUpNow().onSuccess { message = "Backup completed." }.onFailure { message = "Backup failed. Local records are safe; you can try again." }; busy = false } }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Back up now") }
                Text("Backups include structured records. Photos and signatures are uploaded separately when sync is enabled.", style = MaterialTheme.typography.bodySmall)
            } } }
            message?.let { item { AssistChip(onClick = { message = null }, label = { Text(it) }) } }
        }
    }
}

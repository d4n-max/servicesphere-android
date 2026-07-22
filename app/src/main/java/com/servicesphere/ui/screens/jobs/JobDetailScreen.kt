package com.servicesphere.ui.screens.jobs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.servicesphere.billing.FeatureGateResult
import com.servicesphere.activation.ActivationEvents
import com.servicesphere.activation.ActivationParams
import com.servicesphere.data.ServiceLocator
import com.servicesphere.data.local.JobPhotoStorage
import com.servicesphere.data.local.SignatureImageStorage
import com.servicesphere.domain.model.JobStatus
import com.servicesphere.messaging.MessageTemplateType
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.QuickActionButton
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.SectionHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.components.StatusChip
import com.servicesphere.ui.screens.signatures.DeleteSignatureDialog
import com.servicesphere.ui.screens.signatures.EditSignatureDialog
import com.servicesphere.ui.screens.signatures.SignaturePreviewDialog
import com.servicesphere.ui.screens.signatures.SignatureSection
import com.servicesphere.ui.screens.signatures.SignatureUiModel
import com.servicesphere.ui.screens.signatures.SignaturesViewModel
import com.servicesphere.ui.theme.ServiceSphereDanger
import com.servicesphere.ui.theme.ServiceSpherePrimary
import com.servicesphere.ui.theme.ServiceSphereSecondaryContainer
import com.servicesphere.ui.theme.ServiceSphereTextSecondary
import com.servicesphere.ui.timeline.ActivityTimeline
import com.servicesphere.ui.timeline.TimelineTarget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun JobDetailScreen(
    jobId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onCreateQuote: () -> Unit,
    onCreateInvoice: () -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    onComposeMessage: (MessageTemplateType) -> Unit,
    onCaptureSignature: () -> Unit,
    onPhotoGateBlocked: (FeatureGateResult) -> Unit,
    onJobCompleted: () -> Unit,
    isSampleJob: Boolean = false,
    onCreateRealFirstJob: () -> Unit = {},
    onBusinessSetup: () -> Unit = {},
    viewModel: JobDetailViewModel = viewModel(
        factory = JobDetailViewModel.Factory(
            jobId,
            ServiceLocator.jobRepository,
            ServiceLocator.clientRepository,
            ServiceLocator.jobReminderRepository,
            ServiceLocator.reminderScheduler,
            ServiceLocator.workflowRepository,
            ServiceLocator.quoteRepository,
            ServiceLocator.invoiceRepository,
            ServiceLocator.analyticsTracker
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val photosViewModel: JobPhotosViewModel = viewModel(
        key = "job_photos_$jobId",
        factory = JobPhotosViewModel.Factory(
            jobId,
            ServiceLocator.jobPhotoRepository,
            JobPhotoStorage(context.applicationContext),
            ServiceLocator.activationTracker
        )
    )
    val photosUiState by photosViewModel.uiState.collectAsState()
    val signaturesViewModel: SignaturesViewModel = viewModel(
        key = "job_signatures_$jobId",
        factory = SignaturesViewModel.Factory(
            ServiceLocator.signatureRepository.observeSignaturesForJob(jobId),
            ServiceLocator.signatureRepository,
            SignatureImageStorage(context.applicationContext)
        )
    )
    val signaturesUiState by signaturesViewModel.uiState.collectAsState()
    val businessSetupComplete by ServiceLocator.preferences.hasCompletedBusinessSetup.collectAsState(initial = false)
    val currentJob = uiState.job
    val hasMeaningfulActivationDetail = currentJob?.hasMeaningfulActivationDetail == true || photosUiState.photos.isNotEmpty()
    val showBusinessSetupPrompt = currentJob != null && !isSampleJob && !businessSetupComplete
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var photoToDelete by remember { mutableStateOf<JobPhotoUiModel?>(null) }
    var signatureToDelete by remember { mutableStateOf<SignatureUiModel?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photosViewModel.addPhotoFromUri(jobId, uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photosViewModel.addCapturedPhoto(jobId, pendingCameraUri) else pendingCameraUri = null
    }
    fun requestAddPhoto() {
        scope.launch {
            val gate = ServiceLocator.featureGateManager.canAddPhotoProof(jobId)
            if (gate.allowed) showAddPhotoDialog = true else onPhotoGateBlocked(gate)
        }
    }
    fun createCameraUriOrNotify(): Uri? = runCatching {
        photosViewModel.createCameraImageUri()
    }.getOrElse {
        scope.launch { snackbar.showSnackbar("Couldn't prepare the camera. Try choosing a photo from your gallery.") }
        null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            createCameraUriOrNotify()?.let { uri ->
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            scope.launch { snackbar.showSnackbar("Camera permission is needed to take job photos.") }
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onDeleted()
    }
    LaunchedEffect(uiState.convertedInvoiceId) {
        uiState.convertedInvoiceId?.let { invoiceId ->
            viewModel.clearConvertedInvoice()
            onOpenInvoice(invoiceId)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.statusUpdateEvents.collect { status ->
            if (status == JobStatus.COMPLETED) onJobCompleted()
        }
    }
    LaunchedEffect(photosUiState.errorMessage) {
        photosUiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            photosViewModel.clearError()
        }
    }
    LaunchedEffect(photosUiState.successMessage) {
        photosUiState.successMessage?.let {
            snackbar.showSnackbar(it)
            photosViewModel.clearSuccess()
        }
    }
    LaunchedEffect(signaturesUiState.errorMessage) {
        signaturesUiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            signaturesViewModel.clearError()
        }
    }
    LaunchedEffect(signaturesUiState.successMessage) {
        signaturesUiState.successMessage?.let {
            snackbar.showSnackbar(it)
            signaturesViewModel.clearSuccess()
        }
    }
    LaunchedEffect(showBusinessSetupPrompt) {
        if (showBusinessSetupPrompt) {
            ServiceLocator.activationTracker.track(ActivationEvents.BUSINESS_SETUP_PROMPT_SEEN)
        }
    }
    LaunchedEffect(currentJob?.id, hasMeaningfulActivationDetail) {
        if (currentJob != null && hasMeaningfulActivationDetail) {
            ServiceLocator.activationTracker.trackFirst(
                ActivationEvents.ACTIVATION_FIRST_JOB_ORGANIZED,
                mapOf(
                    ActivationParams.SOURCE_SCREEN to "job_detail",
                    ActivationParams.HAS_CLIENT to (!currentJob.clientName.isNullOrBlank()).toString(),
                    ActivationParams.HAS_SCHEDULE to (!currentJob.displaySchedule.isNullOrBlank()).toString(),
                    ActivationParams.HAS_DETAILS to hasMeaningfulActivationDetail.toString(),
                    ActivationParams.JOB_STATUS to currentJob.status
                )
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> item { DetailLoading() }
                uiState.job == null -> item {
                    EmptyState("Job not found", "This local job record is missing or was deleted.", Icons.Filled.WorkOff, "Back", onBack)
                }
                else -> {
                    val job = uiState.job!!
                    if (isSampleJob) {
                        item {
                            SampleJobBanner(onCreateRealFirstJob)
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScreenHeader(job.title, job.clientName ?: "No client linked")
                            StatusChip(job.displayStatus, toneForJob(job.status))
                        }
                    }
                    item {
                        FirstUseJobPrompt(
                            hasMeaningfulDetail = hasMeaningfulActivationDetail,
                            showBusinessSetupPrompt = showBusinessSetupPrompt,
                            onAddQuote = onCreateQuote,
                            onAddNote = { onEdit(job.id) },
                            onAddPhoto = ::requestAddPhoto,
                            onCreateInvoice = onCreateInvoice,
                            onBusinessSetup = onBusinessSetup
                        )
                    }
                    uiState.errorMessage?.let { message ->
                        item { ServiceSphereCard { Text(message, color = ServiceSphereDanger) } }
                    }
                    item { JobDetailsCard(job) }
                    item {
                        ReminderDetailSection(
                            reminder = uiState.reminder,
                            notificationsEnabled = com.servicesphere.reminders.NotificationPermissionManager.hasNotificationPermission(context),
                            onEdit = { onEdit(job.id) },
                            onDisable = viewModel::disableReminder
                        )
                    }
                    item {
                        JobQuickActions(
                            job = job,
                            onCreateQuote = onCreateQuote,
                            onCreateInvoice = onCreateInvoice,
                            onAddPhoto = ::requestAddPhoto,
                            onCaptureSignature = onCaptureSignature
                        )
                    }
                    if (job.status != JobStatus.CANCELLED) item {
                        ServiceSphereButton(
                            if (uiState.isConverting) "Creating invoice..." else if (uiState.linkedInvoiceId != null) "View invoice" else "Create invoice",
                            Modifier.fillMaxWidth(),
                            onClick = { uiState.linkedInvoiceId?.let(onOpenInvoice) ?: viewModel.createInvoice() }
                        )
                    }
                    item { JobMessageActions(onComposeMessage) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Back", Modifier.weight(1f), onClick = onBack)
                            ServiceSphereButton("Edit Job", Modifier.weight(1f), onClick = { onEdit(job.id) })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereOutlinedButton("Change Status", Modifier.weight(1f), onClick = { showStatusDialog = true })
                            ServiceSphereOutlinedButton("Delete Job", Modifier.weight(1f), onClick = { showDeleteDialog = true })
                        }
                    }
                    item {
                        PhotoProofSection(
                            uiState = photosUiState,
                            onAddPhoto = ::requestAddPhoto,
                            onPhotoSelected = photosViewModel::onPhotoSelected,
                            onEditCaption = photosViewModel::startEditCaption,
                            onDeletePhoto = { photoToDelete = it }
                        )
                    }
                    item {
                        SignatureSection(
                            title = "Client Signatures",
                            emptyTitle = "No signature yet",
                            emptyMessage = "Capture client approval when the job is completed.",
                            uiState = signaturesUiState,
                            onCaptureSignature = onCaptureSignature,
                            onPreview = signaturesViewModel::onSignatureSelected,
                            onEdit = signaturesViewModel::startEditSignature,
                            onDelete = { signatureToDelete = it }
                        )
                    }
                    item {
                        ServiceSphereCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Related records", fontWeight = FontWeight.Bold)
                                uiState.sourceQuoteId?.let { ServiceSphereOutlinedButton("Source quote", Modifier.fillMaxWidth(), onClick = { onOpenQuote(it) }) }
                                uiState.linkedInvoiceId?.let { ServiceSphereOutlinedButton("Invoice", Modifier.fillMaxWidth(), onClick = { onOpenInvoice(it) }) }
                            }
                        }
                    }
                    item { ActivityTimeline(uiState.timeline) { target, id -> when (target) { TimelineTarget.QUOTE -> onOpenQuote(id); TimelineTarget.INVOICE -> onOpenInvoice(id); TimelineTarget.JOB -> Unit } } }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteJobDialog(
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteJob()
            }
        )
    }
    if (showStatusDialog) {
        StatusSelectorDialog(
            selectedStatus = uiState.job?.status.orEmpty(),
            onDismiss = { showStatusDialog = false },
            onSelect = {
                viewModel.updateStatus(it)
                showStatusDialog = false
            }
        )
    }
    if (showAddPhotoDialog) {
        AddPhotoDialog(
            onDismiss = { showAddPhotoDialog = false },
            onChooseGallery = {
                showAddPhotoDialog = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTakePhoto = {
                showAddPhotoDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    createCameraUriOrNotify()?.let { uri ->
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
    photosUiState.selectedPhoto?.let { photo ->
        PhotoPreviewDialog(
            photo = photo,
            onDismiss = photosViewModel::closePhotoPreview,
            onEditCaption = { photosViewModel.startEditCaption(photo.id) },
            onDelete = { photoToDelete = photo }
        )
    }
    if (photosUiState.captionEditPhotoId != null) {
        EditCaptionDialog(
            caption = photosUiState.captionDraft,
            onCaptionChange = photosViewModel::onCaptionChanged,
            onDismiss = photosViewModel::cancelCaptionEdit,
            onSave = photosViewModel::saveCaption
        )
    }
    photoToDelete?.let { photo ->
        DeletePhotoDialog(
            onDismiss = { photoToDelete = null },
            onConfirm = {
                photoToDelete = null
                photosViewModel.deletePhoto(photo.id)
            }
        )
    }
    signaturesUiState.selectedSignature?.let { signature ->
        SignaturePreviewDialog(
            signature = signature,
            onDismiss = signaturesViewModel::closePreview,
            onEdit = { signaturesViewModel.startEditSignature(signature.id) },
            onDelete = { signatureToDelete = signature }
        )
    }
    if (signaturesUiState.editSignatureId != null) {
        EditSignatureDialog(
            signedBy = signaturesUiState.signedByDraft,
            approvalText = signaturesUiState.approvalTextDraft,
            onSignedByChange = signaturesViewModel::onSignedByChanged,
            onApprovalTextChange = signaturesViewModel::onApprovalTextChanged,
            onDismiss = signaturesViewModel::cancelEdit,
            onSave = signaturesViewModel::saveSignatureMetadata
        )
    }
    signatureToDelete?.let { signature ->
        DeleteSignatureDialog(
            onDismiss = { signatureToDelete = null },
            onConfirm = {
                signatureToDelete = null
                signaturesViewModel.deleteSignature(signature.id)
            }
        )
    }
}

@Composable
private fun SampleJobBanner(onCreateRealFirstJob: () -> Unit) {
    ServiceSphereCard(accentColor = ServiceSpherePrimary) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sample job. Create your own when you're ready.", fontWeight = FontWeight.Bold)
            Text("Explore how a job workspace keeps notes, quotes, photos, signatures, and invoices together.", color = ServiceSphereTextSecondary)
            ServiceSphereButton("Create my real first job", onClick = onCreateRealFirstJob)
        }
    }
}

@Composable
private fun FirstUseJobPrompt(
    hasMeaningfulDetail: Boolean,
    showBusinessSetupPrompt: Boolean,
    onAddQuote: () -> Unit,
    onAddNote: () -> Unit,
    onAddPhoto: () -> Unit,
    onCreateInvoice: () -> Unit,
    onBusinessSetup: () -> Unit
) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasMeaningfulDetail) {
                Text("Nice. This job now has the details you need in the field.", fontWeight = FontWeight.Bold)
            } else {
                Text("Add one detail so this job is ready to work from.", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton("Add quote", Modifier.weight(1f), onClick = onAddQuote)
                    ServiceSphereOutlinedButton("Add note", Modifier.weight(1f), onClick = onAddNote)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSphereOutlinedButton("Add photo", Modifier.weight(1f), onClick = onAddPhoto)
                    ServiceSphereOutlinedButton("Create invoice", Modifier.weight(1f), onClick = onCreateInvoice)
                }
            }
            if (showBusinessSetupPrompt) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Finish business setup", fontWeight = FontWeight.Bold)
                    Text("Add your details once so quotes and invoices are ready later.", color = ServiceSphereTextSecondary)
                    ServiceSphereButton("Add business details", onClick = onBusinessSetup)
                    Text("Create first job • Add job details • Add business info • Create quote • Create invoice", color = ServiceSphereTextSecondary)
                }
            }
        }
    }
}

private val JobUiModel.hasMeaningfulActivationDetail: Boolean
    get() = !internalNotes.isNullOrBlank() || !address.isNullOrBlank() || estimatedPrice != null

@Composable
private fun JobMessageActions(onComposeMessage: (MessageTemplateType) -> Unit) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Message Client", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceSphereOutlinedButton("Completed", Modifier.weight(1f)) {
                    onComposeMessage(MessageTemplateType.JOB_COMPLETED)
                }
                ServiceSphereOutlinedButton("Thank You", Modifier.weight(1f)) {
                    onComposeMessage(MessageTemplateType.THANK_YOU_REVIEW_REQUEST)
                }
            }
        }
    }
}

@Composable
private fun ReminderDetailSection(
    reminder: JobReminderUiModel?,
    notificationsEnabled: Boolean,
    onEdit: () -> Unit,
    onDisable: () -> Unit
) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (reminder == null) Icons.Filled.NotificationsOff else Icons.Filled.Notifications, contentDescription = null, tint = ServiceSpherePrimary)
                Text("Reminder", fontWeight = FontWeight.SemiBold)
            }
            if (reminder == null) {
                Text("No reminder", color = ServiceSphereTextSecondary)
            } else {
                Text("Reminder: ${reminder.label}")
                Text(reminder.displayTime, color = ServiceSphereTextSecondary)
            }
            if (!notificationsEnabled) {
                Text("Notifications are off", color = ServiceSphereDanger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceSphereOutlinedButton("Edit Reminder", modifier = Modifier.weight(1f), onClick = onEdit)
                if (reminder != null) {
                    ServiceSphereOutlinedButton("Disable", modifier = Modifier.weight(1f), onClick = onDisable)
                }
            }
        }
    }
}

@Composable
private fun DetailLoading() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading job", color = ServiceSphereTextSecondary)
    }
}

@Composable
private fun JobDetailsCard(job: JobUiModel) {
    ServiceSphereCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailLine("Client", job.clientName)
            DetailLine("Schedule", job.displaySchedule)
            DetailLine("Address", job.address)
            DetailLine("Description", job.description)
            DetailLine("Estimated price", job.displayPrice)
            DetailLine("Internal notes", job.internalNotes)
            DetailLine("Created", formatSimpleDate(job.createdAt))
            DetailLine("Updated", formatSimpleDate(job.updatedAt))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = ServiceSphereTextSecondary)
            Text(value)
        }
    }
}

@Composable
private fun JobQuickActions(
    job: JobUiModel,
    onCreateQuote: () -> Unit,
    onCreateInvoice: () -> Unit,
    onAddPhoto: () -> Unit,
    onCaptureSignature: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionButton("Call", Icons.Filled.Call, primary = !job.clientPhone.isNullOrBlank()) {
                job.clientPhone?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")), "No phone app found")
                }
            }
            QuickActionButton("Email", Icons.Filled.Email) {
                job.clientEmail?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it")), "No email app found")
                }
            }
            QuickActionButton("Maps", Icons.Filled.LocationOn) {
                job.address?.takeIf { it.isNotBlank() }?.let {
                    context.tryStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(it)}")), "No maps app found")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionButton("Quote", Icons.Filled.Description, onClick = onCreateQuote)
            QuickActionButton("Invoice", Icons.AutoMirrored.Filled.ReceiptLong, onClick = onCreateInvoice)
            QuickActionButton("Photo", Icons.Filled.Image, onClick = onAddPhoto)
            QuickActionButton("Sign", Icons.Filled.Draw, onClick = onCaptureSignature)
        }
    }
}

@Composable
private fun PhotoProofSection(
    uiState: JobPhotosUiState,
    onAddPhoto: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    onEditCaption: (String) -> Unit,
    onDeletePhoto: (JobPhotoUiModel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Photo Proof (${uiState.photos.size})", "Add Photo", onAddPhoto)
        when {
            uiState.isLoading -> ServiceSphereCard { Text("Loading photos", color = ServiceSphereTextSecondary) }
            uiState.photos.isEmpty() -> EmptyState(
                title = "No job photos yet",
                message = "Add before-and-after photos or site details when you're on the job.",
                icon = Icons.Filled.AddPhotoAlternate,
                actionLabel = "Add photo",
                onAction = onAddPhoto
            )
            else -> uiState.photos.forEach { photo ->
                JobPhotoCard(
                    photo = photo,
                    onClick = { onPhotoSelected(photo.id) },
                    onEditCaption = { onEditCaption(photo.id) },
                    onDelete = { onDeletePhoto(photo) }
                )
            }
        }
    }
}

@Composable
private fun JobPhotoCard(photo: JobPhotoUiModel, onClick: () -> Unit, onEditCaption: () -> Unit, onDelete: () -> Unit) {
    ServiceSphereCard(accentColor = ServiceSpherePrimary, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(photo.localUri, Modifier.size(92.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(photo.caption ?: "No caption", fontWeight = FontWeight.SemiBold)
                Text(formatSimpleDate(photo.createdAt), color = ServiceSphereTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEditCaption) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Text("Edit Caption")
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
private fun PhotoImage(localUri: String, modifier: Modifier) {
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
            contentDescription = "Job photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AddPhotoDialog(onDismiss: () -> Unit, onChooseGallery: () -> Unit, onTakePhoto: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onChooseGallery) {
                    Icon(Icons.Filled.PhotoLibrary, null)
                    Text("Choose from Gallery")
                }
                TextButton(onClick = onTakePhoto) {
                    Icon(Icons.Filled.CameraAlt, null)
                    Text("Take Photo")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PhotoPreviewDialog(photo: JobPhotoUiModel, onDismiss: () -> Unit, onEditCaption: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(photo.caption ?: "Photo proof") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoImage(photo.localUri, Modifier.fillMaxWidth().height(320.dp))
                Text(photo.caption ?: "No caption", style = MaterialTheme.typography.bodyLarge)
                Text(formatSimpleDate(photo.createdAt), color = ServiceSphereTextSecondary)
            }
        },
        confirmButton = { TextButton(onClick = onEditCaption) { Text("Edit Caption") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = ServiceSphereDanger) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun EditCaptionDialog(caption: String, onCaptionChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Caption") },
        text = {
            ServiceSphereTextField(
                value = caption,
                onValueChange = onCaptionChange,
                label = "Caption",
                minLines = 2,
                maxLines = 4
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeletePhotoDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete photo?") },
        text = { Text("This will remove the photo proof from this job.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = ServiceSphereDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatSimpleDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

private fun android.content.Context.tryStartActivity(intent: Intent, failureMessage: String) {
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show() }
}

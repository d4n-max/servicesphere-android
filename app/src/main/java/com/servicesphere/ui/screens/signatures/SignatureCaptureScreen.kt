package com.servicesphere.ui.screens.signatures

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servicesphere.data.ServiceLocator
import com.servicesphere.data.local.SignatureImageStorage
import com.servicesphere.ui.components.EmptyState
import com.servicesphere.ui.components.ScreenHeader
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereCard
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import com.servicesphere.ui.components.ServiceSphereTextField
import com.servicesphere.ui.theme.ServiceSphereDarkSurface
import com.servicesphere.ui.theme.ServiceSphereOutline
import com.servicesphere.ui.theme.ServiceSphereSurface
import com.servicesphere.ui.theme.ServiceSphereTextSecondary

@Composable
fun SignatureCaptureScreen(
    jobId: String?,
    invoiceId: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SignatureCaptureViewModel = viewModel(
        factory = SignatureCaptureViewModel.Factory(
            ServiceLocator.signatureRepository,
            SignatureImageStorage(LocalContext.current.applicationContext),
            ServiceLocator.activationTracker
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(jobId, invoiceId) {
        viewModel.initialize(jobId, invoiceId)
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.saveSuccessSignatureId) {
        if (uiState.saveSuccessSignatureId != null) {
            snackbar.showSnackbar("Signature saved")
            viewModel.resetSaveSuccess()
            onSaved()
        }
    }

    Column(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (jobId.isNullOrBlank() && invoiceId.isNullOrBlank()) {
                item {
                    EmptyState(
                        title = "Signature target missing",
                        message = "Open signature capture from a job or invoice record.",
                        icon = Icons.Filled.Draw,
                        actionLabel = "Back",
                        onAction = onCancel
                    )
                }
            } else {
                item {
                    ScreenHeader(
                        title = if (invoiceId != null) "Capture Invoice Signature" else "Capture Job Signature",
                        subtitle = "Collect client approval with an offline saved signature."
                    )
                }
                item {
                    ServiceSphereCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceSphereTextField(uiState.signedBy, viewModel::onSignedByChanged, "Signed by")
                            ServiceSphereTextField(
                                value = uiState.approvalText,
                                onValueChange = viewModel::onApprovalTextChanged,
                                label = "Approval text",
                                minLines = 3,
                                maxLines = 5
                            )
                        }
                    }
                }
                item {
                    ServiceSphereCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Signature", fontWeight = FontWeight.Bold)
                            Text("Ask the client to sign inside the box below.", color = ServiceSphereTextSecondary)
                            SignatureCanvas(
                                strokes = strokes,
                                activeStroke = activeStroke,
                                onActiveStrokeChanged = { activeStroke = it },
                                onStrokeFinished = {
                                    if (it.size > 1) strokes.add(it)
                                    activeStroke = emptyList()
                                    viewModel.onSignatureDrawnChanged(strokes.isNotEmpty())
                                },
                                onSizeChanged = { canvasSize = it }
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ServiceSphereOutlinedButton(
                                    label = "Clear",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        strokes.clear()
                                        activeStroke = emptyList()
                                        viewModel.onSignatureDrawnChanged(false)
                                    }
                                )
                                ServiceSphereButton(
                                    label = "Save Signature",
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isSaving,
                                    onClick = {
                                        val bitmap = if (strokes.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                                            renderSignatureBitmap(strokes.toList(), canvasSize)
                                        } else {
                                            null
                                        }
                                        viewModel.saveSignature(bitmap)
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    ServiceSphereOutlinedButton("Cancel", Modifier.fillMaxWidth(), onClick = onCancel)
                }
            }
        }
    }
}

@Composable
private fun SignatureCanvas(
    strokes: List<List<Offset>>,
    activeStroke: List<Offset>,
    onActiveStrokeChanged: (List<Offset>) -> Unit,
    onStrokeFinished: (List<Offset>) -> Unit,
    onSizeChanged: (IntSize) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(ServiceSphereSurface, RoundedCornerShape(12.dp))
            .border(1.dp, ServiceSphereOutline, RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(onSizeChanged)
                .pointerInput(Unit) {
                    var currentStroke = emptyList<Offset>()
                    detectDragGestures(
                        onDragStart = { start ->
                            currentStroke = listOf(start)
                            onActiveStrokeChanged(currentStroke)
                        },
                        onDrag = { change, _ ->
                            currentStroke = currentStroke + change.position
                            onActiveStrokeChanged(currentStroke)
                        },
                        onDragEnd = { onStrokeFinished(currentStroke) },
                        onDragCancel = { onStrokeFinished(currentStroke) }
                    )
                }
        ) {
            (strokes + listOf(activeStroke)).forEach { stroke ->
                drawSignatureStroke(stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSignatureStroke(points: List<Offset>) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color = ServiceSphereDarkSurface, style = Stroke(width = 5f, cap = StrokeCap.Round))
}

private fun renderSignatureBitmap(strokes: List<List<Offset>>, size: IntSize): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(17, 24, 39)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { points ->
        if (points.size >= 2) {
            val path = android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}

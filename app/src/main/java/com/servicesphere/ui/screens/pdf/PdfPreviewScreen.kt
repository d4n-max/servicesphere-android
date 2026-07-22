package com.servicesphere.ui.screens.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.servicesphere.ui.components.ServiceSphereButton
import com.servicesphere.ui.components.ServiceSphereOutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Renders the actual generated PDF, never a Compose imitation of a document. */
@Composable
fun PdfPreviewScreen(title: String, filePath: String?, onRegenerate: () -> Unit, onShare: () -> Unit, onBack: () -> Unit) {
    val documentState by produceState<PdfPagesState>(PdfPagesState.Loading, filePath) {
        value = loadPdfPages(filePath)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title)
        when (val state = documentState) {
            PdfPagesState.Loading -> CircularProgressIndicator()
            is PdfPagesState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.message)
                ServiceSphereOutlinedButton("Retry", Modifier.fillMaxWidth(), onClick = onRegenerate)
            }
            is PdfPagesState.Ready -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(state.pages) { page -> Image(page.asImageBitmap(), "PDF page", Modifier.fillMaxWidth()) }
            }
        }
        ServiceSphereButton("Regenerate", Modifier.fillMaxWidth(), onClick = onRegenerate)
        ServiceSphereOutlinedButton("Share PDF", Modifier.fillMaxWidth(), onClick = onShare)
        ServiceSphereOutlinedButton("Back", Modifier.fillMaxWidth(), onClick = onBack)
    }
}

private sealed interface PdfPagesState { data object Loading : PdfPagesState; data class Ready(val pages: List<Bitmap>) : PdfPagesState; data class Error(val message: String) : PdfPagesState }

private suspend fun loadPdfPages(filePath: String?): PdfPagesState = withContext(Dispatchers.IO) {
    if (filePath.isNullOrBlank() || !File(filePath).isFile) return@withContext PdfPagesState.Error("The generated PDF is unavailable. Regenerate it to continue.")
    runCatching {
        ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                (0 until renderer.pageCount).map { index -> renderer.openPage(index).use { page ->
                    Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also { page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                } }
            }
        }
    }.fold({ PdfPagesState.Ready(it) }, { PdfPagesState.Error("The PDF could not be rendered. Regenerate it and try again.") })
}

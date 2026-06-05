package com.servicesphere.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JobPhotoStorage(private val context: Context) {
    suspend fun copyImageToAppStorage(jobId: String, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "job_photos").apply { mkdirs() }
        val destination = File(directory, safePhotoName(jobId))
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open selected image")
        Uri.fromFile(destination).toString()
    }

    fun createCameraImageUri(jobId: String): Uri {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, safePhotoName(jobId))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun deleteStoredPhoto(localUri: String) = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(localUri)
            val file = when (uri.scheme) {
                "file" -> uri.path?.let(::File)
                null -> File(localUri)
                else -> null
            }
            file?.takeIf { it.exists() }?.delete()
        }
    }

    private fun safePhotoName(jobId: String): String {
        val safeJobId = jobId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "job_photo_${safeJobId}_${System.currentTimeMillis()}.jpg"
    }
}

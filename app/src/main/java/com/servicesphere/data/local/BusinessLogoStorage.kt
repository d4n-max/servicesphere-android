package com.servicesphere.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BusinessLogoStorage(private val context: Context) {
    suspend fun copyLogoToAppStorage(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "business").apply { mkdirs() }
        val destination = File(directory, "business_logo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open selected logo")
        Uri.fromFile(destination).toString()
    }

    suspend fun deleteStoredLogo(localUri: String) = withContext(Dispatchers.IO) {
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
}

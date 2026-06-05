package com.servicesphere.data.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SignatureImageStorage(private val context: Context) {
    suspend fun saveSignatureBitmap(bitmap: Bitmap, parentId: String): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "signatures").apply { mkdirs() }
        val file = File(directory, safeSignatureName(parentId))
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        Uri.fromFile(file).toString()
    }

    suspend fun deleteStoredSignature(localUri: String) = withContext(Dispatchers.IO) {
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

    private fun safeSignatureName(parentId: String): String {
        val safeParentId = parentId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "signature_${safeParentId}_${System.currentTimeMillis()}.png"
    }
}

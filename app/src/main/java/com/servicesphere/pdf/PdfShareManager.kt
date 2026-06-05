package com.servicesphere.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class PdfShareManager(private val context: Context) {
    fun sharePdf(filePath: String, chooserTitle: String): String? = runCatching {
        val file = File(filePath)
        if (!file.exists()) error("PDF file not found")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    }.getOrElse { error -> error.message ?: "No app can share this PDF" }

    fun openPdf(filePath: String): String? = runCatching {
        val file = File(filePath)
        if (!file.exists()) error("PDF file not found")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        null
    }.getOrElse { error ->
        if (error is ActivityNotFoundException) "No PDF viewer app found." else error.message ?: "No PDF viewer app found."
    }
}

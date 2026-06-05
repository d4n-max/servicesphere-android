package com.servicesphere.messaging

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

class MessageShareManager(private val context: Context) {
    fun sharePlainText(message: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share message").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.mapCatching { Unit }.recoverCatching { throwable ->
        if (throwable is ActivityNotFoundException) {
            throw IllegalStateException("No app found to send this message", throwable)
        } else {
            throw throwable
        }
    }

    fun sendSms(phone: String?, message: String): Result<Unit> = runCatching {
        val value = phone?.takeIf { it.isNotBlank() } ?: error("No phone number for this client")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(value)}")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.mapCatching { Unit }.recoverCatching { throwable ->
        if (throwable is ActivityNotFoundException) {
            throw IllegalStateException("No app found to send this message", throwable)
        } else {
            throw throwable
        }
    }

    fun sendEmail(email: String?, subject: String, message: String): Result<Unit> = runCatching {
        val value = email?.takeIf { it.isNotBlank() } ?: error("No email address for this client")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(value)}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.mapCatching { Unit }.recoverCatching { throwable ->
        if (throwable is ActivityNotFoundException) {
            throw IllegalStateException("No app found to send this message", throwable)
        } else {
            throw throwable
        }
    }

    fun copyToClipboard(message: String): Result<Unit> = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ServiceSphere message", message))
    }
}

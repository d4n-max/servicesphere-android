package com.servicesphere.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.servicesphere.data.local.AppDatabase
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobPhotoEntity
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.local.SignatureEntity
import com.servicesphere.data.preferences.UserPreferences
import com.servicesphere.reminders.JobReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DataExportManager(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val reminderScheduler: JobReminderScheduler,
    private val fileCleanupManager: LocalFileCleanupManager = LocalFileCleanupManager(context)
) {
    private val exportDirectory: File
        get() = File(context.filesDir, "exports")

    suspend fun exportBackupJson(): ExportResult = exportFile(
        prefix = "servicesphere_backup",
        extension = "json",
        mimeType = "application/json"
    ) {
        val exportData = loadExportData()
        exportData.toJson().toString(2)
    }

    suspend fun exportClientsCsv(): ExportResult = exportFile(
        prefix = "servicesphere_clients",
        extension = "csv",
        mimeType = "text/csv"
    ) {
        val clients = database.clientDao().getAllClientsOnce()
        buildCsv(
            headers = listOf("id", "name", "phone", "email", "address", "notes", "createdAt", "updatedAt"),
            rows = clients.map {
                listOf(it.id, it.name, it.phone, it.email, it.address, it.notes, formatMillis(it.createdAt), formatMillis(it.updatedAt))
            }
        )
    }

    suspend fun exportJobsCsv(): ExportResult = exportFile(
        prefix = "servicesphere_jobs",
        extension = "csv",
        mimeType = "text/csv"
    ) {
        val clientsById = database.clientDao().getAllClientsOnce().associateBy { it.id }
        val jobs = database.jobDao().getAllJobsOnce()
        buildCsv(
            headers = listOf("id", "clientId", "clientName", "title", "description", "address", "scheduledAt", "status", "estimatedPrice", "createdAt", "updatedAt"),
            rows = jobs.map { job ->
                listOf(
                    job.id,
                    job.clientId,
                    job.clientId?.let { clientsById[it]?.name },
                    job.title,
                    job.description,
                    job.address,
                    job.scheduledAt?.let(::formatMillis),
                    job.status,
                    job.estimatedPrice,
                    formatMillis(job.createdAt),
                    formatMillis(job.updatedAt)
                )
            }
        )
    }

    suspend fun exportQuotesCsv(): ExportResult = exportFile(
        prefix = "servicesphere_quotes",
        extension = "csv",
        mimeType = "text/csv"
    ) {
        val clientsById = database.clientDao().getAllClientsOnce().associateBy { it.id }
        val jobsById = database.jobDao().getAllJobsOnce().associateBy { it.id }
        val quotes = database.quoteDao().getAllQuotesOnce()
        buildCsv(
            headers = listOf("id", "quoteNumber", "clientId", "clientName", "jobId", "jobTitle", "status", "issueDate", "validUntil", "subtotal", "discountAmount", "taxAmount", "total", "notes", "createdAt", "updatedAt"),
            rows = quotes.map { quote ->
                listOf(
                    quote.id,
                    quote.quoteNumber,
                    quote.clientId,
                    quote.clientId?.let { clientsById[it]?.name },
                    quote.jobId,
                    quote.jobId?.let { jobsById[it]?.title },
                    quote.status,
                    formatMillis(quote.issueDate),
                    quote.validUntil?.let(::formatMillis),
                    quote.subtotal,
                    quote.discountAmount,
                    quote.taxAmount,
                    quote.total,
                    quote.notes,
                    formatMillis(quote.createdAt),
                    formatMillis(quote.updatedAt)
                )
            }
        )
    }

    suspend fun exportInvoicesCsv(): ExportResult = exportFile(
        prefix = "servicesphere_invoices",
        extension = "csv",
        mimeType = "text/csv"
    ) {
        val clientsById = database.clientDao().getAllClientsOnce().associateBy { it.id }
        val jobsById = database.jobDao().getAllJobsOnce().associateBy { it.id }
        val quotesById = database.quoteDao().getAllQuotesOnce().associateBy { it.id }
        val invoices = database.invoiceDao().getAllInvoicesOnce()
        buildCsv(
            headers = listOf("id", "invoiceNumber", "clientId", "clientName", "jobId", "jobTitle", "quoteId", "quoteNumber", "status", "issueDate", "dueDate", "paidDate", "paymentMethod", "subtotal", "discountAmount", "taxAmount", "total", "notes", "createdAt", "updatedAt"),
            rows = invoices.map { invoice ->
                listOf(
                    invoice.id,
                    invoice.invoiceNumber,
                    invoice.clientId,
                    invoice.clientId?.let { clientsById[it]?.name },
                    invoice.jobId,
                    invoice.jobId?.let { jobsById[it]?.title },
                    invoice.quoteId,
                    invoice.quoteId?.let { quotesById[it]?.quoteNumber },
                    invoice.status,
                    formatMillis(invoice.issueDate),
                    invoice.dueDate?.let(::formatMillis),
                    invoice.paidDate?.let(::formatMillis),
                    invoice.paymentMethod,
                    invoice.subtotal,
                    invoice.discountAmount,
                    invoice.taxAmount,
                    invoice.total,
                    invoice.notes,
                    formatMillis(invoice.createdAt),
                    formatMillis(invoice.updatedAt)
                )
            }
        )
    }

    suspend fun deleteAllLocalData(resetSetup: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            database.jobReminderDao().getAllRemindersOnce().forEach { reminderScheduler.cancel(it.id) }
            reminderScheduler.cancelAllReminders()
            database.withTransaction {
                database.jobReminderDao().deleteAll()
                database.jobPhotoDao().deleteAll()
                database.signatureDao().deleteAll()
                database.lineItemDao().deleteAll()
                database.invoiceDao().deleteAll()
                database.quoteDao().deleteAll()
                database.jobDao().deleteAll()
                database.clientDao().deleteAll()
                database.businessProfileDao().deleteAll()
            }
            if (resetSetup) preferences.resetSetupState()
            fileCleanupManager.deleteAllManagedFiles(includeExports = true)
            true
        }.getOrElse { false }
    }

    fun shareFile(fileUri: android.net.Uri, mimeType: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }.recoverCatching { error ->
        if (error is ActivityNotFoundException) throw IllegalStateException("No app found to share this file") else throw error
    }

    private suspend fun exportFile(
        prefix: String,
        extension: String,
        mimeType: String,
        content: suspend () -> String
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            if (!exportDirectory.exists() && !exportDirectory.mkdirs()) error("Could not create export directory")
            val fileName = "${prefix}_${fileTimestamp()}.$extension"
            val file = File(exportDirectory, fileName)
            file.writeText(content(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ExportResult(success = true, filePath = file.absolutePath, fileUri = uri, fileName = fileName, mimeType = mimeType)
        }.getOrElse {
            ExportResult(success = false, errorMessage = "Couldn't export data")
        }
    }

    private suspend fun loadExportData(): ServiceSphereExportData = ServiceSphereExportData(
        businessProfile = database.businessProfileDao().getBusinessProfileOnce(),
        clients = database.clientDao().getAllClientsOnce(),
        jobs = database.jobDao().getAllJobsOnce(),
        quotes = database.quoteDao().getAllQuotesOnce(),
        invoices = database.invoiceDao().getAllInvoicesOnce(),
        lineItems = database.lineItemDao().getAllLineItemsOnce(),
        jobPhotos = database.jobPhotoDao().getAllPhotosOnce(),
        signatures = database.signatureDao().getAllSignaturesOnce(),
        jobReminders = database.jobReminderDao().getAllRemindersOnce()
    )

    private fun buildCsv(headers: List<String>, rows: List<List<Any?>>): String = buildString {
        appendLine(headers.joinToString(",") { escapeCsv(it) })
        rows.forEach { row ->
            appendLine(row.joinToString(",") { escapeCsv(it?.toString().orEmpty()) })
        }
    }

    private fun escapeCsv(value: String): String {
        val needsEscaping = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsEscaping) "\"$escaped\"" else escaped
    }

    private fun fileTimestamp(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now())

    private fun formatMillis(millis: Long): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
}

private fun ServiceSphereExportData.toJson(): JSONObject = JSONObject()
    .put("app", app)
    .put("version", version)
    .put("exportedAt", exportedAt)
    .put("businessProfile", businessProfile?.toJson() ?: JSONObject.NULL)
    .put("clients", clients.toJsonArray { it.toJson() })
    .put("jobs", jobs.toJsonArray { it.toJson() })
    .put("quotes", quotes.toJsonArray { it.toJson() })
    .put("invoices", invoices.toJsonArray { it.toJson() })
    .put("lineItems", lineItems.toJsonArray { it.toJson() })
    .put("jobPhotos", jobPhotos.toJsonArray { it.toJson() })
    .put("signatures", signatures.toJsonArray { it.toJson() })
    .put("jobReminders", jobReminders.toJsonArray { it.toJson() })

private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(transform(it)) } }

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject = put(name, value ?: JSONObject.NULL)

private fun BusinessProfileEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("businessName", businessName)
    .putNullable("ownerName", ownerName)
    .putNullable("phone", phone)
    .putNullable("email", email)
    .putNullable("address", address)
    .putNullable("website", website)
    .putNullable("taxNumber", taxNumber)
    .putNullable("logoUri", logoUri)
    .put("currencyCode", currencyCode)
    .put("taxRatePercent", taxRatePercent)
    .putNullable("paymentInstructions", paymentInstructions)
    .put("quotePrefix", quotePrefix)
    .put("invoicePrefix", invoicePrefix)
    .put("nextQuoteNumber", nextQuoteNumber)
    .put("nextInvoiceNumber", nextInvoiceNumber)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun ClientEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .putNullable("phone", phone)
    .putNullable("email", email)
    .putNullable("address", address)
    .putNullable("notes", notes)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun JobEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .putNullable("clientId", clientId)
    .put("title", title)
    .putNullable("description", description)
    .putNullable("address", address)
    .putNullable("scheduledAt", scheduledAt)
    .put("status", status)
    .putNullable("estimatedPrice", estimatedPrice)
    .putNullable("internalNotes", internalNotes)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun QuoteEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .putNullable("clientId", clientId)
    .putNullable("jobId", jobId)
    .put("quoteNumber", quoteNumber)
    .put("status", status)
    .put("issueDate", issueDate)
    .putNullable("validUntil", validUntil)
    .put("subtotal", subtotal)
    .put("discountAmount", discountAmount)
    .put("taxAmount", taxAmount)
    .put("total", total)
    .putNullable("notes", notes)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun InvoiceEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .putNullable("clientId", clientId)
    .putNullable("jobId", jobId)
    .putNullable("quoteId", quoteId)
    .put("invoiceNumber", invoiceNumber)
    .put("status", status)
    .put("issueDate", issueDate)
    .putNullable("dueDate", dueDate)
    .putNullable("paidDate", paidDate)
    .putNullable("paymentMethod", paymentMethod)
    .put("subtotal", subtotal)
    .put("discountAmount", discountAmount)
    .put("taxAmount", taxAmount)
    .put("total", total)
    .putNullable("notes", notes)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun LineItemEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("parentId", parentId)
    .put("parentType", parentType)
    .put("description", description)
    .put("quantity", quantity)
    .put("unitPrice", unitPrice)
    .put("total", total)
    .put("sortOrder", sortOrder)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun JobPhotoEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("jobId", jobId)
    .put("localUri", localUri)
    .putNullable("caption", caption)
    .put("createdAt", createdAt)

private fun SignatureEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .putNullable("jobId", jobId)
    .putNullable("invoiceId", invoiceId)
    .put("localUri", localUri)
    .putNullable("signedBy", signedBy)
    .putNullable("approvalText", approvalText)
    .put("createdAt", createdAt)

private fun JobReminderEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("jobId", jobId)
    .put("reminderType", reminderType)
    .put("reminderTimeMillis", reminderTimeMillis)
    .put("isEnabled", isEnabled)
    .put("hasFired", hasFired)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

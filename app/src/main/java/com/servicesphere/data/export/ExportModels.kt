package com.servicesphere.data.export

import android.net.Uri
import com.servicesphere.data.local.BusinessProfileEntity
import com.servicesphere.data.local.ClientEntity
import com.servicesphere.data.local.InvoiceEntity
import com.servicesphere.data.local.JobEntity
import com.servicesphere.data.local.JobPhotoEntity
import com.servicesphere.data.local.JobReminderEntity
import com.servicesphere.data.local.LineItemEntity
import com.servicesphere.data.local.QuoteEntity
import com.servicesphere.data.local.SignatureEntity

data class ServiceSphereExportData(
    val app: String = "ServiceSphere",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val businessProfile: BusinessProfileEntity?,
    val clients: List<ClientEntity>,
    val jobs: List<JobEntity>,
    val quotes: List<QuoteEntity>,
    val invoices: List<InvoiceEntity>,
    val lineItems: List<LineItemEntity>,
    val jobPhotos: List<JobPhotoEntity>,
    val signatures: List<SignatureEntity>,
    val jobReminders: List<JobReminderEntity>
)

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val errorMessage: String? = null
)

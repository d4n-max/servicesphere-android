package com.servicesphere.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.servicesphere.data.ServiceLocator
import com.servicesphere.appcheck.AppCheckTokenVerifier
import com.servicesphere.data.local.SyncOperationEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import android.net.Uri
import java.io.File
import java.security.MessageDigest

sealed interface SyncStatus {
    data object OfflineOnly : SyncStatus
    data object NotSignedIn : SyncStatus
    data object Disabled : SyncStatus
    data class UpToDate(val lastSyncedAt: Long?) : SyncStatus
    data class WaitingForConnection(val pendingChanges: Int) : SyncStatus
    data object Syncing : SyncStatus
    data object BackupInProgress : SyncStatus
    data object RestoreInProgress : SyncStatus
    data object Paused : SyncStatus
    data object ActionRequired : SyncStatus
    data class Failed(val retrying: Boolean) : SyncStatus
}

class SyncOutbox(private val context: Context) {
    private val dao get() = ServiceLocator.database.syncOperationDao()
    suspend fun enqueue(entityType: String, entityId: String, operation: SyncOperationType, payloadJson: String? = null, dependsOnEntityId: String? = null) {
        dao.upsert(SyncOperationEntity(entityType = entityType, entityId = entityId, operation = operation.name, payloadJson = payloadJson, dependsOnEntityId = dependsOnEntityId))
        SyncWorkScheduler.enqueue(context)
    }
    suspend fun pendingCount() = dao.pendingCount()
}

object SyncWorkScheduler {
    private const val UNIQUE_NAME = "servicesphere-cloud-sync"
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }
    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
}

class CloudSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = ServiceLocator.preferences
        if (!preferences.cloudSyncEnabled.first()) return Result.success()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        AppCheckTokenVerifier.requireToken()
        val operations = ServiceLocator.database.syncOperationDao().ready(System.currentTimeMillis())
        if (operations.isEmpty()) return Result.success()
        return try {
            operations.forEach { uploadOperation(uid, it) }
            preferences.markCloudSyncSuccessful()
            Result.success()
        } catch (error: Exception) {
            // WorkManager retries transient Firebase/network failures; permanent auth/rules failures remain visible in the outbox.
            Result.retry()
        }
    }

    private suspend fun uploadOperation(uid: String, operation: SyncOperationEntity) {
        val businessId = ServiceLocator.businessRepository.getBusinessProfileOnce()?.id ?: "default_business"
        val document = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("businesses").document(businessId).collection(operation.entityType).document(operation.entityId)
        val operationType = SyncOperationType.valueOf(operation.operation)
        val payload = operation.payloadJson?.let { org.json.JSONObject(it).toMap() } ?: emptyMap()
        val attachmentData = if (operationType == SyncOperationType.ATTACHMENT_UPLOAD) uploadAttachment(uid, businessId, operation, payload) else emptyMap()
        val data = payload + attachmentData + mapOf(
            "id" to operation.entityId,
            "updatedAt" to System.currentTimeMillis(),
            "deletedAt" to if (operationType == SyncOperationType.DELETE) System.currentTimeMillis() else null,
            "operationId" to operation.id
        )
        document.set(data).awaitTask()
        ServiceLocator.database.syncOperationDao().deleteForEntity(operation.entityType, operation.entityId)
    }

    private suspend fun uploadAttachment(uid: String, businessId: String, operation: SyncOperationEntity, payload: Map<String, Any?>): Map<String, Any?> {
        val rawUri = payload["localUri"] as? String ?: throw IllegalStateException("Attachment metadata is missing its local URI")
        val uri = Uri.parse(rawUri)
        val localFile = uri.takeIf { it.scheme == "file" }?.path?.let(::File)
        if (localFile == null || !localFile.isFile) throw IllegalStateException("The local attachment is no longer available")
        val checksum = localFile.inputStream().use { stream ->
            MessageDigest.getInstance("SHA-256").digest(stream.readBytes()).joinToString("") { "%02x".format(it) }
        }
        val path = "users/$uid/businesses/$businessId/attachments/${operation.entityType}/${operation.entityId}"
        FirebaseStorage.getInstance().reference.child(path).putFile(uri).awaitTask()
        return mapOf("storagePath" to path, "checksum" to checksum, "sizeBytes" to localFile.length())
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it, onCancellation = {}) }
    addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
}

private fun org.json.JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
    when (val value = opt(key)) {
        org.json.JSONObject.NULL -> null
        is org.json.JSONObject -> value.toMap()
        is org.json.JSONArray -> (0 until value.length()).map { index -> value.opt(index) }
        else -> value
    }
}

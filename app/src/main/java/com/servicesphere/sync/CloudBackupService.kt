package com.servicesphere.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.servicesphere.BuildConfig
import com.servicesphere.data.ServiceLocator
import com.servicesphere.appcheck.AppCheckTokenVerifier
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File

data class CloudBackupSummary(val createdAt: Long, val sizeBytes: Long, val recordCount: Int)

class CloudBackupService {
    suspend fun backUpNow(): Result<CloudBackupSummary> = runCatching {
        val uid = requireNotNull(FirebaseAuth.getInstance().currentUser?.uid) { "Sign in before creating a backup." }
        AppCheckTokenVerifier.requireToken()
        val export = ServiceLocator.dataExportManager.exportBackupJson()
        require(export.success && export.filePath != null) { export.errorMessage ?: "Couldn't create local backup." }
        val source = File(export.filePath)
        val body = source.readText()
        val businessId = ServiceLocator.businessRepository.getBusinessProfileOnce()?.id ?: "default_business"
        val recordCount = JSONObject(body).let { json -> listOf("clients", "jobs", "quotes", "invoices").sumOf { json.optJSONArray(it)?.length() ?: 0 } }
        val createdAt = System.currentTimeMillis()
        val storagePath = "users/$uid/businesses/$businessId/backups/latest.json"
        FirebaseStorage.getInstance().reference.child(storagePath).putFile(android.net.Uri.fromFile(source)).awaitBackup()
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("businesses").document(businessId)
            .collection("backups").document("latest")
            .set(mapOf("createdAt" to createdAt, "sizeBytes" to source.length(), "recordCount" to recordCount, "schemaVersion" to BackupManifest.CURRENT_SCHEMA_VERSION, "appVersion" to BuildConfig.VERSION_NAME, "dataChecksum" to sha256(body), "storagePath" to storagePath)).awaitBackup()
        ServiceLocator.preferences.markCloudBackupSuccessful(source.length(), createdAt)
        CloudBackupSummary(createdAt, source.length(), recordCount)
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitBackup(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it, onCancellation = {}) }
    addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
}

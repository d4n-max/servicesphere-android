package com.servicesphere.sync

import java.security.MessageDigest

enum class SyncOperationType { CREATE, UPDATE, DELETE, ATTACHMENT_UPLOAD, ATTACHMENT_DELETE }
enum class SyncOperationState { PENDING, RUNNING, FAILED_PERMANENTLY }
enum class ConflictDecision { APPLY_REMOTE, KEEP_LOCAL, RECORD_CONFLICT }

data class PendingMutation(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperationType,
    val createdAt: Long,
    val dependsOnEntityId: String? = null
)

object SyncOperationCoalescer {
    /** Preserves ordering between entities while making retries idempotent for an entity. */
    fun coalesce(operations: List<PendingMutation>): List<PendingMutation> = operations
        .groupBy { it.entityType to it.entityId }
        .values
        .map { it.maxBy { mutation -> mutation.createdAt } }
        .sortedWith(compareBy<PendingMutation> { it.dependsOnEntityId != null }.thenBy { it.createdAt })
}

object SyncRetryPolicy {
    private const val MAX_DELAY_MS = 6 * 60 * 60 * 1000L
    fun delayMillis(attempt: Int): Long = (30_000L * (1L shl (attempt.coerceAtLeast(1) - 1).coerceAtMost(10))).coerceAtMost(MAX_DELAY_MS)
}

object ConflictResolver {
    fun resolve(localUpdatedAt: Long, localPending: Boolean, remoteUpdatedAt: Long, remoteDeletedAt: Long?): ConflictDecision = when {
        localPending && remoteUpdatedAt <= localUpdatedAt -> ConflictDecision.KEEP_LOCAL
        !localPending && remoteUpdatedAt > localUpdatedAt -> ConflictDecision.APPLY_REMOTE
        remoteDeletedAt != null && !localPending -> ConflictDecision.APPLY_REMOTE
        localPending && remoteUpdatedAt > localUpdatedAt -> ConflictDecision.RECORD_CONFLICT
        else -> ConflictDecision.KEEP_LOCAL
    }
}

data class BackupAttachmentManifest(val id: String, val relativePath: String, val checksum: String, val sizeBytes: Long)
data class BackupManifest(
    val schemaVersion: Int,
    val businessId: String,
    val createdAt: Long,
    val recordCount: Int,
    val attachments: List<BackupAttachmentManifest>,
    val dataChecksum: String
) {
    fun isValidFor(data: String): Boolean = dataChecksum == sha256(data)
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        fun create(businessId: String, data: String, attachments: List<BackupAttachmentManifest>) = BackupManifest(
            CURRENT_SCHEMA_VERSION, businessId, System.currentTimeMillis(), 0, attachments, sha256(data)
        )
    }
}

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

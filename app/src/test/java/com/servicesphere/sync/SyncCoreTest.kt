package com.servicesphere.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoreTest {
    @Test fun `coalescing keeps one latest mutation per entity`() {
        val result = SyncOperationCoalescer.coalesce(listOf(
            PendingMutation("1", "clients", "c1", SyncOperationType.CREATE, 1),
            PendingMutation("2", "clients", "c1", SyncOperationType.UPDATE, 2),
            PendingMutation("3", "clients", "c1", SyncOperationType.DELETE, 3)
        ))
        assertEquals(1, result.size)
        assertEquals(SyncOperationType.DELETE, result.single().operation)
    }

    @Test fun `retry delay is capped and increases exponentially`() {
        assertEquals(30_000L, SyncRetryPolicy.delayMillis(1))
        assertEquals(60_000L, SyncRetryPolicy.delayMillis(2))
        assertEquals(6 * 60 * 60 * 1000L, SyncRetryPolicy.delayMillis(99))
    }

    @Test fun `remote older record never overwrites pending local mutation`() {
        assertEquals(ConflictDecision.KEEP_LOCAL, ConflictResolver.resolve(
            localUpdatedAt = 200, localPending = true, remoteUpdatedAt = 100, remoteDeletedAt = null
        ))
    }

    @Test fun `newer remote record applies when local has no pending mutation`() {
        assertEquals(ConflictDecision.APPLY_REMOTE, ConflictResolver.resolve(
            localUpdatedAt = 100, localPending = false, remoteUpdatedAt = 200, remoteDeletedAt = null
        ))
    }

    @Test fun `backup manifest checksum detects tampering`() {
        val manifest = BackupManifest.create("business", "{}", emptyList())
        assertTrue(manifest.isValidFor("{}"))
        assertFalse(manifest.isValidFor("changed"))
    }
}

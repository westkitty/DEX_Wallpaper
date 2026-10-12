package com.westkitty.dexwallpaper.core

import org.junit.Assert.*
import org.junit.Test

class BackupMergePolicyTest {
    @Test fun healthyLocalOriginalWinsOverVerifiedBackup() {
        assertEquals(BackupMergePolicy.OriginalAction.KEEP_EXISTING,
            BackupMergePolicy.originalAction(true, false, true, true))
    }
    @Test fun missingLocalOriginalMayBeRepairedFromVerifiedBackup() {
        assertEquals(BackupMergePolicy.OriginalAction.INSTALL_VERIFIED,
            BackupMergePolicy.originalAction(true, false, false, true))
    }
    @Test fun quarantinedOriginalMayBeRepairedFromVerifiedBackup() {
        assertEquals(BackupMergePolicy.OriginalAction.INSTALL_VERIFIED,
            BackupMergePolicy.originalAction(true, true, true, true))
    }
    @Test fun metadataOnlyRestoreKeepsQuarantine() {
        assertEquals(BackupMergePolicy.OriginalAction.QUARANTINE,
            BackupMergePolicy.originalAction(true, true, true, false))
    }
    @Test fun newOriginalIsOnlyReadyWhenVerifiedBytesExist() {
        assertEquals(BackupMergePolicy.OriginalAction.INSTALL_VERIFIED,
            BackupMergePolicy.originalAction(false, false, false, true))
        assertEquals(BackupMergePolicy.OriginalAction.QUARANTINE,
            BackupMergePolicy.originalAction(false, false, false, false))
    }
    @Test fun localIdentityRemainsUnchangedOnCollision() {
        val reserved = mutableSetOf("scene", "other")
        assertEquals("new-scene", BackupMergePolicy.distinctImportedId("scene", reserved) { "new-scene" })
        assertEquals(setOf("scene", "other", "new-scene"), reserved)
    }
    @Test fun importedIdentityCanBeReusedWhenUnoccupied() {
        val reserved = mutableSetOf("scene")
        assertEquals("fresh", BackupMergePolicy.distinctImportedId("fresh", reserved) { error("Unexpected generator") })
        assertTrue("fresh" in reserved)
    }
    @Test fun generatedIdentityCollisionsAreRetried() {
        val reserved = mutableSetOf("scene", "taken")
        val ids = listOf("taken", "scene", "good").iterator()
        assertEquals("good", BackupMergePolicy.distinctImportedId("scene", reserved) { ids.next() })
    }
    @Test fun exhaustedIdentityAllocationFailsClosed() {
        val reserved = mutableSetOf("rule")
        assertTrue(runCatching { BackupMergePolicy.distinctImportedId("rule", reserved) { "rule" } }.isFailure)
    }
}

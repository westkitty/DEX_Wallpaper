package com.westkitty.dexwallpaper.core
import com.westkitty.dexwallpaper.data.*
import org.junit.Assert.*
import org.junit.Test
class BackupCodecTest {
    private val asset=AssetEntity("a","Private artwork","a.img","content://private/uri","0".repeat(64),100,100,1000,0,favorite=true,tags="work",license="User supplied")
    private val snapshot=BackupSnapshot(listOf(asset),listOf(CollectionEntity("c","Art","content://private/folder",pinned=true)),listOf(Membership("c","a")),listOf(EditEntity("a",zoom=2f)),
        listOf(ScreenEntity("HOME","c",enabled=true,pending="a",recent="a")),emptyList(),emptyList(),WorkspacePreferences(highContrast=true))
    @Test fun roundTripPreservesMetadataButNotDeviceGrants() {
        val encoded=BackupCodec.encode(snapshot); assertFalse(encoded.contains("content://")); assertFalse(encoded.contains("relativePath"))
        val restored=BackupCodec.decode(encoded); assertTrue(restored.assets.single().favorite); assertEquals("work",restored.assets.single().tags)
        assertEquals(2f,restored.edits.single().zoom,0f); assertTrue(restored.preferences.highContrast); assertEquals("",restored.collections.single().sourceTree); assertTrue(restored.collections.single().pinned)
    }
    @Test fun restoredSchedulesAlwaysPaused() { val s=BackupCodec.decode(BackupCodec.encode(snapshot)).screens.single(); assertFalse(s.enabled); assertTrue(s.paused); assertEquals("a",s.pending) }
    @Test fun unknownVersionAndPathTraversalRejected() {
        assertTrue(runCatching { BackupCodec.decode(BackupCodec.encode(snapshot).replace("\"version\":1","\"version\":999")) }.isFailure)
        assertTrue(runCatching { BackupCodec.decode(BackupCodec.encode(snapshot).replace("\"id\":\"a\"","\"id\":\"../secret\"")) }.isFailure)
    }
    @Test fun danglingMembershipRejected() { assertTrue(runCatching { BackupCodec.decode(BackupCodec.encode(snapshot.copy(memberships=listOf(Membership("c","missing"))))) }.isFailure) }
}

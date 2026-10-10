package com.westkitty.dexwallpaper.core
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import org.junit.Assert.*
import org.junit.Test
class LiveIsolationTest {
    @Test fun independentLiveSourcesNeverReadStaticHomeRecord() {
        val home=LiveConfigCodec.encode(LiveConfiguration("home-live",ImageEdit(),WorkspacePreferences()))
        val lock=LiveConfigCodec.encode(LiveConfiguration("lock-live",ImageEdit(dim=.4f),WorkspacePreferences()))
        val source=LiveDestinationPolicy.source(2,home,lock,"")
        val staticHomeCurrent="unrelated-new-static-home"
        assertEquals("lock-live",(LiveConfigCodec.decode(source) as Outcome.Ok).value.assetId)
        assertFalse(source.contains(staticHomeCurrent))
    }
    @Test fun sharedAndUnknownOwnersRefuseHomeOnlyUpdates() {
        assertFalse(LiveDestinationPolicy.mayUpdate(Destination.HOME,listOf(3)))
        assertFalse(LiveDestinationPolicy.mayUpdate(Destination.HOME,listOf(0)))
        assertFalse(LiveDestinationPolicy.mayUpdate(Destination.HOME,listOf(1,3)))
        assertFalse(LiveDestinationPolicy.mayUpdate(Destination.HOME,listOf(1,8)))
        assertTrue(LiveDestinationPolicy.mayUpdate(Destination.HOME,listOf(1,2)))
    }
    @Test fun bothRequiresExplicitTargetAndInstalledEngine() { assertTrue(LiveDestinationPolicy.mayUpdate(Destination.BOTH,listOf(0)));assertFalse(LiveDestinationPolicy.mayUpdate(Destination.BOTH,emptyList())) }
    @Test fun editSnapshotIsImmutableWhenLibraryEditChanges() {
        val original=ImageEdit(dim=.3f);val encoded=LiveConfigCodec.encode(LiveConfiguration("image",original,WorkspacePreferences(shaderEffect="GLASS")))
        val libraryLater=original.copy(dim=.8f)
        assertEquals(.3f,(LiveConfigCodec.decode(encoded) as Outcome.Ok).value.edit.dim,0f);assertNotEquals(libraryLater,original)
    }
    @Test fun malformedAndExecutableSnapshotsFail() { assertTrue(LiveConfigCodec.decode(" ".repeat(16385)) is Outcome.Error);assertTrue(LiveConfigCodec.decode("{\"version\":9}") is Outcome.Error) }
}

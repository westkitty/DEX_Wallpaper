package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test

class DestinationPolicyTest {
    @Test fun defaultIsHomeOnly() { assertEquals(Destination.HOME, Destination.parse("unknown")) }
    @Test fun homeOnlyNeverRequestsLockFlag() { assertEquals(1, (DestinationPolicy.check(Destination.HOME, WallpaperSnapshot(11,22)) as Outcome.Ok).value) }
    @Test fun lockOnlyNeverRequestsHomeFlag() { assertEquals(2, (DestinationPolicy.check(Destination.LOCK, WallpaperSnapshot(11,22)) as Outcome.Ok).value) }
    @Test fun bothRequiresExplicitDestination() { assertEquals(3, (DestinationPolicy.check(Destination.BOTH, WallpaperSnapshot(11,-1)) as Outcome.Ok).value) }
    @Test fun sharedLockFailsBeforeWrite() { assertTrue(DestinationPolicy.check(Destination.HOME, WallpaperSnapshot(11,-1)) is Outcome.Error); assertTrue(DestinationPolicy.check(Destination.HOME, WallpaperSnapshot(11,0)) is Outcome.Error) }
    @Test fun identicalScreenIdentityFailsClosed() { assertTrue(DestinationPolicy.check(Destination.HOME,WallpaperSnapshot(11,11)) is Outcome.Error) }
    @Test fun devicePolicyFailsClosed() { assertTrue(DestinationPolicy.check(Destination.HOME, WallpaperSnapshot(11,22, allowed = false)) is Outcome.Error) }
    @Test fun oemLockMutationStopsRotation() { val error = DestinationPolicy.verify(Destination.HOME, WallpaperSnapshot(11,22), WallpaperSnapshot(12,23)) as Outcome.Error; assertFalse(error.recoverable) }
    @Test fun homeOnlyVerifiesLockIdentity() { assertTrue(DestinationPolicy.verify(Destination.HOME, WallpaperSnapshot(11,22), WallpaperSnapshot(12,22)) is Outcome.Ok) }
    @Test fun lockOnlyVerifiesHomeIdentity() { assertTrue(DestinationPolicy.verify(Destination.LOCK, WallpaperSnapshot(11,22), WallpaperSnapshot(12,23)) is Outcome.Error) }
}

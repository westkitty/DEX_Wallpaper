package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class ProfileCodecTest {
    @Test fun presetRoundTrips() { DeskShift.presets.forEach { assertEquals(it,(ProfileCodec.decode(ProfileCodec.encode(it)) as Outcome.Ok).value) } }
    @Test fun profilesNeverContainDeviceDestinationOrCredentials() { val s=ProfileCodec.encode(DeskShift.presets.first()); assertFalse(s.contains("destination")); assertFalse(s.contains("token")); assertFalse(s.contains("uri")) }
    @Test fun unboundedInputAndVersionsRejected() { assertTrue(ProfileCodec.decode(" ".repeat(16385)) is Outcome.Error); assertTrue(ProfileCodec.decode(ProfileCodec.encode(DeskShift.presets.first()).replace("\"version\": 1","\"version\": 9")) is Outcome.Error) }
    @Test fun unsafeIdentityAndEffectsRejected() { assertTrue(runCatching { ProfileCodec.encode(VisualProfile("Bad",collectionId="../../private")) }.isFailure); assertTrue(runCatching { ProfileCodec.encode(VisualProfile("Bad",dim=Float.NaN)) }.isFailure) }
}

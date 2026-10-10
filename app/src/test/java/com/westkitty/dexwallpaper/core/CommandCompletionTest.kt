package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class CommandCompletionTest {
    @Test fun deniedWallpaperWriteIsACommandFailureNotSuccess() { val r=CommandCompletionPolicy.from(Outcome.Error("Home-only safety refused"));assertFalse(r.success);assertEquals("Home-only safety refused",r.message) }
    @Test fun successfulManualActionPreservesActualReceipt() { val r=CommandCompletionPolicy.from(Outcome.Ok("Applied selected image to Home"));assertTrue(r.success);assertEquals("Applied selected image to Home",r.message) }
    @Test fun nonWallpaperCommandsHaveBoundedRealOutcome() { assertTrue(CommandCompletionPolicy.from(Outcome.Ok(Unit)).success);assertEquals(220,CommandCompletionPolicy.from(Outcome.Error("x".repeat(1000))).message.length) }
}

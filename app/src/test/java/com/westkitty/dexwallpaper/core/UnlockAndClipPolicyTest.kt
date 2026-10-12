package com.westkitty.dexwallpaper.core

import com.westkitty.dexwallpaper.worker.UnlockRotationPolicy
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import org.junit.Assert.*
import org.junit.Test

class UnlockAndClipPolicyTest {
    @Test fun bothUnlockTogglesDefaultOff() {
        val p = WorkspacePreferences()
        assertFalse(p.rotateOnUnlockHome)
        assertFalse(p.rotateOnUnlockLock)
        assertTrue(UnlockRotationPolicy.targets(p.rotateOnUnlockHome,p.rotateOnUnlockLock).isEmpty())
    }
    @Test fun homeAndLockAreIndependentAndNeverImplicitlyBoth() {
        assertEquals(listOf(Destination.HOME),UnlockRotationPolicy.targets(true,false))
        assertEquals(listOf(Destination.LOCK),UnlockRotationPolicy.targets(false,true))
        assertEquals(listOf(Destination.HOME,Destination.LOCK),UnlockRotationPolicy.targets(true,true))
        assertFalse(UnlockRotationPolicy.targets(true,true).contains(Destination.BOTH))
    }
    @Test fun clipMimeAndExtensionsAreConstrained() {
        assertEquals("mp4",VideoClips.suffixFor("video/mp4",null))
        assertEquals("webm",VideoClips.suffixFor("video/webm",null))
        assertEquals("mp4",VideoClips.suffixFor(null,"holiday.MP4"))
        assertEquals(null,VideoClips.suffixFor("image/png","phony.mp4"))
        assertEquals(null,VideoClips.suffixFor("video/quicktime","test.mov"))
        assertEquals(null,VideoClips.suffixFor("image/gif","test.gif"))
    }
}

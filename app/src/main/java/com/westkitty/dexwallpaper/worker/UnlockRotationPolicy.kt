package com.westkitty.dexwallpaper.worker

import com.westkitty.dexwallpaper.core.Destination

/** Explicit independent opt-ins. BOTH is intentionally never produced. */
object UnlockRotationPolicy {
    fun targets(home: Boolean, lock: Boolean): List<Destination> =
        buildList {
            if (home) add(Destination.HOME)
            if (lock) add(Destination.LOCK)
        }
}

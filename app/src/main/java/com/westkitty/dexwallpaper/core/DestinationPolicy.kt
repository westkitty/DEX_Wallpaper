package com.westkitty.dexwallpaper.core

/** Fail closed when Home-only preservation cannot be verified. OEM behavior still requires device QA. */
object DestinationPolicy {
    fun check(destination: Destination, snapshot: WallpaperSnapshot): Outcome<Int> = when {
        !snapshot.supported -> Outcome.Error("This device does not support wallpaper changes.", false)
        !snapshot.allowed -> Outcome.Error("Wallpaper changes are disabled by device policy.", false)
        destination == Destination.HOME && snapshot.lockId <= 0 -> Outcome.Error(
            "Home-only safety: Android reports a shared or unverifiable Lock wallpaper. Set a separate static Lock wallpaper in system settings first; DEX will not change both screens.")
        destination == Destination.LOCK && snapshot.homeId <= 0 -> Outcome.Error("The Home wallpaper identity cannot be verified. Lock-only application was not attempted.")
        else -> Outcome.Ok(destination.flags)
    }

    fun verify(destination: Destination, before: WallpaperSnapshot, after: WallpaperSnapshot): Outcome<Unit> = when {
        destination == Destination.HOME && before.lockId != after.lockId -> Outcome.Error("OEM safety check failed: Lock wallpaper identity changed. Rotation has been stopped.", false)
        destination == Destination.LOCK && before.homeId != after.homeId -> Outcome.Error("OEM safety check failed: Home wallpaper identity changed. Rotation has been stopped.", false)
        else -> Outcome.Ok(Unit)
    }
}

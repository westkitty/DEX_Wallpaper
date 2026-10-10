package com.westkitty.dexwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.westkitty.dexwallpaper.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface WallpaperPlatform {
    fun snapshot(): WallpaperSnapshot
    fun write(bitmap: Bitmap, flags: Int)
}
class AndroidWallpaperPlatform(context: Context) : WallpaperPlatform {
    private val manager = WallpaperManager.getInstance(context)
    override fun snapshot() = WallpaperSnapshot(manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM), manager.getWallpaperId(WallpaperManager.FLAG_LOCK), manager.isWallpaperSupported, manager.isSetWallpaperAllowed)
    override fun write(bitmap: Bitmap, flags: Int) { manager.setBitmap(bitmap, null, false, flags) }
}
class WallpaperDestinationController(private val platform: WallpaperPlatform) {
    suspend fun apply(bitmap: Bitmap, destination: Destination): Outcome<Unit> = withContext(Dispatchers.IO) {
        try {
            val before = platform.snapshot()
            when (val check = DestinationPolicy.check(destination, before)) {
                is Outcome.Error -> check
                is Outcome.Ok -> {
                    platform.write(bitmap, check.value)
                    DestinationPolicy.verify(destination, before, platform.snapshot())
                }
            }
        } catch (_: SecurityException) { Outcome.Error("Android denied wallpaper access. Check device policy and system wallpaper settings.") }
          catch (e: java.io.IOException) { Outcome.Error("Wallpaper could not be written: ${e.message}") }
          catch (_: IllegalArgumentException) { Outcome.Error("The platform rejected the requested wallpaper destination.") }
    }
}

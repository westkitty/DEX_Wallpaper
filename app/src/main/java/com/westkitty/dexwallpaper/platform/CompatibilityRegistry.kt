package com.westkitty.dexwallpaper.platform

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import com.westkitty.dexwallpaper.core.*

class CompatibilityRegistry(private val context: Context) {
    fun inspect(): List<Capability> {
        val pm = context.packageManager
        val wm = WallpaperManager.getInstance(context)
        val gl = context.getSystemService(ActivityManager::class.java).deviceConfigurationInfo.reqGlEsVersion
        val screens = context.getSystemService(DisplayManager::class.java).displays.size
        return listOf(
            Capability("Static Home / Lock", if (wm.isWallpaperSupported && wm.isSetWallpaperAllowed) CapabilityState.SUPPORTED_WITH_LIMITATIONS else CapabilityState.UNSUPPORTED,
                "Separate explicit flags. Home-only fails safely when a separate Lock wallpaper cannot be verified; OEM confirmation is still required."),
            Capability("Live wallpaper", if (pm.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)) CapabilityState.SUPPORTED_WITH_SETUP else CapabilityState.UNSUPPORTED,
                "Use Android's picker. Android/OEM decides the final destination; independent simultaneous live services are not assumed."),
            Capability("OpenGL ES", if (gl >= 0x20000) CapabilityState.SUPPORTED_WITH_LIMITATIONS else CapabilityState.UNSUPPORTED,
                "Device reports ES ${gl shr 16}.${gl and 65535}. This foundation uses a Canvas renderer; GPU effects are not active."),
            Capability("Vulkan renderer", CapabilityState.UNSUPPORTED, "No Vulkan backend in this build. Static Canvas is the fallback."),
            Capability("Dynamic wallpaper colors", CapabilityState.SUPPORTED, "Android 8+ color callbacks; Material You requires Android 12+."),
            Capability("External displays / DeX", CapabilityState.SUPPORTED_WITH_LIMITATIONS, "$screens display(s) reported. Resizable app windows are supported; external wallpaper ownership is OEM-controlled."),
            Capability("Exact scheduling", CapabilityState.UNSUPPORTED, "No exact-alarm permission. Background rotation is approximate, minimum 15 minutes, and deferred while screen-off or in Battery Saver."),
            Capability("Subject segmentation", CapabilityState.UNSUPPORTED, "No bundled or downloaded detection model. Manual focal points remain available."),
            Capability("Health Connect", CapabilityState.UNSUPPORTED, "No health access is requested or simulated."),
            Capability("Background location", CapabilityState.UNSUPPORTED, "No location permission. Time, charging, battery, and theme rules work offline."),
            Capability("Media / calendar", CapabilityState.UNSUPPORTED, "Notification listener and calendar access are not implemented; no personal data is requested."),
            Capability("WebDAV / Nextcloud", CapabilityState.UNSUPPORTED, "Remote transport is not connected in this foundation; authorized local and Syncthing folders work without a server."),
            Capability("Wallpaper Engine packages", CapabilityState.UNSUPPORTED, "Proprietary Windows binaries and scripts are not executed. No untrusted libraries are loaded."),
            Capability("SVG / video / GIF animation", CapabilityState.UNSUPPORTED, "Static formats accepted by Android BitmapFactory are supported. Animated files are imported as their first frame only.")
        )
    }
}

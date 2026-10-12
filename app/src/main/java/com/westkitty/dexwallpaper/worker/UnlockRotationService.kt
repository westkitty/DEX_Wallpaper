package com.westkitty.dexwallpaper.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.MainActivity
import com.westkitty.dexwallpaper.R
import com.westkitty.dexwallpaper.core.Destination
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A persistent, user-enabled receiver is required: Android 8+ does not deliver
 * ACTION_USER_PRESENT to an ordinary manifest receiver while the app is idle. */
object UnlockRotationMonitor {
    fun reconcile(context: Context, settings: WorkspacePreferences) {
        val intent = Intent(context, UnlockRotationService::class.java)
        if (settings.rotateOnUnlockHome || settings.rotateOnUnlockLock) {
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }
}

class UnlockRotationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channelId = "dex_unlock_rotation"
    private var registered = false
    private var lastUnlockMs = 0L
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastUnlockMs < 1500L) return
            lastUnlockMs = now
            scope.launch { rotateEnabledScreens() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(channelId,
            "DEX unlock wallpaper rotation", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dex)
            .setContentTitle("DEX unlock rotation enabled")
            .setContentText("Home/Lock change only where separately enabled.")
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1008,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1008,notification)
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver,filter)
        registered = true
        scope.launch {
            val prefs = (application as DexApplication).graph.preferences.flow.first()
            if (!prefs.rotateOnUnlockHome && !prefs.rotateOnUnlockLock) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun rotateEnabledScreens() {
        val graph = (application as DexApplication).graph
        val prefs = graph.preferences.flow.first()
        if (!prefs.rotateOnUnlockHome && !prefs.rotateOnUnlockLock) { stopSelf(); return }
        val power = getSystemService(PowerManager::class.java)
        if (power.isPowerSaveMode || !power.isInteractive) return
        graph.repository.initialize()
        val manager = WallpaperManager.getInstance(this)
        val targets = UnlockRotationPolicy.targets(prefs.rotateOnUnlockHome,prefs.rotateOnUnlockLock)
        for (target in targets) {
            try {
                // A static bitmap write could displace an existing live wallpaper:
                // on supported APIs, refuse that target instead of silently replacing it.
                if (Build.VERSION.SDK_INT >= 34) {
                    val flag = if (target == Destination.HOME) WallpaperManager.FLAG_SYSTEM else WallpaperManager.FLAG_LOCK
                    if (manager.getWallpaperInfo(flag) != null) continue
                } else if (target == Destination.HOME && manager.wallpaperInfo != null) continue
                else if (Build.VERSION.SDK_INT < 34 && target == Destination.LOCK) continue
                graph.repository.next(target,automatic=true,onUnlock=true)
            } catch (_: RuntimeException) {
                // The next unlock can retry; do not crash the foreground service.
            }
        }
    }
    override fun onDestroy() {
        if (registered) { unregisterReceiver(receiver); registered=false }
        scope.cancel()
        super.onDestroy()
    }
}

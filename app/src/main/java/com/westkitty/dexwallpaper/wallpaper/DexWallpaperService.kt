package com.westkitty.dexwallpaper.wallpaper

import android.app.WallpaperColors
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.core.ImageEdit
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Each preview/installed engine owns its thread, surface, bitmap, and collectors. No hidden frame loop. */
class DexWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = LivingEngine()
    inner class LivingEngine : Engine() {
        private val graph get() = (application as DexApplication).graph
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val thread = HandlerThread("DEX-wallpaper").apply { start() }
        private val handler = Handler(thread.looper)
        @Volatile private var visible = false
        @Volatile private var surfaceReady = false
        @Volatile private var destroyed = false
        @Volatile private var width = 1
        @Volatile private var height = 1
        private var bitmap: Bitmap? = null // Only accessed by the engine thread.
        private var colors: WallpaperColors? = null
        private var prefs = WorkspacePreferences()
        private var loading: Job? = null
        private var sourceId: String? = null
        private val frame = object : Runnable {
            override fun run() {
                if (!visible || !surfaceReady || destroyed) return
                drawFrame()
                if (prefs.clockOverlay) handler.postDelayed(this, 60_000 - System.currentTimeMillis() % 60_000)
            }
        }
        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            scope.launch {
                combine(graph.repository.dao.observeScreens(), graph.preferences.flow) { screens, settings -> screens.firstOrNull { it.destination == "HOME" }?.currentAssetId to settings }
                    .collect { (id, settings) ->
                        handler.post { prefs = settings; sourceId = id; reload() }
                    }
            }
        }
        private fun reload() {
            loading?.cancel()
            val id = sourceId ?: run { handler.removeCallbacks(frame); handler.post(frame); return }
            if (!surfaceReady || !visible) return
            val w = width.coerceIn(1, 4096); val h = height.coerceIn(1, 4096)
            loading = scope.launch {
                val asset = graph.repository.dao.asset(id) ?: return@launch
                val edit = graph.repository.dao.edit(id)?.model() ?: ImageEdit()
                val result = try { graph.pipeline.render(graph.importer.file(asset), w, h, edit, prefs.safeMode) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { null }
                if (!isActive || destroyed) { result?.recycle(); return@launch }
                handler.post {
                    if (destroyed) result?.recycle()
                    else {
                        bitmap?.recycle(); bitmap = result
                        colors = result?.let { WallpaperColors.fromBitmap(it) }
                        notifyColorsChanged()
                        handler.removeCallbacks(frame); handler.post(frame)
                    }
                }
            }
        }
        private fun drawFrame() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                canvas.drawColor(Color.rgb(15,15,18))
                bitmap?.let { canvas.drawBitmap(it, null, Rect(0,0,canvas.width,canvas.height), Paint(Paint.FILTER_BITMAP_FLAG)) }
                if (prefs.clockOverlay) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = canvas.width * .065f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setShadowLayer(6f,0f,2f,Color.BLACK) }
                    canvas.drawText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), canvas.width * .08f, canvas.height * .85f, paint)
                }
            } catch (_: IllegalArgumentException) { /* Surface was removed between availability and lock. */ }
              catch (_: IllegalStateException) { /* Window manager owns the surface lifecycle. */ }
            finally { canvas?.let { try { surfaceHolder.unlockCanvasAndPost(it) } catch (_: IllegalArgumentException) {} } }
        }
        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.post { handler.removeCallbacks(frame); if (isVisible) reload() else { loading?.cancel(); bitmap?.recycle(); bitmap = null } }
        }
        override fun onSurfaceCreated(holder: SurfaceHolder) { super.onSurfaceCreated(holder); surfaceReady = true; handler.post { reload() } }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h); width = w; height = h; surfaceReady = true; handler.post { reload() }
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { surfaceReady = false; handler.post { handler.removeCallbacks(frame); loading?.cancel(); bitmap?.recycle(); bitmap = null }; super.onSurfaceDestroyed(holder) }
        override fun onComputeColors(): WallpaperColors? = colors
        override fun onDestroy() {
            destroyed = true; visible = false; surfaceReady = false; scope.cancel()
            handler.post { handler.removeCallbacksAndMessages(null); bitmap?.recycle(); bitmap = null; thread.quitSafely() }
            super.onDestroy()
        }
    }
}

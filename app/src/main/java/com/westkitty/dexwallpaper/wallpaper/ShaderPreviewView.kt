package com.westkitty.dexwallpaper.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.westkitty.dexwallpaper.AppGraph
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import kotlinx.coroutines.*

/** A real GLES effect preview, not an animated screenshot or website. No idle frame loop. */
class ShaderPreviewView(context: Context,private val graph: AppGraph): SurfaceView(context),SurfaceHolder.Callback {
    private val thread=HandlerThread("DEX-effect-preview").apply { start() }
    private val handler=Handler(thread.looper)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    var onUnavailable: (String) -> Unit = {}
    private fun unavailable(message: String) { RenderTelemetry.lastError=message; Handler(android.os.Looper.getMainLooper()).post { if(!released) onUnavailable(message) } }
    private var gl: GlWallpaperRenderer?=null
    private var bitmap: Bitmap?=null
    private var assetId: String?=null
    private var prefs=WorkspacePreferences()
    private var load: Job?=null
    @Volatile private var ready=false
    @Volatile private var released=false
    init { holder.addCallback(this) }
    fun configure(id: String?,settings: WorkspacePreferences) {
        handler.post {
            if(released) return@post
            val changed=assetId!=id || prefs.safeMode!=settings.safeMode
            assetId=id; prefs=settings
            if(changed || bitmap==null) reload() else draw()
        }
    }
    override fun surfaceCreated(holder: SurfaceHolder) { ready=true }
    override fun surfaceChanged(holder: SurfaceHolder,format: Int,width: Int,height: Int) {
        ready=true
        handler.post {
            if(released) return@post
            gl?.close(); gl=null
            try { gl=GlWallpaperRenderer().also { it.create(holder.surface,width,height) }; reload() }
            catch(_: Exception) { gl?.close(); gl=null; unavailable("Shader preview unavailable on this surface") }
        }
    }
    private fun reload() {
        load?.cancel()
        val id=assetId ?: return
        if(!ready || released) return
        load=scope.launch {
            val asset=graph.repository.dao.asset(id) ?: return@launch
            val result=try { graph.pipeline.render(graph.importer.file(asset),1000,625,graph.repository.dao.edit(id)?.model() ?: ImageEdit(),prefs.safeMode) }
                catch(e: CancellationException) { throw e }
                catch(_: Exception) { null }
            if(!isActive || released) { result?.recycle(); return@launch }
            handler.post {
                if(released || !ready) result?.recycle()
                else {
                    bitmap?.recycle(); bitmap=result
                    result?.let { try { gl?.upload(it,false); draw() } catch(_: Exception) { unavailable("Shader preview upload failed") } }
                }
            }
        }
    }
    private fun draw() {
        if(!ready || released) return
        try { gl?.draw(if(prefs.safeMode) ShaderEffect.NONE else ShaderEffect.entries.firstOrNull { it.name==prefs.shaderEffect } ?: ShaderEffect.NONE,if(prefs.safeMode) 0f else prefs.effectIntensity) }
        catch(_: Exception) { gl?.close(); gl=null; unavailable("Shader preview context lost") }
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        ready=false; load?.cancel()
        handler.post { gl?.close(); gl=null; bitmap?.recycle(); bitmap=null }
    }
    fun release() {
        released=true; ready=false; scope.cancel(); holder.removeCallback(this)
        handler.post { handler.removeCallbacksAndMessages(null); gl?.close(); gl=null; bitmap?.recycle(); bitmap=null; thread.quitSafely() }
    }
}

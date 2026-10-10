package com.westkitty.dexwallpaper.wallpaper

import android.app.ActivityManager
import android.app.WallpaperColors
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.westkitty.dexwallpaper.DexApplication
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.WorkspacePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Installed and preview engines own separate contexts/threads/jobs. One EGL display is reference-counted. */
class DexWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = LivingEngine()
    inner class LivingEngine : Engine(), SensorEventListener {
        private val graph get() = (application as DexApplication).graph
        private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        private val thread=HandlerThread("DEX-wallpaper").apply { start() }
        private val handler=Handler(thread.looper)
        private val power get()=getSystemService(PowerManager::class.java)
        private val sensors get()=getSystemService(SensorManager::class.java)
        @Volatile private var visible=false
        @Volatile private var surfaceReady=false
        @Volatile private var destroyed=false
        @Volatile private var width=1
        @Volatile private var height=1
        private var bitmap: Bitmap?=null // Engine thread only.
        @Volatile private var colors: WallpaperColors?=null
        private var prefs=WorkspacePreferences()
        private var gl: GlWallpaperRenderer?=null
        private var loading: Job?=null
        private var loadGeneration=0L
        private var sourceId: String?=null
        private var sourceRevision=""
        private var sourceEdit=ImageEdit()
        private var scopeFlags=0
        private val ownershipEvents=Channel<Int>(Channel.CONFLATED)
        private val engineId=java.util.UUID.randomUUID().toString()
        private var lastWorkspace=WorkspacePreferences()
        private var transitionStart=0L
        private var lastDrawAt=0L
        private var offsetX=0f
        private var offsetY=0f
        private var tiltX=0f
        private var tiltY=0f
        private var sensorRegistered=false
        private var recoveryAttempted=false
        private val frame=object: Runnable {
            override fun run() {
                if(!visible || !surfaceReady || destroyed) return
                val transitioning=transitionStart>0 && android.os.SystemClock.uptimeMillis()-transitionStart<prefs.transitionMs
                val continuous=gl!=null && (transitioning || (prefs.zoomAnimation && !prefs.reducedMotion))
                val animated=continuous || (gl!=null && (prefs.parallax || prefs.gyroParallax))
                val thermal=if(Build.VERSION.SDK_INT>=29) power.currentThermalStatus else 0
                val quality=AdaptiveQualityPolicy.evaluate(QualityInput(visible,animated,prefs.frameRate,power.isPowerSaveMode,thermal,
                    getSystemService(ActivityManager::class.java).isLowRamDevice,prefs.reducedMotion,prefs.safeMode,true))
                drawFrame(quality)
                if(continuous && quality.motion && quality.framesPerSecond>0) handler.postDelayed(this,1000L/quality.framesPerSecond)
                else if(prefs.clockOverlay || prefs.batteryOverlay) handler.postDelayed(this,60_000-System.currentTimeMillis()%60_000)
            }
        }
        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            RenderTelemetry.engines.incrementAndGet()
            setTouchEventsEnabled(true)
            scopeFlags=if(Build.VERSION.SDK_INT>=34) wallpaperFlags else 0
            if(!isPreview) {
                LiveEngineRegistry.report(engineId,scopeFlags)
                scope.launch(handler.asCoroutineDispatcher()) {
                    for(which in ownershipEvents) {
                        if(destroyed) break
                        if(which==scopeFlags) continue
                        val previous=scopeFlags
                        val workspace=graph.preferences.flow.first()
                        val stable=sourceRevision.ifBlank { LiveDestinationPolicy.source(previous,workspace.liveHome,workspace.liveLock,workspace.liveShared) }
                        LiveEngineRegistry.report(engineId,0) // No Home/Lock-only commits while reassignment is in flight.
                        if(previous==3 && which in 1..2 && LiveConfigCodec.decode(stable) is Outcome.Ok) {
                            graph.preferences.preserveLiveScope(if(which==1) Destination.HOME else Destination.LOCK,stable)
                        }
                        scopeFlags=which
                        consume(graph.preferences.flow.first())
                        LiveEngineRegistry.report(engineId,which)
                    }
                }
            }
            scope.launch {
                graph.preferences.flow.collect { settings -> handler.post { if(!destroyed) consume(settings) } }
            }
            scope.launch {
                graph.repository.dao.observeAssets().collect { assets -> handler.post {
                    if(!destroyed && visible && bitmap==null && assets.any { it.id==sourceId && !it.quarantined }) reload()
                } }
            }
        }
        private fun consume(workspace: WorkspacePreferences) {
            if(workspace.revision<lastWorkspace.revision) return // Reject delayed pre-reassignment emissions.
            lastWorkspace=workspace
            val encoded=if(isPreview) workspace.livePreview else LiveDestinationPolicy.source(scopeFlags,workspace.liveHome,workspace.liveLock,workspace.liveShared)
            val config=(LiveConfigCodec.decode(encoded) as? Outcome.Ok)?.value
            // Safe/reduced-motion are explicitly system-wide accessibility policies. Image/effect edits are snapshots.
            val effective=(config?.settings ?: WorkspacePreferences()).copy(safeMode=workspace.safeMode,reducedMotion=workspace.reducedMotion || config?.settings?.reducedMotion==true)
            val changed=encoded!=sourceRevision || effective.safeMode!=prefs.safeMode || effective.reducedMotion!=prefs.reducedMotion
            prefs=effective;sourceId=config?.assetId;sourceEdit=config?.edit ?: ImageEdit();sourceRevision=encoded
            configureSensors()
            if(changed || bitmap==null) reload() else scheduleFrame()
        }
        override fun onWallpaperFlagsChanged(which: Int) {
            if(Build.VERSION.SDK_INT<34) return
            super.onWallpaperFlagsChanged(which)
            if(isPreview) return
            // Serialized on this engine's thread; no static screen flow participates in source selection.
            ownershipEvents.trySend(which)
        }
        private fun configureSensors() {
            val wanted=visible && gl!=null && prefs.gyroParallax && !prefs.reducedMotion && !prefs.safeMode
            if(wanted && !sensorRegistered) {
                val sensor=sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                if(sensor!=null) sensorRegistered=sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI,handler)
            } else if(!wanted && sensorRegistered) { sensors.unregisterListener(this); sensorRegistered=false; tiltX=0f; tiltY=0f }
        }
        private fun setupGl() {
            if(gl!=null || !surfaceReady || !visible || prefs.safeMode || prefs.clockOverlay || prefs.batteryOverlay) return
            val version=getSystemService(ActivityManager::class.java).deviceConfigurationInfo.reqGlEsVersion
            if(version<0x20000) return
            try { gl=GlWallpaperRenderer().also { it.create(surfaceHolder.surface,width,height) } }
            catch(e: Exception) { gl?.close(); gl=null; RenderTelemetry.lastError="GLES setup failed; Canvas fallback" }
        }
        private fun scheduleFrame() {
            handler.removeCallbacks(frame)
            if(visible && surfaceReady && !destroyed) {
                val thermal=if(Build.VERSION.SDK_INT>=29) power.currentThermalStatus else 0
                val q=AdaptiveQualityPolicy.evaluate(QualityInput(true,true,prefs.frameRate,power.isPowerSaveMode,thermal,getSystemService(ActivityManager::class.java).isLowRamDevice,prefs.reducedMotion,prefs.safeMode,gl!=null))
                val delay=if(q.framesPerSecond>0) (1000L/q.framesPerSecond-(android.os.SystemClock.uptimeMillis()-lastDrawAt)).coerceAtLeast(0) else 0L
                handler.postDelayed(frame,delay)
            }
        }
        private fun reload() {
            loading?.cancel()
            val generation=++loadGeneration
            if(!surfaceReady || !visible || destroyed) return
            if(prefs.safeMode || prefs.clockOverlay || prefs.batteryOverlay) { gl?.close(); gl=null }
            setupGl()
            configureSensors()
            val id=sourceId ?: run { bitmap?.recycle();bitmap=null;gl?.close();gl=null;scheduleFrame();return }
            val w=width.coerceIn(1,4096); val h=height.coerceIn(1,4096)
            val safe=prefs.safeMode
            val edit=sourceEdit
            loading=scope.launch {
                val asset=graph.repository.dao.asset(id) ?: return@launch
                val result=try { graph.pipeline.render(graph.importer.file(asset),w,h,edit,safe) }
                    catch(e: CancellationException) { throw e }
                    catch(_: Exception) { null }
                if(!isActive || destroyed) { result?.recycle(); return@launch }
                handler.post {
                    if(destroyed || !visible || !surfaceReady || generation!=loadGeneration) result?.recycle()
                    else {
                        bitmap?.recycle(); bitmap=result
                        if(Build.VERSION.SDK_INT>=27) { colors=result?.let { WallpaperColors.fromBitmap(it) }; notifyColorsChanged() }
                        if(result!=null && gl!=null) {
                            try {
                                gl!!.upload(result,!prefs.reducedMotion)
                                transitionStart=if(prefs.reducedMotion || prefs.transition=="NONE") 0 else android.os.SystemClock.uptimeMillis()
                            } catch(_: Exception) { gl?.close(); gl=null; RenderTelemetry.lastError="GPU upload failed; Canvas fallback" }
                        }
                        scheduleFrame()
                    }
                }
            }
        }
        private fun drawFrame(quality: RenderQuality) {
            lastDrawAt=android.os.SystemClock.uptimeMillis()
            if(gl!=null && quality.gpu) {
                val effect=ShaderEffect.entries.firstOrNull { it.name==prefs.shaderEffect } ?: ShaderEffect.NONE
                val transition=Transition.entries.firstOrNull { it.name==prefs.transition } ?: Transition.NONE
                val elapsed=(android.os.SystemClock.uptimeMillis()-transitionStart).toFloat()/prefs.transitionMs.coerceAtLeast(100)
                val x=if(prefs.parallax && !prefs.reducedMotion) offsetX else 0f
                val y=if(prefs.parallax && !prefs.reducedMotion) offsetY else 0f
                try {
                    gl!!.draw(effect,prefs.effectIntensity*quality.effectScale,if(transitionStart==0L) 1f else elapsed,if(quality.motion) transition else Transition.NONE,
                        x+tiltX,y+tiltY,if(prefs.reducedMotion) 0f else android.os.SystemClock.uptimeMillis()%60000/1000f,
                        if(prefs.zoomAnimation && !prefs.reducedMotion && !prefs.safeMode) (1.0+.025*(1-kotlin.math.cos(android.os.SystemClock.uptimeMillis()/10000.0))).toFloat() else 1f)
                    recoveryAttempted=false
                    return
                } catch(_: Exception) {
                    gl?.close(); gl=null; RenderTelemetry.lastError="Context lost; one recovery then Canvas fallback"
                    if(!recoveryAttempted) {
                        recoveryAttempted=true; RenderTelemetry.recoveries.incrementAndGet(); setupGl()
                        bitmap?.let { b -> try { gl?.upload(b,false); scheduleFrame(); return } catch(_: Exception) { gl?.close(); gl=null } }
                    }
                }
            }
            drawCanvas()
        }
        private fun drawCanvas() {
            val start=System.nanoTime(); var canvas: Canvas?=null
            try {
                canvas=surfaceHolder.lockCanvas() ?: return
                canvas.drawColor(Color.rgb(15,15,18))
                bitmap?.let { canvas.drawBitmap(it,null,Rect(0,0,canvas.width,canvas.height),Paint(Paint.FILTER_BITMAP_FLAG)) }
                if(prefs.clockOverlay || prefs.batteryOverlay) {
                    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=canvas.width*.065f; typeface=Typeface.create("sans-serif-light",Typeface.NORMAL); setShadowLayer(6f,0f,2f,Color.BLACK) }
                    if(prefs.clockOverlay) canvas.drawText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),canvas.width*.08f,canvas.height*.85f,paint)
                    if(prefs.batteryOverlay) { paint.textSize=canvas.width*.025f; val c=graph.repository.contextSnapshot(); canvas.drawText("${c.batteryPercent}%${if(c.charging) " · charging" else ""}",canvas.width*.08f,canvas.height*.92f,paint) }
                }
            } catch(_: IllegalArgumentException) { /* Window manager removed the surface while locking. */ }
              catch(_: IllegalStateException) { /* Surface is no longer owned. */ }
            finally { canvas?.let { try { surfaceHolder.unlockCanvasAndPost(it) } catch(_: IllegalArgumentException) {} }; RenderTelemetry.frame(System.nanoTime()-start) }
        }
        override fun onOffsetsChanged(xOffset: Float,yOffset: Float,xStep: Float,yStep: Float,xPixels: Int,yPixels: Int) {
            handler.post { offsetX=(xOffset-.5f)*.05f; offsetY=(yOffset-.5f)*.03f; if(prefs.parallax) scheduleFrame() }
        }
        override fun onTouchEvent(event: MotionEvent) {
            if(prefs.parallax && event.actionMasked==MotionEvent.ACTION_MOVE) { val x=event.x; val y=event.y; handler.post { offsetX=(x/width-.5f)*.05f; offsetY=(y/height-.5f)*.03f; scheduleFrame() } }
        }
        override fun onSensorChanged(event: SensorEvent) {
            val matrix=FloatArray(9); val angles=FloatArray(3)
            SensorManager.getRotationMatrixFromVector(matrix,event.values); SensorManager.getOrientation(matrix,angles)
            tiltX=(angles[2]*.02f).coerceIn(-.03f,.03f); tiltY=(angles[1]*.02f).coerceIn(-.03f,.03f); scheduleFrame()
        }
        override fun onAccuracyChanged(sensor: Sensor?,accuracy: Int) = Unit
        override fun onVisibilityChanged(isVisible: Boolean) {
            visible=isVisible
            handler.post { handler.removeCallbacks(frame); configureSensors(); if(isVisible) reload() else releaseFrame() }
        }
        private fun releaseFrame() { loadGeneration++;loading?.cancel(); bitmap?.recycle(); bitmap=null; gl?.close(); gl=null }
        override fun onSurfaceCreated(holder: SurfaceHolder) { super.onSurfaceCreated(holder); surfaceReady=true; handler.post { if(visible) reload() } }
        override fun onSurfaceChanged(holder: SurfaceHolder,format: Int,w: Int,h: Int) { super.onSurfaceChanged(holder,format,w,h); width=w; height=h; surfaceReady=true; handler.post { gl?.close(); gl=null; reload() } }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { surfaceReady=false; handler.post { handler.removeCallbacks(frame); releaseFrame() }; super.onSurfaceDestroyed(holder) }
        override fun onComputeColors(): WallpaperColors?=colors
        override fun onDestroy() {
            destroyed=true; visible=false; surfaceReady=false; ownershipEvents.close();scope.cancel();LiveEngineRegistry.remove(engineId)
            handler.post { handler.removeCallbacksAndMessages(null); sensors.unregisterListener(this); sensorRegistered=false; releaseFrame(); RenderTelemetry.engines.decrementAndGet(); thread.quitSafely() }
            super.onDestroy()
        }
    }
}

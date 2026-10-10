package com.westkitty.dexwallpaper.wallpaper

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import com.westkitty.dexwallpaper.core.ShaderEffect
import com.westkitty.dexwallpaper.core.Transition
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Dedicated-thread EGL ES2 renderer, also usable with a pbuffer for real shader regression tests. */
class GlWallpaperRenderer {
    private var display: EGLDisplay=EGL14.EGL_NO_DISPLAY
    private var context: EGLContext=EGL14.EGL_NO_CONTEXT
    private var surface: EGLSurface=EGL14.EGL_NO_SURFACE
    private var program=0
    private var texture=0
    private var previousTexture=0
    private var width=1
    private var height=1
    var maxTextureSize=0
        private set
    private var counted=false
    private val vertices=ByteBuffer.allocateDirect(4*4*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        // Image pixels are top-down; texture coordinates compensate rather than flipping the bitmap.
        put(floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)); position(0)
    }
    fun create(nativeSurface: Surface?, w: Int, h: Int) {
        close()
        try {
            display=EglDisplayPool.acquire()
            val configs=arrayOfNulls<EGLConfig>(1); val count=IntArray(1)
            require(EGL14.eglChooseConfig(display,intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,if(nativeSurface==null) EGL14.EGL_PBUFFER_BIT else EGL14.EGL_WINDOW_BIT,EGL14.EGL_NONE),0,configs,0,1,count,0) && count[0]>0) { "No GLES2 surface configuration" }
            val config=requireNotNull(configs[0])
            context=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            require(context!=EGL14.EGL_NO_CONTEXT) { "GLES2 context creation failed" }
            surface=if(nativeSurface==null) EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,w,EGL14.EGL_HEIGHT,h,EGL14.EGL_NONE),0)
                else EGL14.eglCreateWindowSurface(display,config,nativeSurface,intArrayOf(EGL14.EGL_NONE),0)
            require(surface!=EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display,surface,surface,context)) { "EGL surface creation failed" }
            counted=true; RenderTelemetry.contexts.incrementAndGet()
            val limits=IntArray(1); GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,limits,0); maxTextureSize=limits[0]
            require(maxTextureSize>=256) { "GPU texture capability insufficient" }
            program=linkProgram(VERTEX,FRAGMENT)
            resize(w,h)
            checkGl("create")
        } catch(e: Exception) { close(); throw e }
    }
    fun resize(w: Int,h: Int) { width=w.coerceAtLeast(1); height=h.coerceAtLeast(1); GLES20.glViewport(0,0,width,height) }
    fun upload(bitmap: Bitmap, keepPrevious: Boolean=true) {
        require(program!=0)
        if(previousTexture!=0) { deleteTexture(previousTexture); previousTexture=0 }
        if(keepPrevious) previousTexture=texture else if(texture!=0) deleteTexture(texture)
        texture=0
        val id=IntArray(1); GLES20.glGenTextures(1,id,0); texture=id[0]; require(texture!=0) { "Texture allocation failed" }; RenderTelemetry.textures.incrementAndGet()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        val factor=maxTextureSize.toFloat()/maxOf(bitmap.width,bitmap.height)
        val safe=if(factor<1) Bitmap.createScaledBitmap(bitmap,(bitmap.width*factor).toInt().coerceAtLeast(1),(bitmap.height*factor).toInt().coerceAtLeast(1),true) else bitmap
        try { GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,safe,0); checkGl("upload") } finally { if(safe!==bitmap) safe.recycle() }
    }
    fun draw(effect: ShaderEffect,intensity: Float,progress: Float=1f,transition: Transition=Transition.NONE,parallaxX: Float=0f,parallaxY: Float=0f,timeSeconds: Float=0f,zoom: Float=1f) {
        if(program==0 || texture==0) return
        val start=System.nanoTime()
        GLES20.glUseProgram(program)
        GLES20.glClearColor(0f,0f,0f,1f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        vertices.position(0)
        val position=GLES20.glGetAttribLocation(program,"aPosition"); val uv=GLES20.glGetAttribLocation(program,"aUv")
        GLES20.glEnableVertexAttribArray(position); GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,16,vertices)
        vertices.position(2); GLES20.glEnableVertexAttribArray(uv); GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,16,vertices)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glUniform1i(location("uTexture"),0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,if(previousTexture!=0) previousTexture else texture)
        GLES20.glUniform1i(location("uPrevious"),1)
        GLES20.glUniform1i(location("uEffect"),effect.ordinal)
        GLES20.glUniform1f(location("uIntensity"),intensity.coerceIn(0f,1f))
        GLES20.glUniform1f(location("uProgress"),progress.coerceIn(0f,1f))
        GLES20.glUniform1i(location("uTransition"),if(previousTexture!=0) transition.ordinal else 0)
        GLES20.glUniform2f(location("uResolution"),width.toFloat(),height.toFloat())
        GLES20.glUniform2f(location("uParallax"),parallaxX.coerceIn(-.04f,.04f),parallaxY.coerceIn(-.04f,.04f))
        GLES20.glUniform1f(location("uTime"),timeSeconds)
        GLES20.glUniform1f(location("uZoom"),zoom.coerceIn(1f,1.08f))
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        checkGl("draw")
        if(!EGL14.eglSwapBuffers(display,surface)) throw IllegalStateException("EGL context/surface lost (${EGL14.eglGetError()})")
        if(progress>=1f && previousTexture!=0) { deleteTexture(previousTexture); previousTexture=0 }
        RenderTelemetry.frame(System.nanoTime()-start)
    }
    /** Test/readback only: must run on the owning EGL thread. Bottom-up RGBA bytes. */
    fun pixels(): ByteArray {
        val bytes=ByteBuffer.allocateDirect(width*height*4)
        GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes); checkGl("readback")
        return ByteArray(width*height*4).also { bytes.position(0); bytes.get(it) }
    }
    private fun location(name: String)=GLES20.glGetUniformLocation(program,name)
    private fun deleteTexture(id: Int) { GLES20.glDeleteTextures(1,intArrayOf(id),0); RenderTelemetry.textures.decrementAndGet() }
    private fun checkGl(stage: String) { val error=GLES20.glGetError(); check(error==GLES20.GL_NO_ERROR) { "OpenGL error $error at $stage" } }
    private fun shader(type: Int,source: String): Int {
        val id=GLES20.glCreateShader(type); require(id!=0)
        GLES20.glShaderSource(id,source); GLES20.glCompileShader(id)
        val status=IntArray(1); GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,status,0)
        if(status[0]==0) { val log=GLES20.glGetShaderInfoLog(id); GLES20.glDeleteShader(id); throw IllegalStateException("Shader compile failed: $log") }
        return id
    }
    private fun linkProgram(vertex: String,fragment: String): Int {
        val v=shader(GLES20.GL_VERTEX_SHADER,vertex)
        var f=0; var p=0
        try {
            f=shader(GLES20.GL_FRAGMENT_SHADER,fragment); p=GLES20.glCreateProgram()
            GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p)
            val ok=IntArray(1); GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0)
            check(ok[0]!=0) { "Shader link failed: ${GLES20.glGetProgramInfoLog(p)}" }
            return p
        } catch(e: Exception) { if(p!=0) GLES20.glDeleteProgram(p); throw e }
        finally { GLES20.glDeleteShader(v); if(f!=0) GLES20.glDeleteShader(f) }
    }
    fun close() {
        if(display!=EGL14.EGL_NO_DISPLAY) {
            if(context!=EGL14.EGL_NO_CONTEXT && surface!=EGL14.EGL_NO_SURFACE) EGL14.eglMakeCurrent(display,surface,surface,context)
            if(texture!=0) deleteTexture(texture)
            if(previousTexture!=0) deleteTexture(previousTexture)
            if(program!=0) GLES20.glDeleteProgram(program)
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            if(surface!=EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,surface)
            if(context!=EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context)
            EglDisplayPool.release(); EGL14.eglReleaseThread()
        }
        if(counted) { RenderTelemetry.contexts.decrementAndGet(); counted=false }
        display=EGL14.EGL_NO_DISPLAY; context=EGL14.EGL_NO_CONTEXT; surface=EGL14.EGL_NO_SURFACE
        program=0; texture=0; previousTexture=0; maxTextureSize=0
    }
    companion object {
        private const val VERTEX="""
            attribute vec2 aPosition;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main(){ gl_Position=vec4(aPosition,0.0,1.0); vUv=aUv; }
        """
        private const val FRAGMENT="""
            precision mediump float;
            varying vec2 vUv;
            uniform sampler2D uTexture;
            uniform sampler2D uPrevious;
            uniform int uEffect;
            uniform int uTransition;
            uniform float uIntensity;
            uniform float uProgress;
            uniform float uTime;
            uniform float uZoom;
            uniform vec2 uResolution;
            uniform vec2 uParallax;
            float lum(vec3 c){return dot(c,vec3(0.299,0.587,0.114));}
            float noise(vec2 p){return fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453);}
            vec3 image(vec2 uv){return texture2D(uTexture,clamp(uv,vec2(0.0),vec2(1.0))).rgb;}
            void main(){
                vec2 uv=(vUv-0.5)/uZoom+0.5+uParallax;
                if(uTransition==2) uv=(uv-0.5)*(1.0+0.15*(1.0-uProgress))+0.5;
                if(uTransition==3) uv.x=uv.x-(1.0-uProgress);
                vec3 base=image(uv); vec3 color=base;
                if(uEffect==1 || uEffect==2){
                    float haze=(uEffect==1 ? uv.y : 1.0-uv.y)*uIntensity*0.55;
                    color=mix(base,vec3(0.78,0.82,0.90),haze);
                } else if(uEffect==3){
                    float ridge=sin(uv.x*180.0);
                    color=image(uv+vec2(ridge*0.015*uIntensity,0.0))+vec3(max(ridge,0.0)*0.08*uIntensity);
                } else if(uEffect==4 || uEffect==5){
                    float direction=uEffect==4 ? 1.0 : -1.0;
                    vec2 shift=sin(uv.yx*32.0)*0.012*uIntensity*direction;
                    color=image(uv+shift);
                } else if(uEffect==6 || uEffect==7){
                    vec3 fill=vec3(0.96,0.19,0.25);
                    float amount=(uEffect==6 ? lum(base) : 1.0-lum(base))*uIntensity;
                    color=mix(base,fill,amount);
                } else if(uEffect==8 || uEffect==9){
                    vec2 px=vec2(2.0)/uResolution;
                    float edge=abs(lum(image(uv+vec2(px.x,0.0)))-lum(image(uv-vec2(px.x,0.0))))+abs(lum(image(uv+vec2(0.0,px.y)))-lum(image(uv-vec2(0.0,px.y))));
                    vec3 paper=vec3(1.0-clamp(edge*8.0,0.0,1.0));
                    if(uEffect==9) paper=1.0-paper;
                    color=mix(base,paper,uIntensity);
                } else if(uEffect==10 || uEffect==11){
                    float signum=uEffect==10 ? 1.0 : -1.0;
                    vec2 shift=vec2(noise(uv*uResolution),noise(uv.yx*uResolution))*0.02*uIntensity*signum;
                    vec3 blur=image(uv+shift)+image(uv+shift+vec2(0.004,0.0))+image(uv+shift-vec2(0.004,0.0));
                    color=mix(base,blur/3.0,uIntensity);
                } else if(uEffect==12 || uEffect==13){
                    vec2 grid=uv*uResolution/7.0;
                    vec2 cell=(floor(grid)+0.5)*7.0/uResolution;
                    float l=lum(image(cell)); float radius=sqrt(l)*0.48;
                    float dotMask=1.0-smoothstep(radius-0.04,radius+0.04,length(fract(grid)-0.5));
                    vec3 halftone=vec3(uEffect==12 ? dotMask : 1.0-dotMask);
                    color=mix(base,halftone,uIntensity);
                } else if(uEffect==14){
                    float grain=(noise(uv*uResolution+uTime)-0.5)*0.16*uIntensity;
                    color=base+vec3(grain);
                }
                if(uTransition!=0) color=mix(texture2D(uPrevious,clamp(vUv+uParallax,vec2(0.0),vec2(1.0))).rgb,color,uProgress);
                gl_FragColor=vec4(clamp(color,0.0,1.0),1.0);
            }
        """
    }
}

/** EGL_DEFAULT_DISPLAY is shared by preview and installed engines; terminate only the last owner. */
private object EglDisplayPool {
    private var shared: EGLDisplay=EGL14.EGL_NO_DISPLAY
    private var owners=0
    @Synchronized fun acquire(): EGLDisplay {
        if(owners==0) {
            shared=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            require(shared!=EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(shared,IntArray(2),0,IntArray(2),0)) { "EGL display unavailable" }
        }
        owners++
        return shared
    }
    @Synchronized fun release() { if(owners>0 && --owners==0) { EGL14.eglTerminate(shared); shared=EGL14.EGL_NO_DISPLAY } }
}

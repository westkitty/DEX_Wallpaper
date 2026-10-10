package com.westkitty.dexwallpaper

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.wallpaper.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlRendererTest {
    @Test fun actualEglPixelsTransitionsEffectsAndResourceCleanup() {
        val baseline=RenderTelemetry.snapshot()
        val gl=GlWallpaperRenderer()
        val red=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val blue=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            gl.create(null,64,64); gl.upload(red,false); gl.draw(ShaderEffect.NONE,0f)
            val first=gl.pixels(); assertTrue(first[0].toInt() and 255>240); assertTrue(first[2].toInt() and 255<10)
            gl.upload(blue,true); gl.draw(ShaderEffect.NONE,0f,.5f,Transition.CROSSFADE)
            val blend=gl.pixels(); assertTrue(blend[0].toInt() and 255 in 110..145); assertTrue(blend[2].toInt() and 255 in 110..145)
            ShaderEffect.entries.forEach { gl.draw(it,.8f) }
            gl.close(); gl.create(null,64,64); gl.upload(red,false); gl.draw(ShaderEffect.NONE,0f)
            assertTrue(gl.pixels()[0].toInt() and 255>240)
        } finally { gl.close(); red.recycle(); blue.recycle() }
        val final=RenderTelemetry.snapshot(); assertEquals(baseline.contexts,final.contexts); assertEquals(baseline.textures,final.textures)
    }
}

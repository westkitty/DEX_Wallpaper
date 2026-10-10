package com.westkitty.dexwallpaper.creative

import android.graphics.*
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.WallRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Declarative Canvas layers. Asset references resolve only against the private shared library. */
class SceneRenderer(private val repository: WallRepository) {
    suspend fun render(scene: SceneManifest, width: Int, height: Int, elapsedSeconds: Double = 0.0): Bitmap {
        SceneCodec.validate(scene)
        require(width in 64..8192 && height in 64..8192 && width.toLong()*height <= 16_000_000)
        val output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(output)
            canvas.drawColor(Color.parseColor(scene.background))
            val context=repository.contextSnapshot()
            val vars=scene.variables + mapOf("time" to elapsedSeconds.coerceIn(0.0,3600.0),"hour" to context.time.hour.toDouble(),
                "battery" to context.batteryPercent.toDouble(),"charging" to if(context.charging) 1.0 else 0.0)
            for(layer in scene.layers) {
                val visible=(SafeFormula.evaluate(layer.visibleWhen,vars) as? Outcome.Ok)?.value ?: 0.0
                if(visible==0.0) continue
                val x=if(layer.xFormula.isBlank()) layer.x else ((SafeFormula.evaluate(layer.xFormula,vars) as? Outcome.Ok)?.value?.toFloat() ?: layer.x).coerceIn(-1f,2f)
                val left=x*width; val top=layer.y*height; val w=layer.width*width; val h=layer.height*height
                val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    color=Color.parseColor(layer.color); alpha=(alpha*layer.opacity).toInt()
                    xfermode=when(layer.blend) { "MULTIPLY" -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY); "SCREEN" -> PorterDuffXfermode(PorterDuff.Mode.SCREEN); "ADD" -> PorterDuffXfermode(PorterDuff.Mode.ADD); else -> null }
                    typeface=Typeface.create(layer.font,Typeface.NORMAL)
                }
                canvas.save()
                canvas.rotate(layer.rotation,left+w/2,top+h/2)
                when(layer.kind) {
                    LayerKind.RECTANGLE -> canvas.drawRect(left,top,left+w,top+h,paint)
                    LayerKind.CIRCLE -> canvas.drawOval(left,top,left+w,top+h,paint)
                    LayerKind.TEXT,LayerKind.CLOCK,LayerKind.BATTERY -> {
                        val text=when(layer.kind) { LayerKind.CLOCK -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")); LayerKind.BATTERY -> "${context.batteryPercent}%"; else -> layer.text }
                        paint.textSize=h.coerceAtLeast(1f)
                        val measure=paint.measureText(text)
                        if(measure>w && measure>0) paint.textSize*=w/measure
                        canvas.drawText(text,left,top+h,paint)
                    }
                    LayerKind.IMAGE -> {
                        val asset=repository.dao.asset(layer.assetId)
                        if(asset!=null && !asset.quarantined && repository.importer.file(asset).isFile) {
                            val bitmap=repository.pipeline.decode(repository.importer.file(asset),w.toInt().coerceIn(1,4096),h.toInt().coerceIn(1,4096))
                            try { canvas.drawBitmap(bitmap,null,RectF(left,top,left+w,top+h),paint) } finally { bitmap.recycle() }
                        }
                    }
                }
                canvas.restore()
            }
            return output
        } catch(e: Exception) { output.recycle(); throw e }
    }
}

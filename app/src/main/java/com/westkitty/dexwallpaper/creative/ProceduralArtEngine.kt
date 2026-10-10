package com.westkitty.dexwallpaper.creative

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

/** Deterministic offline recipes. The same seed/palette/geometry reconstructs the composition. */
enum class Pattern(val title: String) { CONTOURS("Contour fields"), BAUHAUS("Geometric study"), STRATA("Strata"), GRID("Technical grid"), GRAIN("Paper texture") }
data class Composition(val seed: Long = 42, val pattern: Pattern = Pattern.CONTOURS, val background: Int = 0xff101014.toInt(),
    val primary: Int = 0xffff4655.toInt(), val secondary: Int = 0xffe5d5bf.toInt(), val density: Int = 18)
class ProceduralArtEngine {
    fun render(c: Composition, width: Int, height: Int): Bitmap {
        require(width in 64..8192 && height in 64..8192 && width.toLong()*height <= 16_000_000) { "Generation exceeds 16MP limit" }
        require(c.density in 4..64) { "Pattern density outside bounds" }
        val bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(c.seed)
        canvas.drawColor(c.background)
        when(c.pattern) {
            Pattern.CONTOURS -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = (width / 800f).coerceAtLeast(1f)
                val phase = random.nextDouble()*PI*2
                for (row in 0 until c.density*3) {
                    val path = Path()
                    for (step in 0..100) {
                        val x = step / 100f * width
                        val y = row.toFloat() / (c.density*3) * height + sin(step/100.0*PI*3 + phase + row*.09)*height*.13
                        if (step==0) path.moveTo(x,y.toFloat()) else path.lineTo(x,y.toFloat())
                    }
                    paint.color = if (row % 5 == 0) c.primary else c.secondary; paint.alpha = if (row%5==0) 220 else 65
                    canvas.drawPath(path,paint)
                }
            }
            Pattern.BAUHAUS -> {
                val cell = width / 6f
                for (row in 0..ceil(height/cell).toInt()) for (col in 0..5) {
                    val x=col*cell; val y=row*cell
                    paint.color=if(random.nextBoolean()) c.primary else c.secondary
                    paint.alpha=if(random.nextBoolean()) 240 else 90
                    when(random.nextInt(3)) {
                        0 -> canvas.drawCircle(x+cell/2,y+cell/2,cell*.38f,paint)
                        1 -> canvas.drawArc(RectF(x+cell*.1f,y+cell*.1f,x+cell*.9f,y+cell*.9f),(random.nextInt(4)*90).toFloat(),180f,true,paint)
                        else -> canvas.drawRect(x+cell*.12f,y+cell*.12f,x+cell*.88f,y+cell*.88f,paint)
                    }
                }
            }
            Pattern.STRATA -> {
                for(row in 0 until c.density) {
                    val y = row.toFloat()/c.density*height
                    val path=Path().apply { moveTo(0f,y) }
                    for(step in 0..60) path.lineTo(step/60f*width,(y + sin(step/60.0*PI*2 + row*.4)*height*.07).toFloat())
                    path.lineTo(width.toFloat(),height.toFloat()); path.lineTo(0f,height.toFloat()); path.close()
                    paint.color=if(row%3==0) c.primary else c.secondary; paint.alpha=(40+row*180/c.density).coerceAtMost(255)
                    canvas.drawPath(path,paint)
                }
            }
            Pattern.GRID -> {
                paint.color=c.secondary; paint.alpha=50; paint.strokeWidth=width/1200f
                val gap=width/c.density.toFloat()
                for(n in 0..c.density) canvas.drawLine(n*gap,0f,n*gap,height.toFloat(),paint)
                for(n in 0..ceil(height/gap).toInt()) canvas.drawLine(0f,n*gap,width.toFloat(),n*gap,paint)
                paint.color=c.primary; paint.alpha=230
                repeat(c.density) { val x=random.nextInt(c.density)*gap; val y=random.nextInt(ceil(height/gap).toInt())*gap; canvas.drawRect(x,y,x+gap,y+gap,paint) }
            }
            Pattern.GRAIN -> {
                // Bounded grain count independent of native resolution, preserving offline memory limits.
                repeat(c.density*2500) {
                    paint.color=if(random.nextBoolean()) c.primary else c.secondary
                    paint.alpha=random.nextInt(10,60)
                    val x=random.nextFloat()*width; val y=random.nextFloat()*height
                    canvas.drawCircle(x,y,(width/1200f).coerceAtLeast(.5f),paint)
                }
            }
        }
        return bitmap
    }
}

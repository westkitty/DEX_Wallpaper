package com.westkitty.dexwallpaper.wallpaper

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class RenderMetrics(val engines: Int,val contexts: Int,val textures: Int,val totalFrames: Long,val meanFrameMs: Double,val lastFrameMs: Double,val contextRecoveries: Long,val lastError: String)
object RenderTelemetry {
    val engines=AtomicInteger(0)
    val contexts=AtomicInteger(0)
    val textures=AtomicInteger(0)
    val frames=AtomicLong(0)
    val recoveries=AtomicLong(0)
    private val durations=ArrayDeque<Long>()
    @Volatile var lastError: String=""
    @Synchronized fun frame(nanos: Long) { frames.incrementAndGet(); durations.addLast(nanos); if(durations.size>120) durations.removeFirst() }
    @Synchronized fun snapshot()=RenderMetrics(engines.get(),contexts.get(),textures.get(),frames.get(),
        if(durations.isEmpty()) 0.0 else durations.average()/1_000_000,if(durations.isEmpty()) 0.0 else durations.last().toDouble()/1_000_000,recoveries.get(),lastError)
}

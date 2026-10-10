package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class RenderPolicyTest {
    private val normal=QualityInput(true,true,60,false,0,false,false,false,true)
    @Test fun hiddenHasNoFramesOrGpuWork() { val q=AdaptiveQualityPolicy.evaluate(normal.copy(visible=false)); assertEquals(0,q.framesPerSecond); assertFalse(q.gpu); assertFalse(q.motion) }
    @Test fun batteryAndThermalCapsAreDeterministic() { assertEquals(12,AdaptiveQualityPolicy.evaluate(normal.copy(batterySaver=true)).framesPerSecond); assertEquals(5,AdaptiveQualityPolicy.evaluate(normal.copy(thermalSeverity=4)).framesPerSecond) }
    @Test fun safeModeRetainsStaticFallback() { val q=AdaptiveQualityPolicy.evaluate(normal.copy(safeMode=true)); assertFalse(q.gpu); assertEquals(0f,q.effectScale,0f) }
    @Test fun reducedMotionAndStaticScenesAreEventDriven() { assertEquals(0,AdaptiveQualityPolicy.evaluate(normal.copy(reducedMotion=true)).framesPerSecond); assertFalse(AdaptiveQualityPolicy.evaluate(normal.copy(animated=false)).motion) }
    @Test fun lowRamUsesBoundedFrameCap() { assertEquals(24,AdaptiveQualityPolicy.evaluate(normal.copy(lowRam=true)).framesPerSecond) }
}

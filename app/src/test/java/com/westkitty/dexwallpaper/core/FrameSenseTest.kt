package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test

class FrameSenseTest {
    @Test fun fillCoversLandscape() { val p = FrameSense.place(1000,1000,1600,1000,ImageEdit()); assertEquals(1600f,p.width,0f); assertEquals(-300f,p.y,0f) }
    @Test fun fitPreservesFullImage() { val p = FrameSense.place(1000,1000,1600,1000,ImageEdit(scale = ScaleMode.FIT)); assertEquals(300f,p.x,0f); assertEquals(1000f,p.width,0f) }
    @Test fun portraitUsesIndependentFocalPoint() { val p = FrameSense.place(1600,1000,1000,1600,ImageEdit(focalX = 0f,portraitX = 1f)); assertTrue(p.x < 0) }
    @Test fun stretchMatchesExactBounds() { val p = FrameSense.place(400,300,1000,1600,ImageEdit(scale = ScaleMode.STRETCH)); assertEquals(1000f,p.width,0f); assertEquals(1600f,p.height,0f) }
    @Test fun originalUsesNativePixels() { assertEquals(400f, FrameSense.place(400,300,1000,1600,ImageEdit(scale = ScaleMode.ORIGINAL)).width,0f) }
    @Test fun boundsPreventInvalidCrop() { val e = ImageEdit(zoom = 10f,portraitX = -1f,brightness = 5f).bounded(); assertEquals(4f,e.zoom,0f); assertEquals(0f,e.portraitX,0f); assertEquals(1f,e.brightness,0f) }
    @Test fun sampleSizeBoundsLargeDecode() { val sample = AssetIntegrity.sampleSize(30000,20000,1000,1000); assertTrue(30000L*20000/sample/sample <= 16_000_000) }
    @Test fun dHashDistanceIsLocalAndDeterministic() { assertEquals(1,AssetIntegrity.hammingDistance(0,1)); assertFalse(AssetIntegrity.nearDuplicate(0,-1)) }
}

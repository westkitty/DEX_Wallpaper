package com.westkitty.dexwallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.wallpaper.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Modifies only the test emulator's wallpapers. It is NOT Samsung/OEM visual acceptance. */
@RunWith(AndroidJUnit4::class)
class WallpaperIsolationTest {
    @Test fun explicitHomeWritePreservesIndependentLockIdentity() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val manager=WallpaperManager.getInstance(context)
        val home=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val lock=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            manager.setBitmap(home,null,false,WallpaperManager.FLAG_SYSTEM)
            manager.setBitmap(lock,null,false,WallpaperManager.FLAG_LOCK)
            val lockId=manager.getWallpaperId(WallpaperManager.FLAG_LOCK)
            assertTrue(lockId>0)
            val result=WallpaperDestinationController(AndroidWallpaperPlatform(context)).apply(home,Destination.HOME)
            assertTrue(result.toString(),result is Outcome.Ok)
            assertEquals(lockId,manager.getWallpaperId(WallpaperManager.FLAG_LOCK))
        } finally { home.recycle(); lock.recycle() }
    }
    @Test fun explicitLockWritePreservesHomeIdentity() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val manager=WallpaperManager.getInstance(context)
        val bitmap=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        try {
            manager.setBitmap(bitmap,null,false,WallpaperManager.FLAG_SYSTEM)
            val homeId=manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val result=WallpaperDestinationController(AndroidWallpaperPlatform(context)).apply(bitmap,Destination.LOCK)
            assertTrue(result.toString(),result is Outcome.Ok)
            assertEquals(homeId,manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM))
        } finally { bitmap.recycle() }
    }
}

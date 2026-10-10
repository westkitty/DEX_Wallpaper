package com.westkitty.dexwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.data.*
import com.westkitty.dexwallpaper.wallpaper.BitmapPipeline
import com.westkitty.dexwallpaper.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativePipelineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun importDecodeDeduplicateAndPersistCollections() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val pipeline = BitmapPipeline()
        val importer = AssetImporter(context,db,pipeline)
        val source = File(context.cacheDir,"test-fixture.png")
        val bitmap = Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        try {
            db.library().putCollection(CollectionEntity("test","Test"))
            val first = importer.importUris(listOf(Uri.fromFile(source)),"test") {}
            assertEquals(1,first.imported)
            assertEquals(1,db.library().collectionAssets("test").size)
            val second = importer.importUris(listOf(Uri.fromFile(source)),"test") {}
            assertEquals(1,second.duplicates)
            val asset = db.library().allAssets().single()
            val rendered = pipeline.render(importer.file(asset),240,160,ImageEdit())
            assertEquals(Color.RED,rendered.getPixel(120,80)); rendered.recycle()
            importer.file(asset).delete()
            assertEquals(1,importer.validateAssets())
            assertTrue(db.library().asset(asset.id)!!.quarantined)
        } finally { source.delete(); db.close() }
    }
    @Test fun corruptInputIsRejectedWithoutRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val importer = AssetImporter(context,db,BitmapPipeline())
        val source = File(context.cacheDir,"broken.png").apply { writeText("not an image") }
        try { db.library().putCollection(CollectionEntity("test","Test")); assertEquals(1,importer.importUris(listOf(Uri.fromFile(source)),"test") {}.rejected); assertTrue(db.library().allAssets().isEmpty()) }
        finally { source.delete(); db.close() }
    }
}

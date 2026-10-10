package com.westkitty.dexwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.creative.*
import com.westkitty.dexwallpaper.data.*
import com.westkitty.dexwallpaper.wallpaper.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RecoveryAndCreativeTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun repository(db: DexDatabase): WallRepository {
        val pipeline=BitmapPipeline()
        return WallRepository(context,db,AssetImporter(context,db,pipeline),pipeline,
            WallpaperDestinationController(object: WallpaperPlatform {
                override fun snapshot()=WallpaperSnapshot(1,2)
                override fun write(bitmap: Bitmap,flags: Int) { error("Test must not apply wallpaper") }
            }),Preferences(context))
    }
    @Test fun missingOriginalReimportsByHashAndKeepsIdentity() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val wall=repository(db); wall.initialize()
        val source=File(context.cacheDir,"recovery-fixture.png")
        val b=Bitmap.createBitmap(60,60,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        source.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }; b.recycle()
        try {
            wall.importer.importUris(listOf(Uri.fromFile(source)),"default") {}
            val old=wall.dao.allAssets().single(); wall.importer.file(old).delete(); wall.importer.validateAssets()
            wall.importer.importUris(listOf(Uri.fromFile(source)),"default") {}
            val restored=wall.dao.allAssets().single()
            assertEquals(old.id,restored.id); assertFalse(restored.quarantined); assertTrue(wall.importer.file(restored).isFile)
        } finally { source.delete(); db.close() }
    }
    @Test fun generationIsDeterministicAndResolutionAware() {
        val generator=ProceduralArtEngine()
        val first=generator.render(Composition(seed=123),160,100)
        val second=generator.render(Composition(seed=123),160,100)
        val different=generator.render(Composition(seed=124),160,100)
        try { assertTrue(first.sameAs(second)); assertFalse(first.sameAs(different)); assertEquals(160,first.width) }
        finally { first.recycle(); second.recycle(); different.recycle() }
    }
    @Test fun scenesRenderAndPersistRevisions() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val wall=repository(db); wall.initialize()
        val creative=CreativeRepository(context,wall)
        try {
            val s=SceneManifest(title="Test",width=160,height=100,layers=listOf(SceneLayer("shape",LayerKind.RECTANGLE,x=0f,y=0f,width=1f,height=1f,color="#FF0000")))
            val bitmap=creative.renderer.render(s,160,100); assertEquals(Color.RED,bitmap.getPixel(80,50)); bitmap.recycle()
            val id=creative.saveScene(null,s); creative.saveScene(id,s.copy(title="Revision"))
            assertEquals(2,wall.dao.scene(id)!!.revision); assertEquals(2,wall.dao.revisions(id).size)
        } finally { db.close() }
    }
    @Test fun backupRestoreIsVerifiedAndDoesNotApplyWallpaper() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val destinationDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val destination=repository(destinationDb)
        val archive=File(context.cacheDir,"backup-fixture.zip")
        try {
            source.initialize(); destination.initialize()
            CreativeRepository(context,source).saveGenerated(Composition(),"default",160,100)
            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),true) {}
            assertEquals(1,BackupRestoreCoordinator(context,destination).restore(Uri.fromFile(archive)) {})
            assertEquals(1,destination.dao.allAssets().size); assertFalse(destination.dao.screen("HOME")!!.enabled)
            assertTrue(destination.dao.screen("HOME")!!.paused)
        } finally { archive.delete(); sourceDb.close(); destinationDb.close() }
    }
}

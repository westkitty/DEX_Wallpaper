package com.westkitty.dexwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.core.Destination
import com.westkitty.dexwallpaper.core.Outcome
import com.westkitty.dexwallpaper.data.AssetImporter
import com.westkitty.dexwallpaper.data.DexDatabase
import com.westkitty.dexwallpaper.data.Preferences
import com.westkitty.dexwallpaper.data.WallRepository
import com.westkitty.dexwallpaper.wallpaper.BitmapPipeline
import com.westkitty.dexwallpaper.wallpaper.WallpaperDestinationController
import com.westkitty.dexwallpaper.wallpaper.WallpaperPlatform
import com.westkitty.dexwallpaper.core.WallpaperSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Exercises the persistent import → collection → shuffle → Home-only repository path against a
 * recording platform. This is not a system-picker, WallpaperManager, OEM, or visible-screen pass.
 */
@RunWith(AndroidJUnit4::class)
class LocalRotationJourneyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class RecordingWallpaperPlatform : WallpaperPlatform {
        var homeId = 101
        var lockId = 202
        var homePixel = Color.BLACK
        var lockPixel = Color.BLUE
        val writes = mutableListOf<Int>()

        override fun snapshot() = WallpaperSnapshot(homeId, lockId)

        override fun write(bitmap: Bitmap, flags: Int) {
            writes += flags
            val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            if ((flags and Destination.HOME.flags) != 0) {
                homeId++
                homePixel = center
            }
            if ((flags and Destination.LOCK.flags) != 0) {
                lockId++
                lockPixel = center
            }
        }
    }

    @Test fun importedShuffleAndIntervalSurviveReopenWhileOnlyHomeIsWritten() = runBlocking {
        val databaseName = "journey-${UUID.randomUUID()}.db"
        val sourceFiles = listOf(
            fixture("journey-first.png", Color.RED),
            fixture("journey-second.png", Color.GREEN),
        )
        val platform = RecordingWallpaperPlatform()
        var database = openDatabase(databaseName)
        try {
            val pipeline = BitmapPipeline()
            val importer = AssetImporter(context, database, pipeline)
            val repository = createRepository(database, importer, pipeline, platform)
            repository.initialize()

            val collectionId = repository.createCollection("Journey")
            repository.updateScreen(Destination.HOME) {
                it.copy(collectionId = collectionId, mode = "SHUFFLE", intervalMinutes = 15, enabled = true)
            }
            sourceFiles.forEach { source ->
                val result = importer.importUris(listOf(Uri.fromFile(source)), collectionId) {}
                assertEquals(1, result.imported)
                assertFalse(result.truncated)
            }
            assertEquals(2, repository.dao.collectionAssets(collectionId).size)

            val originalLockId = platform.lockId
            val originalLockPixel = platform.lockPixel
            val homePixels = mutableListOf<Int>()
            repeat(2) {
                val result = repository.next(Destination.HOME)
                assertTrue(result.toString(), result is Outcome.Ok)
                assertEquals(Destination.HOME.flags, platform.writes.last())
                homePixels += platform.homePixel
                assertEquals(originalLockId, platform.lockId)
                assertEquals(originalLockPixel, platform.lockPixel)
            }
            assertNotEquals(homePixels[0], homePixels[1])
            val firstRunScreen = repository.dao.screen(Destination.HOME.name)!!
            assertEquals(15L, firstRunScreen.intervalMinutes)
            assertTrue(firstRunScreen.enabled)
            assertEquals(2, firstRunScreen.queue().recent.size)
            assertNotEquals(firstRunScreen.queue().recent[0], firstRunScreen.queue().recent[1])
            assertTrue(firstRunScreen.queue().pending.isEmpty())
            assertEquals(2, repository.dao.history(Destination.HOME.name).size)
            assertNull(repository.dao.screen(Destination.LOCK.name)!!.currentAssetId)
            assertTrue(platform.homePixel == Color.RED || platform.homePixel == Color.GREEN)

            database.close()
            database = openDatabase(databaseName)
            val reopenedPipeline = BitmapPipeline()
            val reopenedImporter = AssetImporter(context, database, reopenedPipeline)
            val reopened = createRepository(database, reopenedImporter, reopenedPipeline, platform)
            reopened.initialize()

            val persisted = reopened.dao.screen(Destination.HOME.name)!!
            assertEquals(collectionId, persisted.collectionId)
            assertEquals(15L, persisted.intervalMinutes)
            assertTrue(persisted.enabled)
            assertEquals(2, persisted.queue().recent.size)
            assertEquals(2, reopened.dao.history(Destination.HOME.name).size)
            assertEquals(2, reopened.dao.collectionAssets(collectionId).size)

            val beforeRestartedNext = persisted.currentAssetId
            val previousHomePixel = platform.homePixel
            val restartedNext = reopened.next(Destination.HOME)
            assertTrue(restartedNext.toString(), restartedNext is Outcome.Ok)
            assertNotEquals(beforeRestartedNext, reopened.dao.screen(Destination.HOME.name)!!.currentAssetId)
            assertNotEquals(previousHomePixel, platform.homePixel)
            assertEquals(3, reopened.dao.history(Destination.HOME.name).size)
            assertEquals(originalLockId, platform.lockId)
            assertEquals(originalLockPixel, platform.lockPixel)
            assertTrue(platform.writes.all { it == Destination.HOME.flags })
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
            sourceFiles.forEach { it.delete() }
        }
    }

    private fun createRepository(
        database: DexDatabase,
        importer: AssetImporter,
        pipeline: BitmapPipeline,
        platform: RecordingWallpaperPlatform,
    ) = WallRepository(
        context,
        database,
        importer,
        pipeline,
        WallpaperDestinationController(platform),
        Preferences(context),
    )

    private fun openDatabase(name: String) = Room.databaseBuilder(context, DexDatabase::class.java, name)
        .addMigrations(DexDatabase.MIGRATION_1_2, DexDatabase.MIGRATION_2_3)
        .build()

    private fun fixture(name: String, color: Int): File {
        val source = File(context.cacheDir, "$name-${UUID.randomUUID()}")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        try {
            source.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
        } finally {
            bitmap.recycle()
        }
        return source
    }
}

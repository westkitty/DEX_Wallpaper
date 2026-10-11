package com.westkitty.dexwallpaper

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.core.Destination
import com.westkitty.dexwallpaper.core.Outcome
import com.westkitty.dexwallpaper.core.WallpaperSnapshot
import com.westkitty.dexwallpaper.data.AssetEntity
import com.westkitty.dexwallpaper.data.AssetImporter
import com.westkitty.dexwallpaper.data.CollectionEntity
import com.westkitty.dexwallpaper.data.CollectionTransferMode
import com.westkitty.dexwallpaper.data.DexDatabase
import com.westkitty.dexwallpaper.data.Membership
import com.westkitty.dexwallpaper.data.Preferences
import com.westkitty.dexwallpaper.data.WallRepository
import com.westkitty.dexwallpaper.wallpaper.BitmapPipeline
import com.westkitty.dexwallpaper.wallpaper.WallpaperDestinationController
import com.westkitty.dexwallpaper.wallpaper.WallpaperPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Room transaction coverage; compiling this test does not constitute device execution. */
@RunWith(AndroidJUnit4::class)
class CollectionTransferTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun copyAndMoveChangeOnlyMembershipsAndKeepScreenConfiguration() = runBlocking {
        val databaseName = "collection-transfer-${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, DexDatabase::class.java, databaseName)
            .addMigrations(DexDatabase.MIGRATION_1_2, DexDatabase.MIGRATION_2_3)
            .build()
        try {
            val pipeline = BitmapPipeline()
            val importer = AssetImporter(context, database, pipeline)
            val platform = object : WallpaperPlatform {
                override fun snapshot() = WallpaperSnapshot(homeId = 1, lockId = 2)
                override fun write(bitmap: Bitmap, flags: Int) {
                    error("Membership transfer must not write a wallpaper")
                }
            }
            val repository = WallRepository(
                context,
                database,
                importer,
                pipeline,
                WallpaperDestinationController(platform),
                Preferences(context),
            )
            repository.initialize()
            repository.dao.putCollection(CollectionEntity("source", "Source"))
            repository.dao.putCollection(CollectionEntity("target", "Target"))
            repository.dao.insertAsset(AssetEntity("asset", "Artwork", "protected.img", "", "a".repeat(64), 64, 64, 128, 1))
            repository.dao.addMembership(Membership("source", "asset"))
            repository.updateScreen(Destination.HOME) {
                it.copy(collectionId = "source", mode = "SEQUENTIAL", intervalMinutes = 45, enabled = true)
            }

            assertTrue(repository.transferAsset("asset", "source", "target", CollectionTransferMode.COPY) is Outcome.Ok)
            assertEquals(setOf("source", "target"), repository.dao.membershipsForAsset("asset").map { it.collectionId }.toSet())
            assertTrue(repository.transferAsset("asset", "source", "target", CollectionTransferMode.COPY) is Outcome.Ok)
            assertEquals(2, repository.dao.membershipsForAsset("asset").size)

            assertTrue(repository.transferAsset("asset", "source", "target", CollectionTransferMode.MOVE) is Outcome.Ok)
            assertEquals(listOf("target"), repository.dao.membershipsForAsset("asset").map { it.collectionId })
            assertEquals("Artwork", repository.dao.asset("asset")?.name)
            assertTrue(repository.dao.membershipsForAsset("asset").isNotEmpty())

            val home = repository.dao.screen(Destination.HOME.name)!!
            assertEquals("source", home.collectionId)
            assertEquals("SEQUENTIAL", home.mode)
            assertEquals(45L, home.intervalMinutes)
            assertTrue(home.enabled)
            assertFalse(repository.transferAsset("missing", "source", "target", CollectionTransferMode.COPY) is Outcome.Ok)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}

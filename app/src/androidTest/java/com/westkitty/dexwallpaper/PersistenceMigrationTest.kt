package com.westkitty.dexwallpaper

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceMigrationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun realDatabaseReopenPreservesQueuesHistoryAndEdits() = runBlocking {
        val name="persistence-test.db"; context.deleteDatabase(name)
        fun open()=Room.databaseBuilder(context,DexDatabase::class.java,name).addMigrations(DexDatabase.MIGRATION_1_2,DexDatabase.MIGRATION_2_3).build()
        val first=open()
        try {
            first.library().putCollection(CollectionEntity("default","Test"))
            first.library().insertAsset(AssetEntity("asset","Test","asset.img","","0".repeat(64),100,100,1000,0))
            first.library().addMembership(Membership("default","asset"))
            first.library().putScreen(ScreenEntity("HOME",pending="asset",recent="asset",cycle=12))
            first.library().putCollection(CollectionEntity("default","Renamed without deleting members"))
            assertEquals(1,first.library().collectionAssets("default").size)
            first.library().putSource(RemoteSourceEntity("source","Synthetic","https://example.com/art/","WEBDAV"))
            first.library().putEdit(EditEntity("asset",zoom=2f))
            first.library().addReceipt(ReceiptEntity("receipt","asset","HOME","default",100,"Test","SHUFFLE"))
        } finally { first.close() }
        val restored=open()
        try {
            assertEquals(listOf("asset"),restored.library().screen("HOME")!!.queue().pending)
            assertEquals(12L,restored.library().screen("HOME")!!.cycle)
            assertEquals(2f,restored.library().edit("asset")!!.zoom,0f)
            assertEquals("receipt",restored.library().history("HOME").single().id)
            assertEquals(1,restored.library().collectionAssets("default").size)
            assertEquals("WEBDAV",restored.library().source("source")!!.kind)
        } finally { restored.close(); context.deleteDatabase(name) }
    }
    @Test fun additiveVersionOneToThreeMigrationKeepsLibrary() = runBlocking {
        val name="migration-test.db"; context.deleteDatabase(name)
        val fixture=Room.databaseBuilder(context,DexDatabase::class.java,name).build()
        fixture.library().putCollection(CollectionEntity("default","Preserved")); fixture.close()
        // v1 is the unchanged seven core tables; v2 adds scenes/revisions; v3 adds optional remote sources.
        // Remove the additive v2/v3 tables and set the real SQLite version before exercising Room upgrade.
        context.openOrCreateDatabase(name,0,null).use { sqlite ->
            sqlite.execSQL("DROP TABLE scenes"); sqlite.execSQL("DROP TABLE scene_revisions"); sqlite.execSQL("DROP TABLE remote_sources")
            sqlite.execSQL("DELETE FROM room_master_table"); sqlite.version=1
        }
        val upgraded=Room.databaseBuilder(context,DexDatabase::class.java,name).addMigrations(DexDatabase.MIGRATION_1_2,DexDatabase.MIGRATION_2_3).build()
        try { assertEquals("Preserved",upgraded.library().allCollections().single().name); assertTrue(upgraded.library().allScenes().isEmpty()); assertTrue(upgraded.library().allSources().isEmpty()) }
        finally { upgraded.close(); context.deleteDatabase(name) }
    }
}

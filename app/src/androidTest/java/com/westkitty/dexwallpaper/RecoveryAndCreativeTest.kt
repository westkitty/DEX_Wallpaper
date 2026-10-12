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
import kotlinx.coroutines.flow.first
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
    @Test fun reimportOfCorruptButUnquarantinedImageNeverOverwritesOldBytes() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val wall=repository(db); wall.initialize()
        val source=File(context.cacheDir,"reimport-corrupt-unquarantined-fixture.png")
        val bitmap=Bitmap.createBitmap(60,60,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        try {
            assertEquals(1,wall.importer.importUris(listOf(Uri.fromFile(source)),"default") {}.imported)
            val before=wall.dao.allAssets().single()
            val original=wall.importer.file(before)
            val corrupt=original.readBytes().also { it[0]=(it[0].toInt() xor 0xFF).toByte() }
            original.writeBytes(corrupt)
            assertFalse(before.quarantined)
            val result=wall.importer.importUris(listOf(Uri.fromFile(source)),"default") {}
            assertEquals(1,result.duplicates)
            val after=wall.dao.allAssets().single()
            assertEquals(before.id,after.id)
            assertNotEquals(before.relativePath,after.relativePath)
            assertFalse(after.quarantined)
            assertArrayEquals(corrupt,original.readBytes())
            assertArrayEquals(source.readBytes(),wall.importer.file(after).readBytes())
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
    @Test fun restoreDoesNotEraseExistingFolderLinkArtworkMetadataOrEdits() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-merge-identity-fixture.zip")
        try {
            source.initialize(); target.initialize()
            CreativeRepository(context,source).saveGenerated(Composition(seed=733),"default",160,100)
            val backupAsset=source.dao.allAssets().single()
            source.dao.updateAsset(backupAsset.copy(name="From backup",tags="backup",favorite=true))
            source.dao.putEdit(EditEntity(backupAsset.id,zoom=2f))
            source.dao.putCollection(CollectionEntity("default","Backup collection",pinned=false))

            val importResult=target.importer.importUris(listOf(Uri.fromFile(source.importer.file(backupAsset))),"default") {}
            assertEquals(1,importResult.imported)
            val existing=target.dao.allAssets().single()
            target.dao.updateAsset(existing.copy(name="Keep local name",tags="keep",favorite=false))
            target.dao.putEdit(EditEntity(existing.id,zoom=3f))
            val folder=CollectionEntity("default","Keep local collection","content://picker/tree/user-library",pinned=true)
            target.dao.putCollection(folder)

            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),true) {}
            assertEquals(1,BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {})
            assertEquals(folder,target.dao.allCollections().single { it.id=="default" })
            val after=target.dao.allAssets().single()
            assertEquals(existing.id,after.id)
            assertEquals("Keep local name",after.name)
            assertEquals("keep",after.tags)
            assertFalse(after.favorite)
            assertEquals(3f,target.dao.edit(existing.id)!!.zoom,0f)
            assertTrue(target.importer.file(after).isFile)
            assertTrue(target.dao.screen("HOME")!!.paused)
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
    }

    @Test fun metadataOnlyRestoreNeverSilentlyUnquarantinesArtwork() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-metadata-only-fixture.zip")
        try {
            source.initialize(); target.initialize()
            CreativeRepository(context,source).saveGenerated(Composition(seed=734),"default",160,100)
            val fromBackup=source.dao.allAssets().single()
            val imported=target.importer.importUris(listOf(Uri.fromFile(source.importer.file(fromBackup))),"default") {}
            assertEquals(1,imported.imported)
            val existing=target.dao.allAssets().single()
            target.dao.updateAsset(existing.copy(quarantined=true))

            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),false) {}
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}
            assertTrue(target.dao.asset(existing.id)!!.quarantined)
            assertTrue(target.importer.file(existing).isFile)
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
    }

    @Test fun backupRestorePreservesConfiguredDestinationsAndWorkspace() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-screen-merge-fixture.zip")
        try {
            source.initialize(); target.initialize()
            source.dao.putScreen(ScreenEntity("HOME",mode="SEQUENTIAL",intervalMinutes=180))
            source.dao.putScreen(ScreenEntity("LOCK",mode="SEQUENTIAL",intervalMinutes=240))
            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),false) {}

            val locallyConfigured=ScreenEntity("HOME",mode="RANDOM",intervalMinutes=45,enabled=true,paused=false)
            target.dao.putScreen(locallyConfigured)
            target.preferences.flag("reducedMotion",true)
            target.preferences.flag("safeMode",true)
            val userPreferences=target.preferences.flow.first()
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}

            assertEquals(locallyConfigured,target.dao.screen("HOME"))
            val lock=target.dao.screen("LOCK")!!
            assertEquals("SEQUENTIAL",lock.mode)
            assertEquals(240L,lock.intervalMinutes)
            assertFalse(lock.enabled)
            assertTrue(lock.paused)
            val settings=target.preferences.flow.first()
            assertTrue(settings.reducedMotion)
            assertTrue(settings.safeMode)
            assertEquals(userPreferences,settings)
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
    }

    @Test fun restoreRepairsTamperedUnquarantinedOriginalWithoutDeletingOldBytes() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-tampered-original-fixture.zip")
        try {
            source.initialize(); target.initialize()
            CreativeRepository(context,source).saveGenerated(Composition(seed=817),"default",160,100)
            val asset=source.dao.allAssets().single()
            val imported=target.importer.importUris(listOf(Uri.fromFile(source.importer.file(asset))),"default") {}
            assertEquals(1,imported.imported)
            val current=target.dao.allAssets().single()
            val oldFile=target.importer.file(current)
            val corrupted=oldFile.readBytes().also { it[0]=(it[0].toInt() xor 0xFF).toByte() }
            oldFile.writeBytes(corrupted)
            assertFalse(current.quarantined)

            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),true) {}
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}
            val after=target.dao.asset(current.id)!!
            assertEquals(current.id,after.id)
            assertFalse(after.quarantined)
            assertNotEquals(current.relativePath,after.relativePath)
            assertArrayEquals(corrupted,oldFile.readBytes())
            assertArrayEquals(source.importer.file(asset).readBytes(),target.importer.file(after).readBytes())
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
    }

    @Test fun backupImportNeverReplacesSameIdRulesOrScenes() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-rule-scene-collision-fixture.zip")
        try {
            source.initialize(); target.initialize()
            source.dao.putRule(RuleEntity("rule_conflict","Imported Friday night","default",30,enabled=true))
            target.dao.putRule(RuleEntity("rule_conflict","Existing user rule","default",99,enabled=true))
            val importedScene=SceneCodec.encode(SceneManifest(title="Imported scene"))
            val existingScene=SceneCodec.encode(SceneManifest(title="Existing scene"))
            source.dao.putScene(SceneEntity("scene_conflict","Imported scene",importedScene,20))
            target.dao.putScene(SceneEntity("scene_conflict","Existing scene",existingScene,10))

            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),false) {}
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}

            val rules=target.dao.allRules()
            assertEquals(2,rules.size)
            assertEquals("Existing user rule",rules.single { it.id=="rule_conflict" }.name)
            assertTrue(rules.single { it.id=="rule_conflict" }.enabled)
            val importedRule=rules.single { it.id!="rule_conflict" }
            assertEquals("Imported Friday night",importedRule.name)
            assertFalse(importedRule.enabled)
            val scenes=target.dao.allScenes()
            assertEquals(2,scenes.size)
            assertEquals(existingScene,scenes.single { it.id=="scene_conflict" }.manifest)
            assertEquals(importedScene,scenes.single { it.id!="scene_conflict" }.manifest)
            // Re-importing the same backup must not produce additional disabled duplicates.
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}
            assertEquals(2,target.dao.allRules().size)
            assertEquals(2,target.dao.allScenes().size)
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
    }

    @Test fun repairingQuarantinedOriginalNeverOverwritesItsOldBytes() = runBlocking {
        val sourceDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val targetDb=Room.inMemoryDatabaseBuilder(context,DexDatabase::class.java).build()
        val source=repository(sourceDb); val target=repository(targetDb)
        val archive=File(context.cacheDir,"restore-cow-original-fixture.zip")
        try {
            source.initialize(); target.initialize()
            CreativeRepository(context,source).saveGenerated(Composition(seed=736),"default",160,100)
            val fromBackup=source.dao.allAssets().single()
            val result=target.importer.importUris(listOf(Uri.fromFile(source.importer.file(fromBackup))),"default") {}
            assertEquals(1,result.imported)
            val local=target.dao.allAssets().single()
            val oldFile=target.importer.file(local)
            oldFile.writeText("original user bytes retained during repair")
            val oldBytes=oldFile.readBytes()
            target.dao.updateAsset(local.copy(quarantined=true))

            BackupRestoreCoordinator(context,source).export(Uri.fromFile(archive),true) {}
            BackupRestoreCoordinator(context,target).restore(Uri.fromFile(archive)) {}

            val after=target.dao.asset(local.id)!!
            assertFalse(after.quarantined)
            assertEquals(local.id,after.id)
            assertNotEquals(local.relativePath,after.relativePath)
            assertArrayEquals(oldBytes,oldFile.readBytes())
            val restored=target.importer.file(after)
            assertTrue(restored.isFile)
            assertArrayEquals(source.importer.file(fromBackup).readBytes(),restored.readBytes())
        } finally { archive.delete(); sourceDb.close(); targetDb.close() }
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

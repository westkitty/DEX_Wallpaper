package com.westkitty.dexwallpaper.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.westkitty.dexwallpaper.core.*

@Entity(tableName = "assets", indices = [Index(value = ["sha256"], unique = true)])
data class AssetEntity(@PrimaryKey val id: String, val name: String, val relativePath: String, val sourceUri: String,
    val sha256: String, val width: Int, val height: Int, val bytes: Long, val importedAt: Long,
    val favorite: Boolean = false, val excluded: Boolean = false, val quarantined: Boolean = false,
    val tags: String = "", val perceptualHash: Long = 0, val attribution: String = "", val license: String = "User-provided; not redistributed")
@Entity(tableName = "collections")
data class CollectionEntity(@PrimaryKey val id: String, val name: String, val sourceTree: String = "", val pinned: Boolean = false)
@Entity(tableName = "membership", primaryKeys = ["collectionId", "assetId"],
    foreignKeys = [ForeignKey(entity = CollectionEntity::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AssetEntity::class, parentColumns = ["id"], childColumns = ["assetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("assetId")])
data class Membership(val collectionId: String, val assetId: String)
@Entity(tableName = "edits", foreignKeys = [ForeignKey(entity = AssetEntity::class, parentColumns = ["id"], childColumns = ["assetId"], onDelete = ForeignKey.CASCADE)])
data class EditEntity(@PrimaryKey val assetId: String, val scale: String = "FILL", val zoom: Float = 1f,
    val focalX: Float = .5f, val focalY: Float = .5f, val portraitX: Float = .5f, val portraitY: Float = .5f,
    val brightness: Float = 0f, val contrast: Float = 1f, val saturation: Float = 1f, val dim: Float = 0f,
    val vignette: Float = 0f, val blur: Float = 0f, val safeTop: Float = 0f, val safeBottom: Float = 0f) {
    fun model() = ImageEdit(scale = ScaleMode.entries.firstOrNull { it.name == scale } ?: ScaleMode.FILL, zoom = zoom, focalX = focalX, focalY = focalY,
        portraitX = portraitX, portraitY = portraitY, brightness = brightness, contrast = contrast, saturation = saturation, dim = dim, vignette = vignette,
        blur = blur, safeTop = safeTop, safeBottom = safeBottom).bounded()
    companion object { fun from(id: String, e: ImageEdit) = e.bounded().let { EditEntity(id, it.scale.name, it.zoom, it.focalX, it.focalY, it.portraitX,
        it.portraitY, it.brightness, it.contrast, it.saturation, it.dim, it.vignette, it.blur, it.safeTop, it.safeBottom) } }
}
@Entity(tableName = "screens")
data class ScreenEntity(@PrimaryKey val destination: String, val collectionId: String = "default", val mode: String = "SHUFFLE",
    val intervalMinutes: Long = 60, val enabled: Boolean = false, val paused: Boolean = false, val favoritesOnly: Boolean = false,
    val minimumSeparation: Int = 1, val currentAssetId: String? = null, val lastSuccess: Long = 0, val nextDue: Long = 0,
    val pending: String = "", val recent: String = "", val cycle: Long = 0, val manualUntil: Long = 0, val error: String = "") {
    fun queue() = QueueState(pending.split('|').filter { it.isNotEmpty() }, recent.split('|').filter { it.isNotEmpty() }, cycle)
}
@Entity(tableName = "receipts", indices = [Index("destination")])
data class ReceiptEntity(@PrimaryKey val id: String, val assetId: String, val destination: String, val collectionId: String,
    val timestamp: Long, val reason: String, val selectionMode: String, val ruleId: String = "")
@Entity(tableName = "rules")
data class RuleEntity(@PrimaryKey val id: String, val name: String, val collectionId: String, val priority: Int,
    val enabled: Boolean = true, val startMinute: Int = 0, val endMinute: Int = 1439,
    val days: String = "1,2,3,4,5,6,7", val charging: Int = -1, val darkTheme: Int = -1, val minimumBattery: Int = 0,
    val maximumLux: Float? = null, val destination: String = "HOME") {
    fun model() = ContextRule(id, name, collectionId, priority, enabled,
        java.time.LocalTime.of(startMinute.coerceIn(0,1439) / 60, startMinute.coerceIn(0,1439) % 60),
        java.time.LocalTime.of(endMinute.coerceIn(0,1439) / 60, endMinute.coerceIn(0,1439) % 60),
        days.split(',').mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 1..7 }?.let(java.time.DayOfWeek::of) }.toSet(),
        if (charging < 0) null else charging == 1, if (darkTheme < 0) null else darkTheme == 1, minimumBattery, maximumLux)
}

@Entity(tableName = "scenes")
data class SceneEntity(@PrimaryKey val id: String, val name: String, val manifest: String, val updatedAt: Long, val revision: Int = 1)
@Entity(tableName = "scene_revisions", indices = [Index("sceneId")])
data class SceneRevisionEntity(@PrimaryKey val id: String, val sceneId: String, val manifest: String, val createdAt: Long, val revision: Int)

@Entity(tableName = "remote_sources")
data class RemoteSourceEntity(@PrimaryKey val id: String, val name: String, val endpoint: String, val kind: String,
    val collectionId: String = "default", val wifiOnly: Boolean = true, val allowMetered: Boolean = false, val budgetMb: Int = 32,
    val allowPrivateNetwork: Boolean = false, val enabled: Boolean = true, val lastSuccess: Long = 0, val status: String = "Not synchronized")

@Dao
interface LibraryDao {
    @Query("SELECT * FROM assets ORDER BY importedAt DESC") fun observeAssets(): Flow<List<AssetEntity>>
    @Query("SELECT * FROM assets ORDER BY importedAt DESC") suspend fun allAssets(): List<AssetEntity>
    @Query("SELECT * FROM assets WHERE id = :id") suspend fun asset(id: String): AssetEntity?
    @Query("SELECT * FROM assets WHERE sha256 = :hash") suspend fun byHash(hash: String): AssetEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAsset(asset: AssetEntity)
    @Update suspend fun updateAsset(asset: AssetEntity)
    @Query("SELECT * FROM collections") fun observeCollections(): Flow<List<CollectionEntity>>
    @Query("SELECT * FROM collections ORDER BY name") suspend fun allCollections(): List<CollectionEntity>
    @Upsert suspend fun putCollection(collection: CollectionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addMembership(membership: Membership)
    @Query("DELETE FROM membership WHERE collectionId = :collection AND assetId = :asset") suspend fun removeMembership(collection: String, asset: String)
    @Query("SELECT a.* FROM assets a INNER JOIN membership m ON m.assetId = a.id WHERE m.collectionId = :collection ORDER BY a.name, a.id") suspend fun collectionAssets(collection: String): List<AssetEntity>
    @Query("SELECT * FROM membership") fun observeMemberships(): Flow<List<Membership>>
    @Query("SELECT * FROM membership") suspend fun memberships(): List<Membership>
    @Query("SELECT * FROM edits WHERE assetId = :id") suspend fun edit(id: String): EditEntity?
    @Query("SELECT * FROM edits") suspend fun allEdits(): List<EditEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEdit(edit: EditEntity)
    @Query("SELECT * FROM screens ORDER BY destination") fun observeScreens(): Flow<List<ScreenEntity>>
    @Query("SELECT * FROM screens WHERE destination = :target") suspend fun screen(target: String): ScreenEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putScreen(screen: ScreenEntity)
    @Query("SELECT * FROM receipts ORDER BY timestamp DESC LIMIT 200") fun observeHistory(): Flow<List<ReceiptEntity>>
    @Query("SELECT * FROM receipts WHERE destination = :destination ORDER BY timestamp DESC LIMIT 200") suspend fun history(destination: String): List<ReceiptEntity>
    @Insert suspend fun addReceipt(receipt: ReceiptEntity)
    @Query("DELETE FROM receipts WHERE id NOT IN (SELECT id FROM receipts ORDER BY timestamp DESC LIMIT 500)") suspend fun trimHistory()
    @Query("SELECT * FROM rules ORDER BY priority DESC, id") fun observeRules(): Flow<List<RuleEntity>>
    @Query("SELECT * FROM rules ORDER BY priority DESC, id") suspend fun allRules(): List<RuleEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRule(rule: RuleEntity)
    @Query("DELETE FROM rules WHERE id = :id") suspend fun deleteRule(id: String)
    @Query("SELECT * FROM scenes ORDER BY updatedAt DESC") fun observeScenes(): Flow<List<SceneEntity>>
    @Query("SELECT * FROM scenes ORDER BY updatedAt DESC") suspend fun allScenes(): List<SceneEntity>
    @Query("SELECT * FROM scenes WHERE id = :id") suspend fun scene(id: String): SceneEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putScene(scene: SceneEntity)
    @Insert suspend fun addRevision(revision: SceneRevisionEntity)
    @Query("SELECT * FROM scene_revisions WHERE sceneId = :id ORDER BY createdAt DESC LIMIT 20") suspend fun revisions(id: String): List<SceneRevisionEntity>
    @Query("SELECT * FROM remote_sources ORDER BY name") fun observeSources(): Flow<List<RemoteSourceEntity>>
    @Query("SELECT * FROM remote_sources ORDER BY name") suspend fun allSources(): List<RemoteSourceEntity>
    @Query("SELECT * FROM remote_sources WHERE id = :id") suspend fun source(id: String): RemoteSourceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSource(source: RemoteSourceEntity)
    @Query("DELETE FROM remote_sources WHERE id = :id") suspend fun deleteSource(id: String)
    @Query("DELETE FROM scene_revisions WHERE sceneId = :sceneId AND id NOT IN (SELECT id FROM scene_revisions WHERE sceneId = :sceneId ORDER BY createdAt DESC LIMIT 20)") suspend fun trimRevisions(sceneId: String)
}

@Database(entities = [AssetEntity::class, CollectionEntity::class, Membership::class, EditEntity::class, ScreenEntity::class, ReceiptEntity::class, RuleEntity::class, SceneEntity::class, SceneRevisionEntity::class, RemoteSourceEntity::class], version = 3, exportSchema = true)
abstract class DexDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `scenes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `manifest` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `scene_revisions` (`id` TEXT NOT NULL, `sceneId` TEXT NOT NULL, `manifest` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scene_revisions_sceneId` ON `scene_revisions` (`sceneId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `remote_sources` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `endpoint` TEXT NOT NULL, `kind` TEXT NOT NULL, `collectionId` TEXT NOT NULL, `wifiOnly` INTEGER NOT NULL, `allowMetered` INTEGER NOT NULL, `budgetMb` INTEGER NOT NULL, `allowPrivateNetwork` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `lastSuccess` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        fun open(context: Context) = Room.databaseBuilder(context, DexDatabase::class.java, "living-wall.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}

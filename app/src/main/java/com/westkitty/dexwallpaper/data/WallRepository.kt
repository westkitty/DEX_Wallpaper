package com.westkitty.dexwallpaper.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.content.res.Configuration
import androidx.room.withTransaction
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.wallpaper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.UUID

/** One writer for manual, scheduled, widget, tile, and external wallpaper actions. */
class WallRepository(private val context: Context, val db: DexDatabase, val importer: AssetImporter,
    val pipeline: BitmapPipeline, private val controller: WallpaperDestinationController, val preferences: Preferences) {
    private val writeMutex = Mutex()
    val dao = db.library()
    suspend fun <T> exclusive(action: suspend () -> T): T = writeMutex.withLock { action() }
    suspend fun initialize() {
        db.withTransaction {
            if (dao.allCollections().none { it.id == "default" }) dao.putCollection(CollectionEntity("default", "Personal library"))
            Destination.entries.forEach { if (dao.screen(it.name) == null) dao.putScreen(ScreenEntity(it.name)) }
        }
    }
    fun contextSnapshot(): ContextSnapshot {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        return ContextSnapshot(ZonedDateTime.now(), status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            (level * 100 / scale).coerceIn(0,100), context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    }
    suspend fun next(destination: Destination, automatic: Boolean = false): Outcome<String> = writeMutex.withLock {
        val screen = dao.screen(destination.name) ?: ScreenEntity(destination.name)
        val now = System.currentTimeMillis()
        if (automatic) {
            if (!screen.enabled || screen.paused) return@withLock Outcome.Error("Rotation is paused.")
            if (screen.manualUntil > now) return@withLock Outcome.Error("Manual override is active until ${screen.manualUntil}.")
            val power = context.getSystemService(PowerManager::class.java)
            if (power.isPowerSaveMode || !power.isInteractive) return@withLock Outcome.Error("Rotation deferred: Battery Saver or screen off.")
        }
        val decision = Chronoscape.evaluate(dao.allRules().filter { it.destination == destination.name }.map { it.model() }, contextSnapshot())
        val collection = if (automatic) decision.winner?.collectionId ?: screen.collectionId else screen.collectionId
        val assets = dao.collectionAssets(collection)
        val candidates = assets.map { Candidate(it.id, it.favorite, it.excluded, !it.quarantined && importer.file(it).isFile) }
        val mode = RotationMode.entries.firstOrNull { it.name == screen.mode } ?: RotationMode.SHUFFLE
        val state = if (collection == screen.collectionId) screen.queue() else QueueState(recent = screen.queue().recent, cycle = screen.cycle)
        when (val selection = ShuffleGuard.next(candidates, state, mode, destination.ordinal.toLong() + screen.lastSuccess, screen.minimumSeparation, screen.favoritesOnly)) {
            is Outcome.Error -> selection
            is Outcome.Ok -> applyLocked(selection.value.assetId, destination, screen, selection.value,
                collection, if (automatic) "Scheduled · ${decision.winner?.name ?: "default collection"}" else "Manual next", if (automatic) decision.winner?.id ?: "" else "")
        }
    }
    suspend fun apply(assetId: String, destination: Destination, overrideMinutes: Int = 0): Outcome<String> = writeMutex.withLock {
        val screen = dao.screen(destination.name) ?: ScreenEntity(destination.name)
        applyLocked(assetId, destination, screen.copy(manualUntil = System.currentTimeMillis() + overrideMinutes.coerceIn(0,1440) * 60_000L), null,
            screen.collectionId, "Manual selection", "")
    }
    suspend fun previous(destination: Destination): Outcome<String> = writeMutex.withLock {
        val screen = dao.screen(destination.name) ?: ScreenEntity(destination.name)
        val previous = dao.history(destination.name).firstOrNull { it.assetId != screen.currentAssetId }
            ?: return@withLock Outcome.Error("No earlier wallpaper for this destination.")
        applyLocked(previous.assetId, destination, screen, null, previous.collectionId, "Previous wallpaper", "")
    }
    private suspend fun applyLocked(assetId: String, destination: Destination, screen: ScreenEntity, selection: Selection?, collection: String, reason: String, ruleId: String): Outcome<String> = withContext(Dispatchers.IO) {
        val asset = dao.asset(assetId) ?: return@withContext Outcome.Error("Artwork is no longer in the library.")
        if (asset.quarantined || !importer.file(asset).isFile) {
            dao.updateAsset(asset.copy(quarantined = true))
            return@withContext Outcome.Error("Original is missing or quarantined. Re-import the source folder to recover it.")
        }
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceIn(480, 4096)
        val height = metrics.heightPixels.coerceIn(480, 4096)
        val prefs = preferences.flow.first()
        val bitmap = try { pipeline.render(importer.file(asset), width, height, dao.edit(assetId)?.model() ?: ImageEdit(), prefs.safeMode) }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            dao.updateAsset(asset.copy(quarantined = true))
            return@withContext Outcome.Error("Artwork decoding failed; quarantined without deleting the original.")
        }
        val outcome = try { controller.apply(bitmap, destination) } finally { bitmap.recycle() }
        when (outcome) {
            is Outcome.Error -> {
                dao.putScreen(screen.copy(error = outcome.message, enabled = if (outcome.recoverable) screen.enabled else false))
                outcome
            }
            is Outcome.Ok -> {
                val now = System.currentTimeMillis()
                val queue = selection?.queue ?: screen.queue()
                val receipt = "$reason · ${selection?.reason ?: "explicit artwork"}"
                db.withTransaction {
                    dao.putScreen(screen.copy(currentAssetId = assetId, lastSuccess = now,
                        nextDue = ScheduleMath.nextInterval(now, now, screen.intervalMinutes), pending = queue.pending.joinToString("|"),
                        recent = queue.recent.joinToString("|"), cycle = queue.cycle, error = ""))
                    dao.addReceipt(ReceiptEntity(UUID.randomUUID().toString(), assetId, destination.name, collection, now, receipt, screen.mode, ruleId))
                    if (destination == Destination.BOTH) {
                        listOf(Destination.HOME, Destination.LOCK).forEach { target ->
                            val separate = dao.screen(target.name) ?: ScreenEntity(target.name)
                            dao.putScreen(separate.copy(currentAssetId = assetId, lastSuccess = now, error = ""))
                            dao.addReceipt(ReceiptEntity(UUID.randomUUID().toString(), assetId, target.name, collection, now, "Explicit Both-screen application", screen.mode))
                        }
                    }
                    dao.trimHistory()
                }
                Outcome.Ok("${asset.name} applied to ${destination.label.lowercase()}.")
            }
        }
    }
    suspend fun transferAsset(
        assetId: String,
        sourceCollectionId: String,
        targetCollectionId: String,
        mode: CollectionTransferMode,
    ): Outcome<String> = db.withTransaction {
        val collections = dao.allCollections()
        val asset = dao.asset(assetId)
        val memberships = dao.membershipsForAsset(assetId).mapTo(mutableSetOf()) { it.collectionId }
        val plan = when (val decision = CollectionTransferPolicy.plan(
            assetId = assetId,
            sourceCollectionId = sourceCollectionId,
            targetCollectionId = targetCollectionId,
            existingCollectionIds = collections.mapTo(mutableSetOf()) { it.id },
            assetExists = asset != null,
            membershipCollectionIds = memberships,
            mode = mode,
        )) {
            is Outcome.Ok -> decision.value
            is Outcome.Error -> return@withTransaction decision
        }
        val target = collections.first { it.id == targetCollectionId }
        if (plan.addTargetMembership) dao.addMembership(Membership(targetCollectionId, assetId))
        if (plan.removeSourceMembership) dao.removeMembership(sourceCollectionId, assetId)

        val name = asset?.name ?: "Artwork"
        val message = when {
            mode == CollectionTransferMode.COPY && !plan.changesMemberships -> "$name is already linked to ${target.name}."
            mode == CollectionTransferMode.COPY -> "Copied $name to ${target.name}; the source collection is unchanged."
            else -> "Moved $name to ${target.name}; the protected original and per-screen settings are unchanged."
        }
        Outcome.Ok(message)
    }

    suspend fun createCollection(name: String): String {
        require(name.isNotBlank()) { "Collection needs a name" }
        val id = UUID.randomUUID().toString()
        dao.putCollection(CollectionEntity(id, name.trim().take(100)))
        return id
    }
    suspend fun updateScreen(target: Destination, transform: (ScreenEntity) -> ScreenEntity) = writeMutex.withLock {
        val old = dao.screen(target.name) ?: ScreenEntity(target.name)
        val updated = transform(old)
        dao.putScreen(updated.copy(intervalMinutes = updated.intervalMinutes.coerceIn(15, 10080), enabled = updated.enabled && target != Destination.BOTH))
    }
}

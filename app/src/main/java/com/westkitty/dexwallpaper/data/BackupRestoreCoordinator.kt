package com.westkitty.dexwallpaper.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.westkitty.dexwallpaper.core.SceneCodec
import com.westkitty.dexwallpaper.core.BackupMergePolicy
import com.westkitty.dexwallpaper.core.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class BackupRestoreCoordinator(private val context: Context, private val wall: WallRepository) {
    suspend fun snapshot(): BackupSnapshot = wall.db.withTransaction {
        BackupSnapshot(wall.dao.allAssets(),wall.dao.allCollections(),wall.dao.memberships(),wall.dao.allEdits(),
            listOfNotNull(wall.dao.screen("HOME"),wall.dao.screen("LOCK"),wall.dao.screen("BOTH")),wall.dao.allRules(),wall.dao.allScenes(),wall.preferences.flow.first())
    }
    suspend fun export(uri: Uri, includeOriginals: Boolean, progress: (String)->Unit): Int = withContext(Dispatchers.IO) {
        val data=snapshot()
        require(data.assets.sumOf { it.bytes } <= 1024L*1024*1024 || !includeOriginals) { "Backup originals exceed the 1GB export budget" }
        context.contentResolver.openOutputStream(uri,"wt").use { output ->
            requireNotNull(output) { "Backup destination is not writable" }
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(BackupCodec.encode(data).toByteArray()); zip.closeEntry()
                if(includeOriginals) data.assets.forEachIndexed { n,asset ->
                    currentCoroutineContext().ensureActive()
                    val file=wall.importer.file(asset)
                    if (OriginalIntegrity.eligibleForBackup(file, asset.sha256, asset.bytes, asset.quarantined)) {
                        zip.putNextEntry(ZipEntry("assets/${asset.id}.img"))
                        file.inputStream().use { it.copyTo(zip,64*1024) }
                        zip.closeEntry()
                        progress("Backing up ${n+1}/${data.assets.size} images")
                    } else {
                        // Keep metadata even when its original cannot be trusted; restore
                        // will quarantine that row rather than accepting altered bytes.
                        progress("Image ${n+1}/${data.assets.size}: original absent, changed or quarantined; metadata only")
                    }
                }
            }
        }
        data.assets.size
    }
    suspend fun restore(uri: Uri, progress: (String)->Unit): Int = withContext(Dispatchers.IO) {
        wall.exclusive {
        val stage=File(context.cacheDir,"restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var manifest: String?=null; var expanded=0L; var entries=0
            val assetFiles=mutableMapOf<String,File>()
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Backup cannot be read" }
                ZipInputStream(input).use { zip ->
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val entry=zip.nextEntry ?: break
                        require(++entries <= 6000 && !entry.isDirectory) { "Unsupported or oversized archive" }
                        val name=entry.name
                        require(name=="manifest.json" || name.matches(Regex("assets/[A-Za-z0-9_-]{1,80}\\.img"))) { "Untrusted archive path" }
                        val file=File(stage,if(name=="manifest.json") name else name.substringAfter('/'))
                        require(!file.exists()) { "Duplicate archive entry" }
                        val limit=if(name=="manifest.json") BackupCodec.MAX_MANIFEST_BYTES.toLong() else 80L*1024*1024
                        var size=0L
                        file.outputStream().use { out -> val buffer=ByteArray(64*1024); while(true) { val n=zip.read(buffer); if(n<0) break; size+=n; expanded+=n
                            require(size<=limit && expanded<=1024L*1024*1024) { "Archive expanded-size budget exceeded" }; out.write(buffer,0,n) } }
                        if(name=="manifest.json") manifest=file.readText() else assetFiles[name.substringAfter('/').removeSuffix(".img")]=file
                        zip.closeEntry(); progress("Verifying archive entry $entries")
                    }
                }
            }
            val data=BackupCodec.decode(requireNotNull(manifest) { "Backup manifest missing" })
            require(assetFiles.keys.all { id -> data.assets.any { it.id==id } }) { "Unreferenced archive assets" }
            // Validate every included original BEFORE making persistent changes.
            assetFiles.forEach { (id,file) ->
                val expected=data.assets.first { it.id==id }
                require(OriginalIntegrity.matches(file,expected.sha256,expected.bytes)) { "Backup image hash or length mismatch" }
                wall.pipeline.decode(file,160,160).also { it.recycle() }
            }
            val mapping=mutableMapOf<String,String>()
            val prepared=mutableListOf<AssetEntity>()
            // Files are copy-on-write: restore never replaces an existing original. A DB
            // failure removes only the newly installed files; the old paths remain intact.
            val fileJournal=RestoreFileJournal()
            try {
                data.assets.forEach { a ->
                    currentCoroutineContext().ensureActive()
                    val sameHash=wall.dao.byHash(a.sha256)
                    val id=sameHash?.id ?: if(wall.dao.asset(a.id)==null) a.id else UUID.randomUUID().toString()
                    mapping[a.id]=id
                    // A file merely existing is NOT proof that the local original is
                    // intact. Check the actual bytes before retaining it over a known-good
                    // backup copy; preserve corrupt bytes at their old path for recovery.
                    val localFile=sameHash?.let { wall.importer.file(it) }
                    val localHealthy=sameHash!=null && !sameHash.quarantined && localFile!=null &&
                        OriginalIntegrity.matches(localFile,a.sha256,a.bytes)
                    val action=BackupMergePolicy.originalAction(
                        sameHash!=null, !localHealthy, localFile?.isFile==true, assetFiles.containsKey(a.id))
                    var relativePath=sameHash?.relativePath ?: "$id.img"
                    if(action==BackupMergePolicy.OriginalAction.INSTALL_VERIFIED) {
                        val original=requireNotNull(assetFiles[a.id])
                        // A fresh opaque filename keeps a quarantined/corrupt original
                        // untouched until the DB transaction can point at the replacement.
                        val freshName="${UUID.randomUUID()}.img"
                        val target=wall.importer.file(a.copy(id=id,relativePath=freshName))
                        fileJournal.installVerified(original,target)
                        relativePath=freshName
                    }
                    val ready=action!=BackupMergePolicy.OriginalAction.QUARANTINE
                    prepared+=sameHash?.copy(relativePath=relativePath,quarantined=!ready)
                        ?: a.copy(id=id,relativePath=relativePath,quarantined=!ready,sourceUri="")
                }
                wall.db.withTransaction {
                    // Preserve existing collections, including user-selected SAF tree grants.
                    val currentCollections=wall.dao.allCollections().map { it.id }.toSet()
                    data.collections.forEach { c ->
                        if(c.id !in currentCollections) wall.dao.putCollection(c.copy(sourceTree=""))
                    }
                    prepared.forEach { a -> if(wall.dao.asset(a.id)==null) wall.dao.insertAsset(a) else wall.dao.updateAsset(a) }
                    data.memberships.forEach { wall.dao.addMembership(it.copy(assetId=mapping.getValue(it.assetId))) }
                    data.edits.forEach { e ->
                        val targetId=mapping.getValue(e.assetId)
                        if(wall.dao.edit(targetId)==null) wall.dao.putEdit(e.copy(assetId=targetId))
                    }
                    // Wallpaper destinations are user-owned configuration, not metadata
                    // to replace unconditionally. Only untouched defaults may receive
                    // imported settings; imported schedules remain disabled and paused.
                    data.screens.forEach { s ->
                        val current=wall.dao.screen(s.destination)
                        if(current==null || current==ScreenEntity(s.destination)) {
                            fun mapped(queue: String)=queue.split('|').mapNotNull { mapping[it] }.distinct().joinToString("|")
                            wall.dao.putScreen(s.copy(currentAssetId=s.currentAssetId?.let { mapping[it] },pending=mapped(s.pending),recent=mapped(s.recent),enabled=false,paused=true))
                        }
                    }
                    val knownRules=wall.dao.allRules().toMutableList()
                    val ruleIds=knownRules.mapTo(mutableSetOf()) { it.id }
                    data.rules.forEach { rule ->
                        val imported=rule.copy(enabled=false)
                        // Repeated imports of the same rule are idempotent even when its
                        // original ID collided and had to be remapped during the first import.
                        if(knownRules.none { it.copy(id="",enabled=false)==imported.copy(id="",enabled=false) }) {
                            val id=BackupMergePolicy.distinctImportedId(rule.id,ruleIds) { UUID.randomUUID().toString() }
                            val row=imported.copy(id=id)
                            wall.dao.putRule(row)
                            knownRules+=row
                        }
                    }
                    val knownScenes=wall.dao.allScenes().toMutableList()
                    val sceneIds=knownScenes.mapTo(mutableSetOf()) { it.id }
                    data.scenes.forEach { s ->
                        val scene=(SceneCodec.decode(s.manifest) as Outcome.Ok).value
                        val manifest=SceneCodec.encode(scene.copy(layers=scene.layers.map { it.copy(assetId=mapping[it.assetId] ?: it.assetId) }))
                        val imported=s.copy(manifest=manifest)
                        if(knownScenes.none { it.copy(id="")==imported.copy(id="") }) {
                            val id=BackupMergePolicy.distinctImportedId(s.id,sceneIds) { UUID.randomUUID().toString() }
                            val row=imported.copy(id=id)
                            wall.dao.putScene(row)
                            knownScenes+=row
                        }
                    }
                }
                fileJournal.committed()
            } finally {
                // Cancellation/Room rollback removes newly created original files only.
                fileJournal.rollbackIfUncommitted()
            }
            // Import visual preferences only into a pristine workspace. An already
            // configured tablet keeps its accessibility, performance, preview/live,
            // and security preferences. Never import external-command privileges.
            val localPreferences=wall.preferences.flow.first()
            if(localPreferences==WorkspacePreferences()) {
                val p=data.preferences
                listOf("reducedMotion" to p.reducedMotion,"highContrast" to p.highContrast,"compact" to p.compact,"safeMode" to p.safeMode,
                    "dynamicColors" to p.dynamicColors,"clockOverlay" to p.clockOverlay,"batteryOverlay" to p.batteryOverlay,"parallax" to p.parallax,
                    "gyroParallax" to p.gyroParallax,"zoomAnimation" to p.zoomAnimation,"externalCommands" to false,"externalLockCommands" to false,"externalBothCommands" to false).forEach { (key,value) -> wall.preferences.flag(key,value) }
                wall.preferences.number("cacheMb",p.cacheMb); wall.preferences.number("frameRate",p.frameRate)
                wall.preferences.text("shaderEffect",p.shaderEffect); wall.preferences.decimal("effectIntensity",p.effectIntensity)
                wall.preferences.text("transition",p.transition); wall.preferences.number("transitionMs",p.transitionMs)
            }
            progress("Restored metadata; imported schedules paused; existing device settings preserved")
            data.assets.size
        } finally { stage.deleteRecursively() }
        }
    }
    suspend fun exportSidecar(uri: Uri, assetId: String) = withContext(Dispatchers.IO) {
        val asset=requireNotNull(wall.dao.asset(assetId)); val edit=wall.dao.edit(assetId) ?: EditEntity(assetId)
        val data=BackupSnapshot(listOf(asset),listOf(CollectionEntity("default","Sidecar")),listOf(Membership("default",assetId)),listOf(edit),emptyList(),emptyList(),emptyList(),WorkspacePreferences())
        context.contentResolver.openOutputStream(uri,"wt").use { requireNotNull(it).write(BackupCodec.encode(data).toByteArray()) }
    }
    suspend fun importSidecar(uri: Uri): Int = withContext(Dispatchers.IO) {
        val raw=context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream)
            val out=java.io.ByteArrayOutputStream()
            val buffer=ByteArray(8192)
            while(true) { val n=stream.read(buffer); if(n<0) break; require(out.size()+n<=BackupCodec.MAX_MANIFEST_BYTES) { "Sidecar too large" }; out.write(buffer,0,n) }
            out.toByteArray()
        }
        require(raw.size<=BackupCodec.MAX_MANIFEST_BYTES)
        val data=BackupCodec.decode(String(raw))
        var count=0
        data.edits.forEach { edit -> val hash=data.assets.first { it.id==edit.assetId }.sha256; wall.dao.byHash(hash)?.let { wall.dao.putEdit(edit.copy(assetId=it.id)); count++ } }
        count
    }
}

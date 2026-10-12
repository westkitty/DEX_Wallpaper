package com.westkitty.dexwallpaper.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.westkitty.dexwallpaper.AppGraph
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.creative.Composition
import com.westkitty.dexwallpaper.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class Workspace(val label: String, val shortLabel: String) {
    CONTROL("Control center", "Control"), LIBRARY("Artwork library", "Library"), ROTATION("Rotation lab", "Rotation"),
    EDITOR("Image editor", "Editor"), MOTION("Motion studio", "Motion"), GENERATIVE("Generative canvas", "Generate"),
    SCENE("Scene composer", "Scenes"), AUTOMATION("Context automation", "Rules"), DATA("Live data", "Live data"),
    SOURCES("Sync & sources", "Sources"), HEALTH("System health", "Health"), SETTINGS("Settings", "Settings")
}
data class UiState(val page: Workspace = Workspace.CONTROL, val destination: Destination = Destination.HOME,
    val assets: List<AssetEntity> = emptyList(), val collections: List<CollectionEntity> = emptyList(),
    val memberships: List<Membership> = emptyList(), val screens: List<ScreenEntity> = emptyList(),
    val history: List<ReceiptEntity> = emptyList(), val rules: List<RuleEntity> = emptyList(), val scenes: List<SceneEntity> = emptyList(), val sources: List<RemoteSourceEntity> = emptyList(),
    val preferences: WorkspacePreferences = WorkspacePreferences(), val selectedId: String? = null,
    val busy: Boolean = false, val progress: String = "", val message: String? = null, val search: String = "",
    val edit: ImageEdit = ImageEdit(), val editRevision: Int = 0, val editDirty: Boolean = false) {
    val screen get() = screens.firstOrNull { it.destination == destination.name } ?: ScreenEntity(destination.name)
    val selected get() = assets.firstOrNull { it.id == selectedId } ?: assets.firstOrNull { it.id == screen.currentAssetId }
    val collection get() = collections.firstOrNull { it.id == screen.collectionId }
    val collectionAssets get() = assets.filter { a -> memberships.any { it.collectionId == screen.collectionId && it.assetId == a.id } }
}
class DexViewModel(val graph: AppGraph, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(UiState(page = Workspace.entries.firstOrNull { it.name == saved.get<String>("page") } ?: Workspace.CONTROL,
        destination = Destination.parse(saved["destination"] ?: "HOME"), selectedId = saved["selectedId"]))
    val state: StateFlow<UiState> = mutable.asStateFlow()
    private var operation: Job? = null
    // Keep startup cleanup from releasing a just-selected folder grant before its collection is saved.
    private val safGrantMutex = kotlinx.coroutines.sync.Mutex()
    private val editMutex=kotlinx.coroutines.sync.Mutex()
    private val editUndo=ArrayDeque<Pair<String,ImageEdit>>()
    init {
        val dao = graph.repository.dao
        viewModelScope.launch { dao.observeAssets().collect { list -> mutable.update { it.copy(assets = list) } } }
        viewModelScope.launch { dao.observeCollections().collect { list -> mutable.update { it.copy(collections = CollectionPinPolicy.forPicker(list)) } } }
        viewModelScope.launch { dao.observeMemberships().collect { list -> mutable.update { it.copy(memberships = list) } } }
        viewModelScope.launch { dao.observeScreens().collect { list -> mutable.update { it.copy(screens = list) } } }
        viewModelScope.launch { dao.observeHistory().collect { list -> mutable.update { it.copy(history = list) } } }
        viewModelScope.launch { dao.observeSources().collect { list -> mutable.update { it.copy(sources = list) } } }
        viewModelScope.launch { dao.observeScenes().collect { list -> mutable.update { it.copy(scenes = list) } } }
        viewModelScope.launch { dao.observeRules().collect { list -> mutable.update { it.copy(rules = list) } } }
        viewModelScope.launch { graph.preferences.flow.collect { p -> mutable.update { it.copy(preferences = p) } } }
        viewModelScope.launch {
            graph.repository.initialize()
            val cleanup = cleanupSafGrants()
            if (cleanup.failed > 0) notify("Some unused document access grants could not be released; review access in Android settings.")
        }
    }
    fun navigate(page: Workspace) { saved["page"] = page.name; mutable.update { it.copy(page = page) } }
    fun destination(target: Destination) { saved["destination"] = target.name; mutable.update { it.copy(destination = target) } }
    fun search(value: String) { mutable.update { it.copy(search = value) } }
    fun clearMessage() { mutable.update { it.copy(message = null) } }
    fun notify(message: String) { mutable.update { it.copy(message = message) } }
    private fun runOperation(label: String, action: suspend () -> String) {
        if (mutable.value.busy) { notify("Wait for the current operation, or cancel it first."); return }
        operation = viewModelScope.launch {
            mutable.update { it.copy(busy = true, progress = label) }
            try { notify(withContext(Dispatchers.IO) { action() }) }
            catch (e: CancellationException) { notify("Operation cancelled; completed work has been kept."); throw e }
            catch (e: SecurityException) { notify("Permission denied. Choose the source again using the system picker.") }
            catch (e: Exception) { notify(e.message?.take(220) ?: "Operation failed. Originals and settings were preserved.") }
            finally { mutable.update { it.copy(busy = false, progress = "") } }
        }
    }
    private suspend fun cleanupSafGrants(): SafGrantCleanupResult = withContext(NonCancellable) {
        safGrantMutex.lock()
        try { cleanupSafGrantsLocked() } finally { safGrantMutex.unlock() }
    }
    private suspend fun cleanupSafGrantsLocked(): SafGrantCleanupResult = withContext(NonCancellable) {
        graph.importer.releaseUnreferencedDocumentGrants()
    }
    private fun persistFolderReadGrant(uri: Uri) {
        val resolver = graph.context.contentResolver
        try {
            val alreadyPersisted = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
            if (!alreadyPersisted) resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            throw IllegalArgumentException("Folder provider refused persistent read access. Import individual images instead.", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("The selected folder cannot provide persistent read access. Choose it again or import individual images.", e)
        }
    }
    private fun SafGrantCleanupResult.warningSuffix(): String =
        if (failed == 0) "" else " $failed unused document access grant(s) could not be released; review access in Android settings."
    fun cancel() { operation?.cancel() }
    private fun Outcome<String>.message() = when (this) { is Outcome.Ok -> value; is Outcome.Error -> message }
    fun next() { val target = state.value.destination; runOperation("Selecting and applying…") { graph.repository.next(target).message() } }
    fun previous() { val target = state.value.destination; runOperation("Restoring previous wallpaper…") { graph.repository.previous(target).message() } }
    fun apply(id: String? = state.value.selected?.id, overrideMinutes: Int = 0) {
        val target = state.value.destination
        if (id == null) { notify("Import and select artwork first."); return }
        runOperation("Rendering ${target.label.lowercase()}…") { graph.repository.apply(id, target, overrideMinutes).message() }
    }
    fun togglePause() { val s = state.value; updateScreen { it.copy(paused = !s.screen.paused) } }
    fun enableRotation(enabled: Boolean) { updateScreen { it.copy(enabled = enabled, paused = false) } }
    fun updateScreen(transform: (ScreenEntity) -> ScreenEntity) {
        val target = state.value.destination
        viewModelScope.launch { graph.repository.updateScreen(target, transform); graph.scheduler.reconcile() }
    }
    fun collection(id: String) {
        val target = state.value.destination
        replaceScreenState(state.value.screen.copy(collectionId = id, pending = "", cycle = 0))
        viewModelScope.launch {
            try {
                replaceScreenState(persistCollectionSelection(target, id))
                graph.scheduler.reconcile()
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) {
                graph.repository.dao.screen(target.name)?.let(::replaceScreenState)
                notify(e.message?.take(180) ?: "Collection selection could not be saved.")
            }
        }
    }
    private fun replaceScreenState(screen: ScreenEntity) {
        mutable.update { current -> current.copy(screens = current.screens.filterNot { it.destination == screen.destination } + screen) }
    }
    private suspend fun persistCollectionSelection(target: Destination, collectionId: String): ScreenEntity {
        graph.repository.initialize()
        require(graph.repository.dao.allCollections().any { it.id == collectionId }) {
            "Selected collection is no longer available. Choose an existing collection and try again."
        }
        val current = graph.repository.dao.screen(target.name) ?: ScreenEntity(target.name)
        if (current.collectionId != collectionId) {
            graph.repository.updateScreen(target) { it.copy(collectionId = collectionId, pending = "", cycle = 0) }
        }
        return graph.repository.dao.screen(target.name) ?: current.copy(collectionId = collectionId, pending = "", cycle = 0)
    }
    fun newCollection(name: String) {
        val target = state.value.destination
        runOperation("Creating collection…") {
            val id = graph.repository.createCollection(name)
            val screen = persistCollectionSelection(target, id)
            replaceScreenState(screen)
            graph.scheduler.reconcile()
            "Collection created and selected for ${target.label.lowercase()}."
        }
    }
    fun select(id: String, editor: Boolean = false) {
        saved["selectedId"] = id
        viewModelScope.launch {
            val edit = graph.repository.dao.edit(id)?.model() ?: ImageEdit()
            mutable.update { it.copy(selectedId = id, edit = edit, editDirty = false, editRevision = it.editRevision + 1, page = if (editor) Workspace.EDITOR else it.page) }
            if (editor) saved["page"] = Workspace.EDITOR.name
        }
    }
    fun updateEdit(edit: ImageEdit, rememberUndo: Boolean = true) {
        val id = state.value.selected?.id ?: return
        if (rememberUndo) { editUndo.addLast(id to state.value.edit); if(editUndo.size>30) editUndo.removeFirst() }
        val bounded = edit.bounded()
        mutable.update { it.copy(edit = bounded, editDirty = true) }
        // Persist sidecar immediately, so recreation cannot discard unsaved user work.
        viewModelScope.launch { editMutex.lock(); try { graph.repository.dao.putEdit(EditEntity.from(id, bounded)); mutable.update { it.copy(editRevision = it.editRevision + 1, editDirty = false) } } finally { editMutex.unlock() } }
    }
    fun resetEdit() { updateEdit(ImageEdit()) }
    fun undoEdit() { val id=state.value.selected?.id ?: return; val index=editUndo.indexOfLast { it.first==id };if(index<0) { notify("No editor undo available.");return };val prior=editUndo.removeAt(index);updateEdit(prior.second,rememberUndo=false) }
    fun pinCollection(id: String, pinned: Boolean) {
        runOperation(if (pinned) "Pinning collection…" else "Unpinning collection…") {
            val collection = graph.repository.dao.allCollections().firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Collection no longer exists.")
            graph.repository.dao.putCollection(CollectionPinPolicy.setPinned(collection, pinned))
            "${if (pinned) "Pinned" else "Unpinned"} ${collection.name}; the collection picker updates its order."
        }
    }
    fun transferAsset(assetId: String, targetCollectionId: String, mode: CollectionTransferMode) {
        val sourceCollectionId = state.value.screen.collectionId
        if (sourceCollectionId == targetCollectionId) { notify("Choose a different destination collection."); return }
        runOperation(if (mode == CollectionTransferMode.COPY) "Copying artwork link…" else "Moving artwork link…") {
            graph.repository.transferAsset(assetId, sourceCollectionId, targetCollectionId, mode).message()
        }
    }
    fun favorite(id: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(favorite = !it.favorite)) } } }
    fun exclude(id: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(excluded = !it.excluded)) } } }
    fun tags(id: String, value: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(tags = value.take(400))) } } }
    fun importUris(uris: List<Uri>) {
        val target = state.value.destination
        val collection = state.value.screen.collectionId
        runOperation("Importing protected originals…") {
            var cleanup = SafGrantCleanupResult()
            val result = try {
                replaceScreenState(persistCollectionSelection(target, collection))
                val imported = graph.importer.importUris(uris, collection) { p ->
                    mutable.update { it.copy(progress = "${p.processed} processed · ${p.imported} imported · ${p.duplicates} duplicates · ${p.rejected} rejected") }
                }
                graph.sync.enforceCacheBudget(state.value.preferences.cacheMb)
                imported
            } finally {
                cleanup = cleanupSafGrants()
            }
            val limitNote = if (result.truncated) " Scan stopped at the 5,000-item limit; choose a smaller selection for the rest." else ""
            "Imported ${result.imported}; linked ${result.duplicates} duplicates; rejected ${result.rejected}.$limitNote${cleanup.warningSuffix()}"
        }
    }
    fun importFolder(uri: Uri, includeHidden: Boolean = false) {
        if (state.value.busy) { notify("Wait for the current operation, or cancel it first."); return }
        val target = state.value.destination
        val collection = state.value.screen.collectionId
        runOperation("Scanning authorized folder…") {
            safGrantMutex.lock()
            try {
                var cleanup = SafGrantCleanupResult()
                val result = try {
                    persistFolderReadGrant(uri)
                    replaceScreenState(persistCollectionSelection(target, collection))
                    val scanned = graph.importer.scanTree(uri, collection, includeHidden) { update ->
                        mutable.update { it.copy(progress = "${update.message} · ${update.scannedEntries} entries scanned · ${update.imported} new · ${update.rejected} rejected") }
                    }
                    val c = graph.repository.dao.allCollections().first { it.id == collection }
                    graph.repository.dao.putCollection(c.copy(sourceTree = uri.toString()))
                    graph.sync.enforceCacheBudget(state.value.preferences.cacheMb)
                    scanned
                } finally {
                    cleanup = cleanupSafGrantsLocked()
                }
                val limitNote = if (result.truncated) " Scan was bounded to 5,000 file/folder entries and depth 16; choose smaller subfolders to import the rest." else ""
                "Folder synchronized: ${result.imported} new, ${result.duplicates} existing/recovered, ${result.rejected} rejected.$limitNote${cleanup.warningSuffix()}"
            } finally {
                safGrantMutex.unlock()
            }
        }
    }
    fun rescan(collection: CollectionEntity, includeHidden: Boolean = false) {
        if (collection.sourceTree.isBlank()) { notify("Choose a folder for this collection first."); return }
        runOperation("Synchronizing folder…") {
            var cleanup = SafGrantCleanupResult()
            val result = try {
                graph.importer.scanTree(Uri.parse(collection.sourceTree), collection.id, includeHidden) { update ->
                    mutable.update { it.copy(progress = "${update.message} · ${update.scannedEntries} entries scanned") }
                }
            } finally {
                cleanup = cleanupSafGrants()
            }
            val limitNote = if (result.truncated) " Scan stopped at the 5,000-entry or depth-16 bound." else ""
            "Synchronized: ${result.imported} new, ${result.duplicates} existing, ${result.rejected} rejected.$limitNote${cleanup.warningSuffix()}"
        }
    }
    fun validateAssets() { runOperation("Checking library files…") { "${graph.importer.validateAssets()} missing, altered or unreadable originals quarantined. Originals are never deleted." } }
    fun setFlag(key: String, value: Boolean) { viewModelScope.launch { graph.preferences.flag(key, value) } }
    fun setText(key: String, value: String) { viewModelScope.launch { graph.preferences.text(key, value) } }
    fun setDecimal(key: String, value: Float) { viewModelScope.launch { graph.preferences.decimal(key, value) } }
    fun setNumber(key: String, value: Int) { viewModelScope.launch { graph.preferences.number(key, value); if (key == "cacheMb") graph.sync.enforceCacheBudget(value) } }
    fun addRule(name: String, start: Int, end: Int, priority: Int, charging: Int = -1, dark: Int = -1) {
        val s = state.value
        viewModelScope.launch { graph.repository.dao.putRule(RuleEntity(UUID.randomUUID().toString(), name.ifBlank { "Time window" }, s.screen.collectionId, priority, startMinute = start, endMinute = end, charging = charging, darkTheme = dark, destination = s.destination.name)) }
    }
    fun toggleRule(rule: RuleEntity) { viewModelScope.launch { graph.repository.dao.putRule(rule.copy(enabled = !rule.enabled)) } }
    fun generate(c: Composition, width: Int = 2960, height: Int = 1848) {
        val collection=state.value.screen.collectionId
        runOperation("Generating offline artwork…") { val id=graph.creative.saveGenerated(c,collection,width,height); select(id); "Composition saved to your collection. Apply it when ready." }
    }
    fun saveScene(id: String?, scene: SceneManifest) {
        runOperation("Saving scene revision…") { graph.creative.saveScene(id,scene); "Scene saved with revision history." }
    }
    fun renderScene(scene: SceneManifest, time: Double = 0.0) {
        val collection=state.value.screen.collectionId
        runOperation("Rendering scene to protected artwork…") { val id=graph.creative.renderToLibrary(scene,collection,time); select(id); "Scene rendered into the library. Apply from Control or Library." }
    }
    fun exportDiagnostics(uri: Uri) { runOperation("Exporting sanitized diagnostics…") { com.westkitty.dexwallpaper.platform.DiagnosticsRepository(graphContext(), graph).export(uri); "Sanitized diagnostic counts exported; no private names, artwork, URIs, or credentials." } }
    private fun graphContext() = graph.context
    fun exportBackup(uri: Uri, originals: Boolean) { runOperation("Exporting backup…") { val count=graph.backup.export(uri,originals) { message -> mutable.update { it.copy(progress=message) } }; "Backup exported: $count asset records; credentials and device URI grants excluded." } }
    fun restoreBackup(uri: Uri) {
        runOperation("Validating backup…") {
            var cleanup = SafGrantCleanupResult()
            val count = try {
                graph.backup.restore(uri) { message -> mutable.update { it.copy(progress = message) } }
            } finally {
                cleanup = cleanupSafGrants()
            }
            graph.scheduler.reconcile()
            "Restored $count records. Rotation and rules paused; review screen destinations before enabling.${cleanup.warningSuffix()}"
        }
    }
    fun exportSidecar(uri: Uri) { val id=state.value.selected?.id ?: return; runOperation("Exporting editing sidecar…") { graph.backup.exportSidecar(uri,id); "Editing sidecar exported; original pixels excluded." } }
    fun importSidecar(uri: Uri) { runOperation("Importing editing sidecars…") { val count=graph.backup.importSidecar(uri); state.value.selected?.let { select(it.id) }; "Applied $count matching sidecars by artwork hash." } }
    fun loadProfile(profile: VisualProfile) {
        val s=state.value
        runOperation("Loading visual profile…") { graph.profiles.apply(profile,s.destination,s.selected?.id); s.selected?.let { select(it.id) }; graph.scheduler.reconcile(); "${profile.name} profile loaded; wallpaper was not applied automatically." }
    }
    fun saveProfile(name: String) { val s=state.value; runOperation("Saving visual profile…") { graph.profiles.save(graph.profiles.snapshot(name,s.destination,s.selected?.id)); "Custom visual profile saved locally." } }
    fun exportProfile(uri: Uri) { val s=state.value; runOperation("Exporting visual profile…") { graph.profiles.export(uri,graph.profiles.snapshot("Custom",s.destination,s.selected?.id)); "Visual profile exported without artwork or credentials." } }
    fun importProfile(uri: Uri) { val s=state.value; runOperation("Importing visual profile…") { val p=graph.profiles.import(uri); graph.profiles.apply(p,s.destination,s.selected?.id); s.selected?.let { select(it.id) }; graph.scheduler.reconcile(); "${p.name} imported and loaded into the selected destination configuration; no wallpaper applied." } }
    fun addSource(name: String,endpoint: String,kind: String,username: String,password: String,wifi: Boolean,metered: Boolean,privateNetwork: Boolean,budget: Int) {
        val collection=state.value.screen.collectionId
        runOperation("Saving trusted source…") {
            val valid=when(val parsed=NetworkPolicy.endpoint(endpoint,privateNetwork)) { is Outcome.Error -> throw IllegalArgumentException(parsed.message);is Outcome.Ok -> parsed.value }
            require(kind in setOf("IMAGE","WEBDAV","NEXTCLOUD","RSS","JSON"))
            if(kind=="WEBDAV" || kind=="NEXTCLOUD") require(valid.uri.path.orEmpty().endsWith('/')) { "WebDAV/Nextcloud collection endpoint must end with / to bound authorized links" }
            val id=UUID.randomUUID().toString()
            require(graph.repository.dao.allCollections().any { it.id==collection }) { "Choose an existing collection" }
            withContext(Dispatchers.IO) {
                try {
                    if(username.isNotBlank() || password.isNotBlank()) graph.vault.put(id,com.westkitty.dexwallpaper.security.RemoteCredentials(username,password))
                    graph.repository.dao.putSource(RemoteSourceEntity(id,name.ifBlank { "Trusted source" }.take(100),valid.uri.toString(),kind,collection,wifi,metered,budget.coerceIn(1,80),privateNetwork))
                } catch(e: Exception) { graph.vault.remove(id);throw e }
            }
            "Trusted source saved; no automatic transfer. Use Synchronize explicitly."
        }
    }
    fun syncSource(id: String) { runOperation("Synchronizing trusted source…") { graph.sync.sync(id) { message -> mutable.update { it.copy(progress=message) } } } }
    fun toggleSource(source: RemoteSourceEntity) { if(state.value.busy) { notify("Finish or cancel synchronization before changing the source.");return };viewModelScope.launch { graph.repository.dao.source(source.id)?.let { graph.repository.dao.putSource(it.copy(enabled=!it.enabled)) } } }
    fun deleteSource(source: RemoteSourceEntity) { runOperation("Removing connection configuration…") { graph.sync.removeSource(source.id); "Source and encrypted credential removed; imported artwork kept." } }
    fun bindData(sourceId: String,sceneId: String) { runOperation("Binding cached numeric data…") { "${graph.sync.bindVariables(sourceId,sceneId)} fields copied into persistent scene variables; revision saved." } }
    private fun ownsLiveTarget(target: Destination): Boolean {
        val manager=android.app.WallpaperManager.getInstance(graph.context)
        val component=android.content.ComponentName(graph.context,com.westkitty.dexwallpaper.wallpaper.DexWallpaperService::class.java)
        if(android.os.Build.VERSION.SDK_INT<34) return target==Destination.BOTH && manager.wallpaperInfo?.component==component
        fun owned(flag: Int)=if(android.os.Build.VERSION.SDK_INT>=34) manager.getWallpaperInfo(flag)?.component==component else false
        return when(target) { Destination.HOME -> owned(1);Destination.LOCK -> owned(2);Destination.BOTH -> owned(1) || owned(2) }
    }
    fun prepareLivePreview(onReady: () -> Unit) {
        val s=state.value;val asset=s.selected ?: run { notify("Select artwork before opening the live picker.");return }
        viewModelScope.launch {
            try {
                val before=withContext(Dispatchers.IO) { graph.controller.snapshot() }
                val safe=DestinationPolicy.check(s.destination,before)
                if(safe is Outcome.Error) { notify(safe.message);return@launch }
                require(!asset.quarantined && graph.importer.file(asset).isFile) { "Artwork is unavailable; recover it before live setup" }
                val edit=graph.repository.dao.edit(asset.id)?.model() ?: ImageEdit()
                graph.preferences.prepareLivePreview(s.destination,LiveConfigCodec.encode(LiveConfiguration(asset.id,edit,s.preferences)),before)
                onReady()
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { notify(e.message?.take(180) ?: "Live preview preparation failed") }
        }
    }
    fun completeLivePicker(success: Boolean) {
        viewModelScope.launch {
            try {
                val prepared=graph.preferences.flow.first()
                val target=Destination.entries.firstOrNull { it.name==prepared.livePending }
                graph.preferences.text("livePending","") // Never replay cancellation on engine/process recreation.
                if(!success || target==null) { notify("Picker cancelled or OEM did not report success; installed live snapshots were not changed. Commit explicitly in Motion if ownership is verified.");return@launch }
                val flags=withTimeoutOrNull(3000) { com.westkitty.dexwallpaper.wallpaper.LiveEngineRegistry.flow.first { LiveDestinationPolicy.mayUpdate(target,it) } }
                val before=WallpaperSnapshot(prepared.liveBeforeHome,prepared.liveBeforeLock)
                val after=withContext(Dispatchers.IO) { graph.controller.snapshot() }
                if(flags==null || !LiveDestinationPolicy.mayFinishPicker(target,flags,before,after) || !ownsLiveTarget(target)) {
                    notify("Picker assignment could not match the requested destination / preserve the other identity; no live snapshot committed. OS choices cannot be rolled back safely; review system settings.");return@launch
                }
                graph.preferences.commitLive(target,prepared.livePreview)
                notify(if(target==Destination.BOTH) "Configured all OS-assigned DEX live instances; OS still owns final destinations." else "Live snapshot committed to verified ${target.label.lowercase()} ownership; other wallpaper identity unchanged.")
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { notify("Live destination verification failed; no retry or fallback to Both. Review system settings.") }
        }
    }
    fun commitLive() {
        val s=state.value;val asset=s.selected ?: run { notify("Select artwork first.");return }
        runOperation("Committing destination-scoped live snapshot…") {
            val snapshot=withContext(Dispatchers.IO) { graph.controller.snapshot() }
            val check=DestinationPolicy.check(s.destination,snapshot)
            require(check is Outcome.Ok) { (check as Outcome.Error).message }
            require(LiveDestinationPolicy.mayUpdate(s.destination,com.westkitty.dexwallpaper.wallpaper.LiveEngineRegistry.flags()) && ownsLiveTarget(s.destination)) { "Home/Lock live ownership is unknown/shared/unassigned; scoped update refused. Both only configures OS-assigned DEX instances." }
            require(!asset.quarantined && graph.importer.file(asset).isFile) { "Artwork must be recovered first" }
            val edit=graph.repository.dao.edit(asset.id)?.model() ?: ImageEdit()
            graph.preferences.commitLive(s.destination,LiveConfigCodec.encode(LiveConfiguration(asset.id,edit,s.preferences)))
            "Live snapshot committed for ${s.destination.label}; static records cannot rebind live Lock. Both only updates assigned DEX instances."
        }
    }
    fun deleteRule(id: String) { viewModelScope.launch { graph.repository.dao.deleteRule(id) } }
}

package com.westkitty.dexwallpaper.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.westkitty.dexwallpaper.AppGraph
import com.westkitty.dexwallpaper.core.*
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
    val history: List<ReceiptEntity> = emptyList(), val rules: List<RuleEntity> = emptyList(),
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
    init {
        val dao = graph.repository.dao
        viewModelScope.launch { dao.observeAssets().collect { list -> mutable.update { it.copy(assets = list) } } }
        viewModelScope.launch { dao.observeCollections().collect { list -> mutable.update { it.copy(collections = list) } } }
        viewModelScope.launch { dao.observeMemberships().collect { list -> mutable.update { it.copy(memberships = list) } } }
        viewModelScope.launch { dao.observeScreens().collect { list -> mutable.update { it.copy(screens = list) } } }
        viewModelScope.launch { dao.observeHistory().collect { list -> mutable.update { it.copy(history = list) } } }
        viewModelScope.launch { dao.observeRules().collect { list -> mutable.update { it.copy(rules = list) } } }
        viewModelScope.launch { graph.preferences.flow.collect { p -> mutable.update { it.copy(preferences = p) } } }
        viewModelScope.launch { graph.repository.initialize() }
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
    fun collection(id: String) { updateScreen { it.copy(collectionId = id, pending = "", cycle = 0) } }
    fun newCollection(name: String) { runOperation("Creating collection…") { val id = graph.repository.createCollection(name); collection(id); "Collection created." } }
    fun select(id: String, editor: Boolean = false) {
        saved["selectedId"] = id
        viewModelScope.launch {
            val edit = graph.repository.dao.edit(id)?.model() ?: ImageEdit()
            mutable.update { it.copy(selectedId = id, edit = edit, editDirty = false, editRevision = it.editRevision + 1, page = if (editor) Workspace.EDITOR else it.page) }
            if (editor) saved["page"] = Workspace.EDITOR.name
        }
    }
    fun updateEdit(edit: ImageEdit) {
        val id = state.value.selected?.id ?: return
        val bounded = edit.bounded()
        mutable.update { it.copy(edit = bounded, editDirty = true) }
        // Persist sidecar immediately, so recreation cannot discard unsaved user work.
        viewModelScope.launch { graph.repository.dao.putEdit(EditEntity.from(id, bounded)); mutable.update { it.copy(editRevision = it.editRevision + 1, editDirty = false) } }
    }
    fun resetEdit() { updateEdit(ImageEdit()) }
    fun favorite(id: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(favorite = !it.favorite)) } } }
    fun exclude(id: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(excluded = !it.excluded)) } } }
    fun tags(id: String, value: String) { viewModelScope.launch { graph.repository.dao.asset(id)?.let { graph.repository.dao.updateAsset(it.copy(tags = value.take(400))) } } }
    fun importUris(uris: List<Uri>) {
        val collection = state.value.screen.collectionId
        runOperation("Importing protected originals…") {
            val result = graph.importer.importUris(uris, collection) { p -> mutable.update { it.copy(progress = "${p.processed} processed · ${p.imported} imported · ${p.duplicates} duplicates · ${p.rejected} rejected") } }
            "Imported ${result.imported}; linked ${result.duplicates} duplicates; rejected ${result.rejected}."
        }
    }
    fun importFolder(uri: Uri, includeHidden: Boolean = false) {
        val collection = state.value.screen.collectionId
        runOperation("Scanning authorized folder…") {
            val c = graph.repository.dao.allCollections().first { it.id == collection }
            graph.repository.dao.putCollection(c.copy(sourceTree = uri.toString()))
            val p = graph.importer.scanTree(uri, collection, includeHidden) { p -> mutable.update { it.copy(progress = "${p.message} · ${p.imported} new · ${p.rejected} rejected") } }
            "Folder synchronized: ${p.imported} new, ${p.duplicates} existing, ${p.rejected} rejected."
        }
    }
    fun rescan(collection: CollectionEntity, includeHidden: Boolean = false) {
        if (collection.sourceTree.isBlank()) { notify("Choose a folder for this collection first."); return }
        runOperation("Synchronizing folder…") { val p = graph.importer.scanTree(Uri.parse(collection.sourceTree), collection.id, includeHidden) { p -> mutable.update { it.copy(progress = p.message) } }; "Synchronized: ${p.imported} new, ${p.rejected} rejected." }
    }
    fun validateAssets() { runOperation("Checking library files…") { "${graph.importer.validateAssets()} missing files quarantined. Originals are never deleted." } }
    fun setFlag(key: String, value: Boolean) { viewModelScope.launch { graph.preferences.flag(key, value) } }
    fun setNumber(key: String, value: Int) { viewModelScope.launch { graph.preferences.number(key, value) } }
    fun addRule(name: String, start: Int, end: Int, priority: Int, charging: Int = -1, dark: Int = -1) {
        val s = state.value
        viewModelScope.launch { graph.repository.dao.putRule(RuleEntity(UUID.randomUUID().toString(), name.ifBlank { "Time window" }, s.screen.collectionId, priority, startMinute = start, endMinute = end, charging = charging, darkTheme = dark, destination = s.destination.name)) }
    }
    fun toggleRule(rule: RuleEntity) { viewModelScope.launch { graph.repository.dao.putRule(rule.copy(enabled = !rule.enabled)) } }
    fun deleteRule(id: String) { viewModelScope.launch { graph.repository.dao.deleteRule(id) } }
}

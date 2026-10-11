@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.westkitty.dexwallpaper.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ControlCenter(vm: DexViewModel, state: UiState, wide: Boolean) {
    val active = state.assets.firstOrNull { it.id == state.screen.currentAssetId } ?: state.selected
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SectionTitle("Your living wall.", "${state.destination.label} / ${if (state.screen.paused) "Paused" else if (state.screen.enabled) "Automatic rotation" else "Manual control"}")
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1.9f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArtworkPreview(vm, active, modifier = Modifier.fillMaxWidth())
                Text(active?.name ?: "No artwork applied", style = MaterialTheme.typography.titleMedium)
                Text("Rendered composition preview • not the system wallpaper surface", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) { WallStatus(state) }
        } else {
            ArtworkPreview(vm, active, modifier = Modifier.fillMaxWidth())
            Text(active?.name ?: "No artwork applied", style = MaterialTheme.typography.titleMedium)
            WallStatus(state)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(vm::next, enabled = !state.busy && state.collectionAssets.isNotEmpty()) { Icon(Icons.Outlined.SkipNext, null); Text("Next wallpaper") }
            OutlinedButton(vm::previous, enabled = !state.busy && state.history.any { it.destination == state.destination.name }) { Text("Previous") }
            OutlinedButton(vm::togglePause, enabled = state.screen.enabled) { Text(if (state.screen.paused) "Resume" else "Pause") }
            OutlinedButton(onClick = { vm.apply(active?.id) }, enabled = !state.busy && active != null) { Text("Apply") }
            OutlinedButton(onClick = { active?.let { vm.select(it.id, true) } }, enabled = active != null) { Text("Edit") }
            OutlinedButton(onClick = { active?.let { vm.favorite(it.id) } }, enabled = active != null) { Text(if (active?.favorite == true) "Unfavorite" else "Favorite") }
            OutlinedButton(onClick = { active?.let { vm.exclude(it.id) } }, enabled = active != null) { Text(if (active?.excluded == true) "Include" else "Exclude") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectionPicker(state, vm::collection)
            TextButton(onClick = { vm.navigate(Workspace.HEALTH) }) { Text("Diagnostics") }
        }
        if (state.destination == Destination.HOME) Text("HOME ONLY • DEX uses FLAG_SYSTEM, verifies the separate Lock identity, and refuses unverifiable shared destinations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.screen.error.isNotBlank()) Text(state.screen.error, color = MaterialTheme.colorScheme.error)
        val receipt = state.history.firstOrNull { it.destination == state.destination.name }
        HorizontalDivider()
        Eyebrow("Decision receipt")
        Text(receipt?.let { "${formatTime(it.timestamp)} · ${it.reason}\nSource collection: ${state.collections.firstOrNull { c -> c.id == it.collectionId }?.name ?: it.collectionId}" }
            ?: "Every successful wallpaper change records its destination, source, selection mode, and reason. No changes yet.")
    }
}
@Composable private fun WallStatus(state: UiState) {
    DataLine("Destination", state.destination.label)
    DataLine("Collection", state.collection?.name ?: "Personal library")
    DataLine("Selection", "${state.screen.mode.lowercase()} · ${state.collectionAssets.count { !it.excluded && !it.quarantined }} eligible")
    DataLine("Next change", if (!state.screen.enabled || state.screen.paused) "Not scheduled" else "~${formatTime(state.screen.nextDue)} (approximate)")
    DataLine("Last success", formatTime(state.screen.lastSuccess))
    DataLine("Automation", "${state.rules.count { it.enabled && it.destination == state.destination.name }} active rules")
    DataLine("Renderer", "Static composition / optional live Canvas")
    DataLine("Battery policy", "Defer rotation while screen-off or Battery Saver")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LibraryWorkspace(vm: DexViewModel, state: UiState) {
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var collectionName by rememberSaveable { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    var inspectorId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMoveTargetId by remember { mutableStateOf<String?>(null) }
    val assets = state.collectionAssets.filter { a -> (!favoritesOnly || a.favorite) && (state.search.isBlank() || "${a.name} ${a.tags}".contains(state.search, true)) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Artwork library", "Protected originals. One shared collection database.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CollectionPicker(state, vm::collection)
            OutlinedButton(onClick = { create = true }, enabled = !state.busy) { Text("New collection") }
            state.collection?.let { collection ->
                OutlinedButton(onClick = { vm.pinCollection(collection.id, !collection.pinned) }, enabled = !state.busy) {
                    Text(if (collection.pinned) "Unpin collection" else "Pin collection")
                }
            }
            FilterChip(favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, enabled = !state.busy, label = { Text("Favorites") })
        }
        OutlinedTextField(state.search, vm::search, label = { Text("Search artwork or tags") }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
        if (assets.isEmpty()) {
            Column(Modifier.padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("${state.collection?.name ?: "Personal library"} / empty")
                Text("Use Add images to import photos or an authorized folder. Imports stay on this device and original files are never modified.")
                Text("No broad storage permission or account is required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyVerticalGrid(GridCells.Adaptive(if (state.preferences.compact) 160.dp else 220.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(assets, key = { it.id }) { asset ->
                Column(Modifier.clickable { vm.select(asset.id); inspectorId = asset.id }) {
                    Thumbnail(vm, asset)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(asset.name, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { vm.favorite(asset.id) }) { Icon(if (asset.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "${if (asset.favorite) "Unfavorite" else "Favorite"} ${asset.name}", tint = if (asset.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text("${asset.width} × ${asset.height}${if (asset.quarantined) " · missing" else if (asset.excluded) " · excluded" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (create) AlertDialog(onDismissRequest = { create = false }, title = { Text("New collection") }, text = { OutlinedTextField(collectionName, { collectionName = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { vm.newCollection(collectionName); collectionName = ""; create = false }, enabled = collectionName.isNotBlank()) { Text("Create") } }, dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } })
    inspectorId?.let { id -> state.assets.firstOrNull { it.id == id }?.let { asset ->
        var tags by remember(asset.id) { mutableStateOf(asset.tags) }
        AlertDialog(onDismissRequest = { inspectorId = null }, title = { Text(asset.name) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArtworkPreview(vm, asset)
                Text("${asset.width} × ${asset.height} · ${asset.bytes / 1024} KB\nImported ${formatTime(asset.importedAt)}\n${asset.license}")
                Text(ShuffleGuard.eligibility(Candidate(asset.id, asset.favorite, asset.excluded, !asset.quarantined), state.screen.favoritesOnly))
                Text("Asset ID: ${asset.id}\nCollection ID: ${state.screen.collectionId}\nSHA-256: ${asset.sha256.take(16)}…", style = MaterialTheme.typography.labelSmall)
                val similar = state.assets.count { it.id != id && AssetIntegrity.nearDuplicate(it.perceptualHash, asset.perceptualHash) }
                Text("$similar visually similar image(s) · local dHash heuristic, not proof of identity")
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") })
                Row { TextButton(onClick = { vm.tags(id, tags); vm.notify("Tags saved.") }) { Text("Save tags") }; TextButton(onClick = { vm.exclude(id) }) { Text(if (asset.excluded) "Include in rotation" else "Exclude") } }
                val transferTargets = state.collections.filterNot { it.id == state.screen.collectionId }
                var transferTargetId by remember(asset.id, state.screen.collectionId) { mutableStateOf(transferTargets.firstOrNull()?.id) }
                var transferMenuOpen by remember(asset.id) { mutableStateOf(false) }
                if (transferTargets.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { transferMenuOpen = true }, enabled = !state.busy) {
                            Text(transferTargets.firstOrNull { it.id == transferTargetId }?.name ?: "Choose collection")
                        }
                        DropdownMenu(expanded = transferMenuOpen, onDismissRequest = { transferMenuOpen = false }) {
                            transferTargets.forEach { target ->
                                DropdownMenuItem(text = { Text(target.name) }, onClick = {
                                    transferTargetId = target.id
                                    transferMenuOpen = false
                                })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { transferTargetId?.let { vm.transferAsset(id, it, CollectionTransferMode.COPY) } },
                            enabled = transferTargetId != null && !state.busy) { Text("Copy to collection") }
                        TextButton(onClick = { transferTargetId?.let { pendingMoveTargetId = it } },
                            enabled = transferTargetId != null && !state.busy) { Text("Move to collection") }
                    }
                    Text("Only collection membership changes. Protected original, edits, and screen configuration stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Create another collection to copy or move this artwork.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.apply(id, overrideMinutes = 60); inspectorId = null }) { Text("Apply & pin for 1 hour") }
            }
        }, confirmButton = { TextButton(onClick = { vm.apply(id); inspectorId = null }) { Text("Apply to ${state.destination.label.lowercase()}") } }, dismissButton = { TextButton(onClick = { vm.select(id, true); inspectorId = null }) { Text("Edit") } })
    } }
    val moveTargetId = pendingMoveTargetId
    val moveTarget = state.collections.firstOrNull { it.id == moveTargetId }
    val moveAsset = state.assets.firstOrNull { it.id == inspectorId }
    if (moveTarget != null && moveAsset != null) AlertDialog(
        onDismissRequest = { pendingMoveTargetId = null },
        title = { Text("Move artwork to ${moveTarget.name}?") },
        text = { Text("This removes the artwork's membership from ${state.collection?.name ?: "the current collection"}. Its protected original and edits remain in the library; this does not apply a wallpaper or change screen settings.") },
        confirmButton = {
            TextButton(onClick = {
                vm.transferAsset(moveAsset.id, moveTarget.id, CollectionTransferMode.MOVE)
                pendingMoveTargetId = null
                inspectorId = null
            }, enabled = !state.busy) { Text("Move membership") }
        },
        dismissButton = { TextButton(onClick = { pendingMoveTargetId = null }) { Text("Cancel") } },
    )
}
@Composable private fun Thumbnail(vm: DexViewModel, asset: AssetEntity) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, asset.id) {
        value = withContext(Dispatchers.IO) { try { android.graphics.BitmapFactory.decodeFile(vm.graph.importer.thumbnail(asset).path) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null } }
    }
    DisposableEffect(bitmap) {
        onDispose { bitmap?.takeUnless { it.isRecycled }?.recycle() }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), asset.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Icon(Icons.Outlined.BrokenImage, "Thumbnail unavailable for ${asset.name}")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun RotationLab(vm: DexViewModel, state: UiState) {
    var minutes by remember(state.screen.intervalMinutes, state.destination) { mutableStateOf(state.screen.intervalMinutes.toString()) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("Rotation lab", "ShuffleGuard / persistent, explainable selection")
        CollectionPicker(state, vm::collection)
        Eyebrow("Selection method")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { RotationMode.entries.forEach { mode -> FilterChip(state.screen.mode == mode.name,
            onClick = { vm.updateScreen { it.copy(mode = mode.name, pending = "") } }, label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        FlagRow("Automatic rotation", state.screen.enabled, "Independent ${state.destination.label.lowercase()} schedule. Both-screen changes are manual only.", enabled = state.destination != Destination.BOTH) { vm.enableRotation(it) }
        FlagRow("Favorites only", state.screen.favoritesOnly, "Images not marked favorite are excluded from this rotation.") { value -> vm.updateScreen { it.copy(favoritesOnly = value, pending = "") } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(5) }, label = { Text("Interval / minutes") }, modifier = Modifier.widthIn(max = 220.dp), singleLine = true)
            Button(onClick = { minutes.toLongOrNull()?.let { value -> vm.updateScreen { it.copy(intervalMinutes = value.coerceIn(15,10080), nextDue = System.currentTimeMillis() + value.coerceIn(15,10080) * 60_000) } } }, enabled = minutes.toLongOrNull() != null) { Text("Save") }
        }
        Text("WorkManager is approximate: minimum 15 minutes. Android may delay execution. No exact alarms or always-on scheduler. Changes are deferred while the display is off or Battery Saver is active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        DataLine("Fairness separation", "${state.screen.minimumSeparation} appearance(s) where pool size permits")
        Slider(state.screen.minimumSeparation.toFloat(), { n -> vm.updateScreen { it.copy(minimumSeparation = n.toInt()) } }, valueRange = 0f..10f, steps = 9)
        DataLine("Queue integrity", "${state.screen.queue().pending.size} pending · cycle ${state.screen.cycle} · committed only after a successful wallpaper write")
        if (state.screen.manualUntil > System.currentTimeMillis()) {
            DataLine("Manual override", "Pinned until ${formatTime(state.screen.manualUntil)}")
            TextButton(onClick = { vm.updateScreen { it.copy(manualUntil = 0) } }) { Text("Release override") }
        }
        HorizontalDivider()
        Eyebrow("Future interval preview / approximate")
        val now = System.currentTimeMillis()
        (1..5).forEach { n -> Text("$n   ~${formatTime(now + state.screen.intervalMinutes * 60_000 * n)}") }
        HorizontalDivider()
        Eyebrow("History / decision receipts")
        state.history.filter { it.destination == state.destination.name }.take(20).forEach { r ->
            Text("${formatTime(r.timestamp)} · ${state.assets.firstOrNull { it.id == r.assetId }?.name ?: "Missing artwork"}\n${r.reason}", style = MaterialTheme.typography.bodySmall)
        }
        if (state.history.none { it.destination == state.destination.name }) Text("No successful changes yet.")
    }
}

@Composable fun EditorWorkspace(vm: DexViewModel, state: UiState, wide: Boolean, exportSidecar: () -> Unit, importSidecar: () -> Unit) {
    var portrait by rememberSaveable { mutableStateOf(false) }
    var compare by rememberSaveable { mutableStateOf(false) }
    val asset = state.selected
    if (asset == null) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { SectionTitle("FrameSense editor", "Non-destructive, orientation-aware framing"); Text("Select an image in your library. Edits are saved as metadata; original pixels are protected."); Button(onClick = { vm.navigate(Workspace.LIBRARY) }) { Text("Open library") } }
        return
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("FrameSense editor", asset.name)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(portrait, onClick = { portrait = !portrait }, label = { Text(if (portrait) "Portrait" else "Landscape") })
            FilterChip(compare, onClick = { compare = !compare }, label = { Text("Original / edited") })
            TextButton(vm::undoEdit) { Text("Undo") }
            TextButton(vm::resetEdit) { Text("Reset edits") }
        }
        val gestureModifier = Modifier.pointerInput(asset.id, portrait) {
            detectTransformGestures { _, pan, zoom, _ ->
                val e = vm.state.value.edit
                val x = if (portrait) e.portraitX else e.focalX
                val y = if (portrait) e.portraitY else e.focalY
                vm.updateEdit(if (portrait) e.copy(zoom = e.zoom * zoom, portraitX = x - pan.x / size.width.coerceAtLeast(1), portraitY = y - pan.y / size.height.coerceAtLeast(1))
                    else e.copy(zoom = e.zoom * zoom, focalX = x - pan.x / size.width.coerceAtLeast(1), focalY = y - pan.y / size.height.coerceAtLeast(1)))
            }
        }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ArtworkPreview(vm, asset, state.edit, state.editRevision, Modifier.weight(1.6f).then(gestureModifier), portrait, original = compare)
            Column(Modifier.weight(1f)) { FramingControls(vm, state, portrait) }
        } else {
            ArtworkPreview(vm, asset, state.edit, state.editRevision, Modifier.fillMaxWidth().then(gestureModifier), portrait, original = compare)
            FramingControls(vm, state, portrait)
        }
        Text("Drag to pan. Pinch to zoom. Landscape and portrait focal points are stored independently. All edits autosave.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Eyebrow("QuietCanvas / image treatment")
        EditSlider("Brightness", state.edit.brightness, -1f..1f) { vm.updateEdit(state.edit.copy(brightness = it)) }
        EditSlider("Contrast", state.edit.contrast, 0f..2f) { vm.updateEdit(state.edit.copy(contrast = it)) }
        EditSlider("Saturation / grayscale", state.edit.saturation, 0f..2f) { vm.updateEdit(state.edit.copy(saturation = it)) }
        EditSlider("Dimming", state.edit.dim, 0f..1f) { vm.updateEdit(state.edit.copy(dim = it)) }
        EditSlider("Vignette", state.edit.vignette, 0f..1f) { vm.updateEdit(state.edit.copy(vignette = it)) }
        EditSlider("Soft-focus blur (downsampled)", state.edit.blur, 0f..25f) { vm.updateEdit(state.edit.copy(blur = it)) }
        EditSlider("Top icon-safe zone", state.edit.safeTop, 0f..0.45f) { vm.updateEdit(state.edit.copy(safeTop = it)) }
        EditSlider("Bottom icon-safe zone", state.edit.safeBottom, 0f..0.45f) { vm.updateEdit(state.edit.copy(safeBottom = it)) }
        Text("Safe zones are calibrated manually. DEX cannot read Samsung launcher icon positions.", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.apply(asset.id) }, enabled = !state.busy) { Text("Apply to ${state.destination.label.lowercase()}") }
            OutlinedButton(exportSidecar, enabled = !state.busy) { Text("Export sidecar") }
            OutlinedButton(importSidecar, enabled = !state.busy) { Text("Import sidecar") }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FramingControls(vm: DexViewModel, state: UiState, portrait: Boolean) {
    Eyebrow("Scaling")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ScaleMode.entries.forEach { mode -> FilterChip(state.edit.scale == mode, onClick = { vm.updateEdit(state.edit.copy(scale = mode)) }, label = { Text(mode.name.lowercase().replace('_',' ')) }) } }
    PrecisionNumber("Zoom",state.edit.zoom,1f..4f) { vm.updateEdit(state.edit.copy(zoom=it)) }
    PrecisionNumber("Focal X",if(portrait) state.edit.portraitX else state.edit.focalX,0f..1f) { vm.updateEdit(if(portrait) state.edit.copy(portraitX=it) else state.edit.copy(focalX=it)) }
    PrecisionNumber("Focal Y",if(portrait) state.edit.portraitY else state.edit.focalY,0f..1f) { vm.updateEdit(if(portrait) state.edit.copy(portraitY=it) else state.edit.copy(focalY=it)) }
    EditSlider("Zoom", state.edit.zoom, 1f..4f) { vm.updateEdit(state.edit.copy(zoom = it)) }
    EditSlider("Horizontal focal point", if (portrait) state.edit.portraitX else state.edit.focalX, 0f..1f) { vm.updateEdit(if (portrait) state.edit.copy(portraitX = it) else state.edit.copy(focalX = it)) }
    EditSlider("Vertical focal point", if (portrait) state.edit.portraitY else state.edit.focalY, 0f..1f) { vm.updateEdit(if (portrait) state.edit.copy(portraitY = it) else state.edit.copy(focalY = it)) }
}
@Composable private fun EditSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    Column { Text("$label  ${"%.2f".format(value)}", style = MaterialTheme.typography.bodySmall); Slider(value.coerceIn(range), change, valueRange = range) }
}

@Composable private fun PrecisionNumber(label: String,value: Float,range: ClosedFloatingPointRange<Float>,save: (Float)->Unit) {
    var input by remember(value) { mutableStateOf("%.3f".format(java.util.Locale.ROOT,value)) }
    val parsed=input.toFloatOrNull()?.takeIf { it.isFinite() && it in range }
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(input,{input=it.take(12)},label={Text(label)},singleLine=true,modifier=Modifier.weight(1f),isError=parsed==null)
        TextButton(onClick={parsed?.let(save)},enabled=parsed!=null) { Text("Set") }
    }
}

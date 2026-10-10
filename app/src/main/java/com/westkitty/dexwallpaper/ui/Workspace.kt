package com.westkitty.dexwallpaper.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.westkitty.dexwallpaper.core.Destination
import com.westkitty.dexwallpaper.wallpaper.DexWallpaperService

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DexWorkspace(vm: DexViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val images = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importUris(uris)
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.importFolder(uri)
    }
    var includeBackupOriginals by remember { mutableStateOf(false) }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { vm.exportBackup(it, includeBackupOriginals) } }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.restoreBackup(it) } }
    val diagnostics = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportDiagnostics) }
    val exportSidecar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportSidecar) }
    val importSidecar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importSidecar) }
    val exportProfile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportProfile) }
    val importProfile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importProfile) }
    val livePickerResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> vm.completeLivePicker(result.resultCode == android.app.Activity.RESULT_OK) }
    var confirmLivePicker by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    BackHandler(state.page != Workspace.CONTROL) { vm.navigate(Workspace.CONTROL) }
    DexTheme(state.preferences) {
        val focus = remember { FocusRequester() }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding).onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    when (event.key) {
                        Key.DirectionRight -> { vm.next(); true }
                        Key.DirectionLeft -> { vm.previous(); true }
                        Key.L -> { vm.navigate(Workspace.LIBRARY); true }
                        Key.E -> { state.selected?.let { vm.select(it.id, true) }; true }
                        Key.Enter -> { vm.apply(); true }
                        Key.Z -> { if(state.page==Workspace.EDITOR) { vm.undoEdit(); true } else false }
                        else -> false
                    }
                } else false
            }.focusRequester(focus).focusable()) {
                val wide = maxWidth >= 900.dp
                Row(Modifier.fillMaxSize()) {
                    if (wide) Sidebar(state, vm::navigate)
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = if (wide) 28.dp else 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!wide) {
                                var navOpen by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { navOpen = true }) { Icon(Icons.Outlined.Menu, "Choose workspace") }
                                    DropdownMenu(navOpen, { navOpen = false }) { Workspace.entries.forEach { page -> DropdownMenuItem(text = { Text(page.label) }, onClick = { vm.navigate(page); navOpen = false }) } }
                                }
                                Text("DEX", style = MaterialTheme.typography.titleLarge)
                            } else Eyebrow("The Living Wall System")
                            Spacer(Modifier.weight(1f))
                            var targetOpen by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { targetOpen = true }) { Icon(Icons.Outlined.Wallpaper, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (wide) state.destination.label else when (state.destination) { Destination.HOME -> "Home"; Destination.LOCK -> "Lock"; Destination.BOTH -> "Both" }) }
                                DropdownMenu(targetOpen, { targetOpen = false }) { Destination.entries.forEach { target -> DropdownMenuItem(text = { Text(target.label) }, onClick = { vm.destination(target); targetOpen = false }) } }
                            }
                            Spacer(Modifier.width(8.dp))
                            var addOpen by remember { mutableStateOf(false) }
                            Box {
                                FilledTonalButton(onClick = { addOpen = true }, enabled = !state.busy) { Icon(Icons.Outlined.Add, null); if (wide) Text("Add images") }
                                DropdownMenu(addOpen, { addOpen = false }) {
                                    DropdownMenuItem(text = { Text("Choose images") }, onClick = { addOpen = false; images.launch(arrayOf("image/*")) })
                                    DropdownMenuItem(text = { Text("Choose a folder") }, onClick = { addOpen = false; folder.launch(null) })
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
                        if (state.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(state.progress, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = vm::cancel) { Text("Cancel") }
                            }
                        }
                        val livePicker = { confirmLivePicker = true }
                        Box(Modifier.weight(1f).padding(horizontal = if (wide) 28.dp else 16.dp, vertical = if (state.preferences.compact) 12.dp else 24.dp)) {
                            when (state.page) {
                                Workspace.CONTROL -> ControlCenter(vm, state, wide)
                                Workspace.LIBRARY -> LibraryWorkspace(vm, state)
                                Workspace.ROTATION -> RotationLab(vm, state)
                                Workspace.EDITOR -> EditorWorkspace(vm, state, wide, { exportSidecar.launch("dex-editing-sidecar.json") }, { importSidecar.launch(arrayOf("application/json", "text/plain")) })
                                Workspace.MOTION -> MotionWorkspace(vm, state, livePicker)
                                Workspace.AUTOMATION -> AutomationWorkspace(vm, state)
                                Workspace.SOURCES -> SourcesWorkspace(vm, state) { folder.launch(null) }
                                Workspace.HEALTH -> HealthWorkspace(vm, state, { originals -> includeBackupOriginals = originals; exportBackup.launch("dex-wallpaper-backup.zip") }, { restoreBackup.launch(arrayOf("application/zip", "application/octet-stream")) }, { diagnostics.launch("dex-sanitized-diagnostics.json") })
                                Workspace.SETTINGS -> SettingsWorkspace(vm, state, { exportProfile.launch("dex-visual-profile.json") }, { importProfile.launch(arrayOf("application/json", "text/plain")) })
                                Workspace.DATA -> LiveDataWorkspace(vm, state)
                                Workspace.GENERATIVE -> GenerativeWorkspace(vm, state)
                                Workspace.SCENE -> SceneWorkspace(vm, state)
                            }
                        }
                    }
                }
            }
        }
        if (confirmLivePicker) AlertDialog(onDismissRequest = { confirmLivePicker = false }, title = { Text("The system controls live destinations") },
            text = { Text("Requested: ${state.destination.label}. Android/OEM controls final assignment. Choose exactly that destination; cancel if a single-screen choice cannot preserve the other screen. Preview does not change installed DEX snapshots. A reported successful result commits only with matching engine ownership; otherwise return to Motion to review. Different simultaneous live services are not guaranteed.") },
            confirmButton = { TextButton(onClick = {
                confirmLivePicker = false
                vm.prepareLivePreview {
                    try { livePickerResult.launch(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, DexWallpaperService::class.java))) }
                    catch (_: android.content.ActivityNotFoundException) { vm.notify("No compatible live-wallpaper picker is installed.") }
                }
            }) { Text("I understand / open picker") } }, dismissButton = { TextButton(onClick = { confirmLivePicker = false }) { Text("Cancel") } })
    }
}
@Composable private fun Sidebar(state: UiState, navigate: (Workspace) -> Unit) {
    Column(Modifier.width(204.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.padding(top = 12.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Wallpaper, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text("DEX", style = MaterialTheme.typography.titleLarge); Text("WALLPAPER", style = MaterialTheme.typography.labelSmall) }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(Workspace.entries) { page ->
                val selected = state.page == page
                TextButton(onClick = { navigate(page) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text(if (selected) "—  ${page.shortLabel}" else "    ${page.shortLabel}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        HorizontalDivider()
        DataLine("Core", "Local • private • offline")
    }
}

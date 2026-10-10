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
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { /* Imported private copies do not need enduring source access. */ } }
            vm.importUris(uris)
        }
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); vm.importFolder(uri) }
            catch (_: SecurityException) { vm.notify("Folder provider refused persistent access. Import individual images instead.") }
        }
    }
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
                                OutlinedButton(onClick = { targetOpen = true }) { Icon(Icons.Outlined.Wallpaper, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(state.destination.label) }
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
                        val livePicker = {
                            vm.notify("Android's picker controls the destination. Choose Home only explicitly. If the picker offers only Both, cancel; DEX cannot guarantee Lock preservation for live wallpapers.")
                            try { context.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, DexWallpaperService::class.java))) }
                            catch (_: android.content.ActivityNotFoundException) { vm.notify("No compatible live-wallpaper picker is installed.") }
                        }
                        Box(Modifier.weight(1f).padding(horizontal = if (wide) 28.dp else 16.dp, vertical = if (state.preferences.compact) 12.dp else 24.dp)) {
                            when (state.page) {
                                Workspace.CONTROL -> ControlCenter(vm, state, wide)
                                Workspace.LIBRARY -> LibraryWorkspace(vm, state)
                                Workspace.ROTATION -> RotationLab(vm, state)
                                Workspace.EDITOR -> EditorWorkspace(vm, state, wide)
                                Workspace.MOTION -> MotionWorkspace(vm, state, livePicker)
                                Workspace.AUTOMATION -> AutomationWorkspace(vm, state)
                                Workspace.SOURCES -> SourcesWorkspace(vm, state) { folder.launch(null) }
                                Workspace.HEALTH -> HealthWorkspace(vm, state)
                                Workspace.SETTINGS -> SettingsWorkspace(vm, state)
                                Workspace.DATA -> LiveDataWorkspace(vm, state)
                                Workspace.GENERATIVE -> UnavailableWorkspace("Generative canvas", "On-device procedural compositions", "The generator is not implemented in this checkpoint. Imported artwork and the non-destructive editor remain available.") { vm.navigate(Workspace.EDITOR) }
                                Workspace.SCENE -> UnavailableWorkspace("Scene composer", "Portable visual environments", "Layered scenes and scene manifests are not implemented in this checkpoint. No external package scripts are executed.") { vm.navigate(Workspace.LIBRARY) }
                            }
                        }
                    }
                }
            }
        }
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
@Composable private fun UnavailableWorkspace(title: String, subtitle: String, reason: String, action: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionTitle(title, subtitle)
        Eyebrow("Not implemented")
        Text(reason)
        OutlinedButton(action) { Text("Open working tools") }
    }
}

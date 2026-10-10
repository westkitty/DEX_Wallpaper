@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.westkitty.dexwallpaper.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.platform.CompatibilityRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MotionWorkspace(vm: DexViewModel, state: UiState, chooseLive: () -> Unit) {
    val context=LocalContext.current
    val glAvailable=remember { context.getSystemService(android.app.ActivityManager::class.java).deviceConfigurationInfo.reqGlEsVersion>=0x20000 }
    var previewError by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        SectionTitle("MotionGlass", "GLES effects / precise transitions / visibility-aware fallback")
        if(state.selected!=null && glAvailable && !state.preferences.safeMode && previewError.isBlank()) {
            androidx.compose.ui.viewinterop.AndroidView(factory={ com.westkitty.dexwallpaper.wallpaper.ShaderPreviewView(it,vm.graph).apply { onUnavailable={ error -> previewError=error } } },
                modifier=Modifier.fillMaxWidth().aspectRatio(1.6f),update={ it.configure(state.selected?.id,state.preferences) },onRelease={ it.release() })
            Text("Real OpenGL ES preview • static frame, redrawn on effect changes",style=MaterialTheme.typography.bodySmall)
        } else ArtworkPreview(vm,state.selected)
        if(previewError.isNotBlank()) Text("$previewError • CPU composition shown instead.",color=MaterialTheme.colorScheme.error)
        Eyebrow("Shader effect")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            ShaderEffect.entries.forEach { effect -> FilterChip(state.preferences.shaderEffect==effect.name,onClick={vm.setText("shaderEffect",effect.name)},label={Text(effect.title)},enabled=glAvailable && !state.preferences.safeMode && previewError.isBlank()) }
        }
        Text("Effect intensity ${"%.2f".format(state.preferences.effectIntensity)}")
        Slider(state.preferences.effectIntensity,{vm.setDecimal("effectIntensity",it)},valueRange=0f..1f,enabled=glAvailable && !state.preferences.safeMode)
        Eyebrow("Transitions")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Transition.entries.forEach { t -> FilterChip(state.preferences.transition==t.name,onClick={vm.setText("transition",t.name)},label={Text(t.title)},enabled=!state.preferences.reducedMotion && glAvailable && !state.preferences.safeMode) } }
        Text("Duration ${state.preferences.transitionMs} ms")
        Slider(state.preferences.transitionMs.toFloat(),{vm.setNumber("transitionMs",it.toInt())},valueRange=100f..3000f,enabled=!state.preferences.reducedMotion)
        Text("Animation cap ${state.preferences.frameRate} fps; static scenes have no frame loop")
        Slider(state.preferences.frameRate.toFloat(),{vm.setNumber("frameRate",it.toInt())},valueRange=5f..60f)
        FlagRow("Launcher / swipe parallax",state.preferences.parallax,"Where the launcher forwards offsets or wallpaper touch. Offsets are bounded; no icon-position access.",enabled=glAvailable && !state.preferences.reducedMotion) { vm.setFlag("parallax",it) }
        val rotationSensor=remember { context.getSystemService(android.hardware.SensorManager::class.java).getDefaultSensor(android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR)!=null || context.getSystemService(android.hardware.SensorManager::class.java).getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)!=null }
        FlagRow("Tilt-responsive parallax",state.preferences.gyroParallax,"Rotation-vector sensor, visible engine only. Automatically released while hidden.",enabled=rotationSensor && glAvailable && !state.preferences.reducedMotion) { vm.setFlag("gyroParallax",it) }
        FlagRow("Slow zoom animation",state.preferences.zoomAnimation,"Bounded 1×–1.05× breathing zoom at the configured/adaptive frame cap.",enabled=glAvailable && !state.preferences.reducedMotion) { vm.setFlag("zoomAnimation",it) }
        FlagRow("Reduced motion",state.preferences.reducedMotion,"Disable transitions, parallax, and zoom without disabling your artwork.") { vm.setFlag("reducedMotion",it) }
        FlagRow("Safe mode",state.preferences.safeMode,"CPU static fallback; skip GPU effects, blur, vignette, and motion.") { vm.setFlag("safeMode",it) }
        OutlinedButton(onClick=vm::commitLive,enabled=state.selected!=null && !state.busy) { Text("Commit live snapshot / ${state.destination.label}") }
        Text("Controls above edit a preview draft. Installed wallpaper changes only when you explicitly commit a snapshot. Static Home/Lock changes and per-image edits never rebind a live Lock source. Independent live updates require Android 14+ engine ownership evidence; unknown/shared Home-only updates fail closed.",style=MaterialTheme.typography.bodySmall)
        Button(chooseLive,enabled=state.selected!=null && (android.os.Build.VERSION.SDK_INT>=34 || state.destination==Destination.BOTH)) { Text("Open Android live-wallpaper picker") }
        Text("GPU effects apply to the live engine, not static WallpaperManager writes. Clock/battery overlays use the event-driven Canvas fallback and bypass shaders. Vulkan, video/GIF decoders, and independent simultaneous live services are not implemented.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(android.os.Build.VERSION.SDK_INT<34) Text("This Android version cannot report live engine ownership. Home/Lock-only live setup is disabled; static destinations remain available. Both requires explicit selection.",style=MaterialTheme.typography.bodySmall)
        Text("LIVE DESTINATION LIMIT • The system picker decides the destination. Cancel if it cannot explicitly preserve Lock.",style=MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AutomationWorkspace(vm: DexViewModel, state: UiState) {
    var name by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf("08:00") }
    var end by rememberSaveable { mutableStateOf("18:00") }
    var priority by rememberSaveable { mutableStateOf("10") }
    var charging by rememberSaveable { mutableIntStateOf(-1) }
    var dark by rememberSaveable { mutableIntStateOf(-1) }
    var previewHours by rememberSaveable { mutableIntStateOf(24) }
    fun parseTime(text: String) = try { LocalTime.parse(text).let { it.hour * 60 + it.minute } } catch (_: Exception) { null }
    val rules = state.rules.filter { it.destination == state.destination.name }.map { it.model() }
    val context = vm.graph.repository.contextSnapshot()
    val decision = Chronoscape.evaluate(rules, context)
    val conflicts = Chronoscape.conflicts(rules)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("Chronoscape", "Rules that can explain themselves.")
        Text("Rules are evaluated by the scheduled rotation worker. They do not start a second timer. Manual changes use your selected collection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CollectionPicker(state, vm::collection)
        OutlinedTextField(name, { name = it }, label = { Text("Rule name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it.take(5) }, label = { Text("Start / HH:mm") }, modifier = Modifier.width(170.dp), singleLine = true, isError = parseTime(start) == null)
            OutlinedTextField(end, { end = it.take(5) }, label = { Text("End / HH:mm") }, modifier = Modifier.width(170.dp), singleLine = true, isError = parseTime(end) == null)
            OutlinedTextField(priority, { priority = it.filter { c -> c.isDigit() || c == '-' }.take(6) }, label = { Text("Priority") }, modifier = Modifier.width(140.dp), singleLine = true)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(charging == 1, onClick = { charging = if (charging == 1) -1 else 1 }, label = { Text("Charging only") })
            FilterChip(dark == 1, onClick = { dark = if (dark == 1) -1 else 1 }, label = { Text("Dark theme only") })
        }
        Button(onClick = { vm.addRule(name, parseTime(start)!!, parseTime(end)!!, priority.toIntOrNull() ?: 0, charging, dark); name = "" }, enabled = parseTime(start) != null && parseTime(end) != null && state.destination != Destination.BOTH) { Text("Add rule for this collection") }
        state.rules.filter { it.destination == state.destination.name }.forEach { rule ->
            FlagRow("${rule.name} / priority ${rule.priority}", rule.enabled, "${rule.startMinute / 60}:${"%02d".format(rule.startMinute % 60)} — ${rule.endMinute / 60}:${"%02d".format(rule.endMinute % 60)} · ${state.collections.firstOrNull { it.id == rule.collectionId }?.name ?: rule.collectionId}") { vm.toggleRule(rule) }
            TextButton(onClick = { vm.deleteRule(rule.id) }) { Text("Remove rule") }
        }
        HorizontalDivider()
        Eyebrow("Dry run / rule debugger")
        Text("Evaluation uses current time, charging, battery, and theme. It does NOT change your wallpaper.")
        if (decision.evaluations.isEmpty()) Text("No rules configured. The selected collection is the fallback.")
        decision.evaluations.forEach { e -> Text("${e.rule.name} → ${e.reason}", color = if (e.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        if (conflicts.isNotEmpty()) { Text("${conflicts.size} overlapping equal-priority conflict(s). A stable rule ID breaks ties deterministically.", color = MaterialTheme.colorScheme.error); conflicts.forEach { Text("${it.first.take(8)} ↔ ${it.second.take(8)}", style = MaterialTheme.typography.labelSmall) } }
        Eyebrow("Future preview / no writes")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(previewHours == 24, { previewHours = 24 }, label = { Text("24 hours") }); FilterChip(previewHours == 168, { previewHours = 168 }, label = { Text("7 days") }) }
        Text("Hourly simulation; charging, battery, and theme are held at the current snapshot. Not an exact Android execution forecast.", style = MaterialTheme.typography.bodySmall)
        Chronoscape.preview(rules, context, previewHours).take(24).forEach { (time, collection) -> Text("${time.format(DateTimeFormatter.ofPattern("EEE HH:mm"))} → ${state.collections.firstOrNull { it.id == collection }?.name ?: "Default collection"}", style = MaterialTheme.typography.bodySmall) }
        if (previewHours > 24) Text("${previewHours - 24} further hours evaluated; first 24 shown to keep the workspace compact.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun SourcesWorkspace(vm: DexViewModel, state: UiState, chooseFolder: () -> Unit) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("Wallpaper Bridge", "Authorized folders / verified imports / offline originals")
        Text("Choose a local or Syncthing-managed folder through Android's Storage Access Framework. DEX reads only the selected tree, copies images atomically, and identifies exact duplicates by SHA-256.")
        Button(chooseFolder) { Text("Authorize a source folder") }
        FlagRow("Include hidden folders when rescanning", hidden, "Explicit opt-in. Restricted to your authorized folder; no broad filesystem access.") { hidden = it }
        state.collections.forEach { c ->
            DataLine(c.name, if (c.sourceTree.isBlank()) "Manual collection · no linked folder" else "Authorized folder · persistent read access requested")
            if (c.sourceTree.isNotBlank()) OutlinedButton(onClick = { vm.rescan(c, hidden) }, enabled = !state.busy) { Text("Synchronize ${c.name}") }
        }
        HorizontalDivider()
        Eyebrow("Transfer integrity")
        Text("Protected copies / 80 MB per image / 5,000 files per scan / depth 16 / .partial staging / decode validation / exact duplicate linking")
        Eyebrow("Network connectors / unavailable")
        Text("Nextcloud, WebDAV, online artwork discovery, premium providers, and cross-device favorite synchronization are not connected in this build. No remote credentials or user data are transmitted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun HealthWorkspace(vm: DexViewModel, state: UiState, backup: (Boolean) -> Unit, restore: () -> Unit, diagnostics: () -> Unit) {
    val context = LocalContext.current
    val capabilities = remember { CompatibilityRegistry(context).inspect() }
    var refresh by remember { mutableIntStateOf(0) }
    var originals by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var metrics by remember { mutableStateOf(com.westkitty.dexwallpaper.wallpaper.RenderTelemetry.snapshot()) }
    val sizes by produceState<List<Pair<String, Long>>>(emptyList(), refresh, state.assets.size) {
        value = withContext(Dispatchers.IO) {
            fun bytes(file: java.io.File): Long = if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else file.length()
            listOf("Originals" to bytes(vm.graph.importer.originals), "Thumbnails" to bytes(vm.graph.importer.thumbnails),
                "Database" to bytes(context.getDatabasePath("living-wall.db")), "Cache" to bytes(context.cacheDir))
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("System health", "Capability negotiation, storage, and safe recovery")
        FlagRow("One-touch safe mode", state.preferences.safeMode, "Disable expensive effects; preserve collections, schedules, and editing sidecars.") { vm.setFlag("safeMode", it) }
        Eyebrow("Storage budget")
        sizes.forEach { (name, bytes) -> DataLine(name, "${"%.1f".format(bytes / 1048576.0)} MB") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.validateAssets(); refresh++ }, enabled = !state.busy) { Text("Check missing files") }
            OutlinedButton(onClick = { vm.graph.scope.launch { vm.graph.importer.thumbnails.listFiles()?.forEach { it.delete() }; vm.notify("Thumbnail cache cleared; originals kept."); refresh++ } }) { Text("Clear thumbnails") }
        }
        Text("Missing originals are quarantined, not deleted. Re-import the same source to recover matching hashes without changing identity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DataLine("Thumbnail cache limit", "${state.preferences.cacheMb} MB; originals are never evicted")
        Slider(state.preferences.cacheMb.toFloat(), { vm.setNumber("cacheMb", it.toInt()) }, valueRange = 16f..512f)
        HorizontalDivider()
        Eyebrow("Backup / recovery")
        FlagRow("Include original artwork", originals, "Backups may contain private images. Choose a trusted destination. Credentials and device-specific URI grants are always excluded.") { originals = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { backup(originals) }, enabled = !state.busy) { Text("Export backup") }
            OutlinedButton(onClick = { confirmRestore = true }, enabled = !state.busy) { Text("Restore backup") }
        }
        OutlinedButton(diagnostics, enabled = !state.busy) { Text("Export sanitized diagnostics") }
        Text("Restore merges metadata by verified hashes. Rotation and rules are paused; it never applies a wallpaper automatically. Original/source images, grants, and private data are not included in diagnostic exports.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Eyebrow("Measured renderer / process counters")
        DataLine("Frames", "${metrics.totalFrames} actual frames; mean ${"%.2f".format(metrics.meanFrameMs)} ms over up to 120 frames; last ${"%.2f".format(metrics.lastFrameMs)} ms")
        DataLine("Resources", "${metrics.engines} live engines / ${metrics.contexts} EGL contexts / ${metrics.textures} textures / ${metrics.contextRecoveries} recovery attempts")
        if (metrics.lastError.isNotBlank()) Text(metrics.lastError, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = { metrics = com.westkitty.dexwallpaper.wallpaper.RenderTelemetry.snapshot(); refresh++ }) { Text("Refresh measured snapshot") }
        Eyebrow("Device compatibility")
        capabilities.forEach { c -> DataLine(c.name, "${c.state.name.replace('_',' ')}\n${c.explanation}") }
        Eyebrow("Build status")
        Text("Device behavior requires Android verification. Compilation and unit-test evidence is recorded separately in TEST_REPORT.md; this screen never invents a successful device check.")
    }
    if (confirmRestore) AlertDialog(onDismissRequest = { confirmRestore = false }, title = { Text("Restore visual environment?") },
        text = { Text("This merges artwork metadata and replaces matching collection/settings records. Rotation and rules are paused for destination review. Original source files are not deleted. Export a backup of current settings first if needed.") },
        confirmButton = { TextButton(onClick = { confirmRestore = false; restore() }) { Text("Choose backup") } }, dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } })
}
@Composable fun SettingsWorkspace(vm: DexViewModel, state: UiState, exportProfile: () -> Unit, importProfile: () -> Unit) {
    val context=LocalContext.current
    var legal by remember { mutableStateOf("") }
    var profileName by rememberSaveable { mutableStateOf("") }
    val customProfiles by produceState<List<VisualProfile>>(emptyList(),state.busy) { value=vm.graph.profiles.saved() }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("Workspace settings", "Precise defaults. Optional complexity.")
        Eyebrow("DeskShift / visual profiles")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { (DeskShift.presets+customProfiles).forEach { profile -> OutlinedButton(onClick={vm.loadProfile(profile)},enabled=!state.busy) { Text(profile.name) } } }
        Text("Profiles change the selected destination configuration, workspace motion/clock settings, and selected artwork's effects. They never apply wallpaper automatically. Missing transferred collection IDs keep your existing collection.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(profileName,{profileName=it.take(80)},label={Text("Custom profile name")},singleLine=true)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.saveProfile(profileName)},enabled=profileName.isNotBlank() && !state.busy) { Text("Save custom profile") }
            OutlinedButton(exportProfile,enabled=!state.busy) { Text("Export profile") }
            OutlinedButton(importProfile,enabled=!state.busy) { Text("Import & load profile") }
        }
        HorizontalDivider()
        DataLine("Wallpaper destination", "${state.destination.label} · default is Home only")
        FlagRow("High contrast", state.preferences.highContrast, "Brighter text and control outlines.") { vm.setFlag("highContrast", it) }
        FlagRow("Compact density", state.preferences.compact, "Denser galleries and smaller workspace margins; touch targets remain accessible.") { vm.setFlag("compact", it) }
        FlagRow("Reduced motion", state.preferences.reducedMotion, "Motion-free wallpaper fallback; no change to your collection.") { vm.setFlag("reducedMotion", it) }
        FlagRow("Material You colors", state.preferences.dynamicColors, "Use Android wallpaper-derived colors on Android 12+.", enabled = android.os.Build.VERSION.SDK_INT >= 31) { vm.setFlag("dynamicColors", it) }
        FlagRow("Safe mode", state.preferences.safeMode, "Bypass blur and vignette while keeping their saved values.") { vm.setFlag("safeMode", it) }
        HorizontalDivider()
        Eyebrow("Privacy first")
        Text("No account, advertisements, analytics, telemetry, or backend. Images and state stay in application-private storage. Permissions are requested only through supported Android pickers.")
        Eyebrow("External automation")
        FlagRow("Allow external wallpaper commands", state.preferences.externalCommands, "Off by default. When enabled, any installed app can send whitelisted commands. No arbitrary code, paths, or executables are accepted.") { vm.setFlag("externalCommands", it) }
        FlagRow("Authorize external Lock commands", state.preferences.externalLockCommands, "Requires external commands above. Home-only remains the default.", enabled = state.preferences.externalCommands) { vm.setFlag("externalLockCommands", it) }
        FlagRow("Authorize external Both commands", state.preferences.externalBothCommands, "Explicitly allows third-party commands to change both screens. Off by default.", enabled = state.preferences.externalCommands) { vm.setFlag("externalBothCommands", it) }
        Text("Tasker / Send Intent: action com.westkitty.dexwallpaper.COMMAND; explicit package com.westkitty.dexwallpaper; extras command=NEXT, destination=HOME. Supported: NEXT, PREVIOUS, PAUSE, RESUME, APPLY (assetId), SWITCH_COLLECTION (collectionId). Both-screen application is always an explicit target. IDs are visible in library/rule inspectors.", style = MaterialTheme.typography.bodySmall)
        Text("Quick Settings: add Next wallpaper. Home widget: Next / Pause / Album / Select. These controls are Home-only by design. Launcher long-press exposes Next Home and Library.", style = MaterialTheme.typography.bodySmall)
        Eyebrow("Keyboard")
        Text("Ctrl + →  Next\nCtrl + ←  Previous\nCtrl + L  Library\nCtrl + E  Edit selection\nCtrl + Enter  Apply\nTab / Shift + Tab  Focus navigation")
        OutlinedButton(onClick={ legal=listOf("LICENSE","THIRD_PARTY_NOTICES.md","PRIVACY.md").joinToString("\n\n") { name -> context.assets.open(name).bufferedReader().use { it.readText() } } }) { Text("Read licenses & privacy") }
        Eyebrow("Scheduling")
        Text("Automatic rotation is approximate and independent for Home and Lock. Both-screen selection is explicit and manual. Exact alarms and direct-boot storage are not requested.")
    }
    if (legal.isNotEmpty()) AlertDialog(onDismissRequest={legal=""},title={Text("Licenses & privacy")},text={Text(legal,Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.bodySmall)},confirmButton={TextButton(onClick={legal=""}) { Text("Close") }})
}
@Composable fun LiveDataWorkspace(vm: DexViewModel, state: UiState) {
    val context = LocalContext.current
    var battery by remember { mutableIntStateOf(0) }
    var charging by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(LocalTime.now()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
                battery = level * 100 / scale
                charging = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(Unit) { while (true) { clock = LocalTime.now(); kotlinx.coroutines.delay(60_000 - System.currentTimeMillis() % 60_000) } }
    val power = context.getSystemService(PowerManager::class.java)
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val network = cm.getNetworkCapabilities(cm.activeNetwork)
    val memory = Runtime.getRuntime()
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionTitle("Ambient Desk", "Local device data / optional wallpaper overlay")
        Text(clock.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.headlineLarge)
        DataLine("Date", java.time.LocalDate.now().toString())
        DataLine("Battery", "$battery%${if (charging) " · charging" else ""}${if (power.isPowerSaveMode) " · Battery Saver" else ""}")
        DataLine("Network", if (network == null) "Offline" else if (network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "Wi-Fi" else "Connected / other transport")
        DataLine("Process memory snapshot", "${(memory.totalMemory() - memory.freeMemory()) / 1048576} MB Java heap used (not total device RAM)")
        FlagRow("Clock on live wallpaper", state.preferences.clockOverlay, "Minute-aligned event updates only while visible. Requires selecting the live service.") { vm.setFlag("clockOverlay", it) }
        FlagRow("Battery on live wallpaper",state.preferences.batteryOverlay,"Minute-boundary local battery snapshot on the Canvas live engine.") { vm.setFlag("batteryOverlay",it) }
        Text("Calendar, weather, media, health, geographic data, and CPU usage are unavailable until their authorized integrations are implemented. DEX does not invent live data or request their permissions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

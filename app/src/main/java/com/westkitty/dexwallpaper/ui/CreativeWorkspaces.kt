package com.westkitty.dexwallpaper.ui

import android.graphics.Color
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.creative.*
import com.westkitty.dexwallpaper.creative.Composition as ArtComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable fun GenerativeWorkspace(vm: DexViewModel, state: UiState) {
    var seed by rememberSaveable { mutableStateOf("42") }
    var patternName by rememberSaveable { mutableStateOf(Pattern.CONTOURS.name) }
    var primary by rememberSaveable { mutableStateOf("#FF4655") }
    var secondary by rememberSaveable { mutableStateOf("#E5D5BF") }
    var background by rememberSaveable { mutableStateOf("#101014") }
    var density by rememberSaveable { mutableFloatStateOf(18f) }
    var portrait by rememberSaveable { mutableStateOf(false) }
    val pattern=Pattern.valueOf(patternName)
    fun color(text: String) = try { Color.parseColor(text) } catch (_: IllegalArgumentException) { null }
    val composition=if(seed.toLongOrNull()!=null && listOf(primary,secondary,background).all { color(it)!=null })
        ArtComposition(seed.toLong(),pattern,color(background)!!,color(primary)!!,color(secondary)!!,density.toInt()) else null
    val preview by produceState<android.graphics.Bitmap?>(null,composition,portrait) {
        value=composition?.let { c -> withContext(Dispatchers.Default) { vm.graph.creative.generator.render(c,if(portrait) 625 else 1000,if(portrait) 1000 else 625) } }
    }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        SectionTitle("Generative canvas", "Offline recipes. Reproducible seeds. Your palette.")
        Box(Modifier.fillMaxWidth().heightIn(max=440.dp).aspectRatio(if(portrait) .625f else 1.6f).background(MaterialTheme.colorScheme.surfaceContainer),contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(),"${pattern.title}, seed $seed",Modifier.fillMaxSize()) } ?: Text("Enter a valid seed and hexadecimal palette.")
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Pattern.entries.forEach { p -> FilterChip(pattern==p,onClick={patternName=p.name},label={Text(p.title)}) } }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(seed,{seed=it.take(20)},label={Text("Seed")},singleLine=true,modifier=Modifier.width(170.dp),isError=seed.toLongOrNull()==null)
            OutlinedButton(onClick={seed=kotlin.random.Random.nextLong().toString()}) { Text("New variation") }
            FilterChip(portrait,onClick={portrait=!portrait},label={Text(if(portrait) "Portrait" else "Landscape")})
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(primary,{primary=it.take(9)},label={Text("Primary / hex")},singleLine=true,modifier=Modifier.width(180.dp),isError=color(primary)==null)
            OutlinedTextField(secondary,{secondary=it.take(9)},label={Text("Secondary / hex")},singleLine=true,modifier=Modifier.width(180.dp),isError=color(secondary)==null)
            OutlinedTextField(background,{background=it.take(9)},label={Text("Background / hex")},singleLine=true,modifier=Modifier.width(180.dp),isError=color(background)==null)
        }
        Text("Pattern density: ${density.toInt()}")
        Slider(density,{density=it},valueRange=4f..64f,steps=59)
        CollectionPicker(state,vm::collection)
        Button(onClick={ composition?.let { vm.generate(it,if(portrait) 1848 else 2960,if(portrait) 2960 else 1848) } },enabled=composition!=null && !state.busy) { Text("Save native-resolution composition") }
        Text("Saved compositions become ordinary protected artwork, so favorites, collections, rotation, editing, and Home/Lock safety apply without a second content store.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        state.selected?.takeIf { it.attribution.startsWith("Generated offline") }?.let { asset ->
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={vm.favorite(asset.id)}) { Text(if(asset.favorite) "Unfavorite composition" else "Favorite composition") }; Button(onClick={vm.apply(asset.id)},enabled=!state.busy) { Text("Apply saved composition") } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SceneWorkspace(vm: DexViewModel, state: UiState) {
    val initial=remember { SceneCodec.encode(SceneManifest(title="Untitled scene",layers=listOf(SceneLayer("title",LayerKind.TEXT,text="DEX",x=.08f,y=.78f,width=.5f,height=.09f)))) }
    var json by rememberSaveable { mutableStateOf(initial) }
    var sceneId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLayerId by rememberSaveable { mutableStateOf("title") }
    var raw by rememberSaveable { mutableStateOf(false) }
    var time by rememberSaveable { mutableFloatStateOf(0f) }
    var variableName by rememberSaveable { mutableStateOf("") }
    var variableValue by rememberSaveable { mutableStateOf("0") }
    var revisionToken by remember { mutableIntStateOf(0) }
    val decoded=remember(json) { SceneCodec.decode(json) }
    val scene=(decoded as? Outcome.Ok)?.value
    val selected=scene?.layers?.firstOrNull { it.id==selectedLayerId }
    fun update(new: SceneManifest) { json=SceneCodec.encode(new) }
    fun updateLayer(new: SceneLayer) { scene?.let { update(it.copy(layers=it.layers.map { l -> if(l.id==new.id) new else l })) } }
    val preview by produceState<android.graphics.Bitmap?>(null,json,time) {
        value=scene?.let { s -> withContext(Dispatchers.IO) { try { vm.graph.creative.renderer.render(s,1000,(1000f*s.height/s.width).toInt().coerceIn(64,1600),time.toDouble()) } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null } } }
    }
    val revisions by produceState<List<com.westkitty.dexwallpaper.data.SceneRevisionEntity>>(emptyList(),sceneId,revisionToken,state.scenes) { value=sceneId?.let { vm.graph.repository.dao.revisions(it) } ?: emptyList() }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        SectionTitle("Scene composer", "Living Epochs / neutral layers, bounded expressions, versioned revisions")
        Box(Modifier.fillMaxWidth().aspectRatio(scene?.let { it.width.toFloat()/it.height } ?: 1.6f).background(MaterialTheme.colorScheme.surfaceContainer),contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(),"Rendered scene composition",Modifier.fillMaxSize()) } ?: Text((decoded as? Outcome.Error)?.message ?: "Scene preview unavailable")
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            state.scenes.forEach { saved -> FilterChip(sceneId==saved.id,onClick={json=saved.manifest;sceneId=saved.id;selectedLayerId=""},label={Text(saved.name)}) }
            OutlinedButton(onClick={json=initial;sceneId=null;selectedLayerId="title"}) { Text("New scene") }
            FilterChip(raw,onClick={raw=!raw},label={Text("Manifest inspector")})
        }
        if(raw) {
            OutlinedTextField(json,{json=it.take(SceneCodec.MAX_BYTES)},label={Text("Scene manifest / schema 1")},modifier=Modifier.fillMaxWidth().heightIn(min=220.dp,max=440.dp),textStyle=MaterialTheme.typography.bodySmall,isError=scene==null)
            Text("Only declarative layers and numeric formulas. No scripts, filesystem paths, remote libraries, or proprietary Wallpaper Engine binaries.",style=MaterialTheme.typography.bodySmall)
        }
        if(scene!=null) {
            OutlinedTextField(scene.title,{ if(it.isNotBlank()) update(scene.copy(title=it.take(120))) },label={Text("Scene title")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Eyebrow("Layers / back to front")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                scene.layers.forEach { l -> FilterChip(selectedLayerId==l.id,onClick={selectedLayerId=l.id},label={Text("${scene.layers.indexOf(l)+1} ${l.kind.name.lowercase()}")}) }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                LayerKind.entries.forEach { kind -> OutlinedButton(onClick={ val id=UUID.randomUUID().toString(); update(scene.copy(layers=scene.layers+SceneLayer(id,kind,text=if(kind==LayerKind.TEXT) "Text" else "",assetId=if(kind==LayerKind.IMAGE) state.selected?.id ?: "" else ""))); selectedLayerId=id },enabled=scene.layers.size<64 && (kind!=LayerKind.IMAGE || state.selected!=null)) { Text("+ ${kind.name.lowercase()}") } }
            }
            if(selected!=null) {
                Eyebrow("Layer inspector")
                if(selected.kind==LayerKind.TEXT) OutlinedTextField(selected.text,{updateLayer(selected.copy(text=it.take(1000)))},label={Text("Text")},modifier=Modifier.fillMaxWidth())
                LayerSlider("X",selected.x,-1f..2f) { updateLayer(selected.copy(x=it)) }
                LayerSlider("Y",selected.y,-1f..2f) { updateLayer(selected.copy(y=it)) }
                LayerSlider("Width",selected.width,.001f..2f) { updateLayer(selected.copy(width=it)) }
                LayerSlider("Height / type size",selected.height,.001f..2f) { updateLayer(selected.copy(height=it)) }
                LayerSlider("Opacity",selected.opacity,0f..1f) { updateLayer(selected.copy(opacity=it)) }
                LayerSlider("Rotation",selected.rotation,-180f..180f) { updateLayer(selected.copy(rotation=it)) }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("sans-serif","serif","monospace").forEach { font -> FilterChip(selected.font==font,onClick={updateLayer(selected.copy(font=font))},label={Text(font)}) } }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("NORMAL","MULTIPLY","SCREEN","ADD").forEach { blend -> FilterChip(selected.blend==blend,onClick={updateLayer(selected.copy(blend=blend))},label={Text(blend.lowercase())}) } }
                var condition by remember(selected.id,selected.visibleWhen) { mutableStateOf(selected.visibleWhen) }
                var formula by remember(selected.id,selected.xFormula) { mutableStateOf(selected.xFormula) }
                OutlinedTextField(condition,{condition=it.take(512)},label={Text("Visible when / e.g. battery > 20")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(formula,{formula=it.take(512)},label={Text("X formula / e.g. 0.2 + sin(time) * 0.1")},modifier=Modifier.fillMaxWidth())
                TextButton(onClick={try { updateLayer(selected.copy(visibleWhen=condition,xFormula=formula)) } catch(e: Exception) { vm.notify(e.message ?: "Invalid formula") }}) { Text("Validate & save expressions") }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={update(scene.copy(layers=scene.layers.filter { it.id!=selected.id }));selectedLayerId=""}) { Text("Remove layer") }
                    TextButton(onClick={val list=scene.layers.toMutableList();val i=list.indexOf(selected);if(i<list.lastIndex){list.removeAt(i);list.add(i+1,selected);update(scene.copy(layers=list))}}) { Text("Bring forward") }
                }
            }
            HorizontalDivider()
            Eyebrow("Persistent scene variables")
            scene.variables.forEach { (k,v) -> Text("$k = $v",style=MaterialTheme.typography.bodySmall) }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(variableName,{variableName=it.take(41)},label={Text("Variable name")},modifier=Modifier.width(200.dp),singleLine=true)
                OutlinedTextField(variableValue,{variableValue=it.take(20)},label={Text("Value")},modifier=Modifier.width(160.dp),singleLine=true)
                OutlinedButton(onClick={try { update(scene.copy(variables=scene.variables+(variableName to variableValue.toDouble())));variableName="" } catch(e: Exception) { vm.notify(e.message ?: "Invalid variable") }},enabled=variableValue.toDoubleOrNull()!=null) { Text("Save variable") }
            }
            Eyebrow("Timeline simulation / static export")
            LayerSlider("Time / seconds",time,0f..scene.durationSeconds.toFloat()) { time=it }
            Text("Formula previews and exported static frames work locally. Live animated scene playback is not enabled by this editor yet.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={vm.saveScene(sceneId,scene);revisionToken++},enabled=!state.busy) { Text("Save revision") }; OutlinedButton(onClick={vm.renderScene(scene,time.toDouble())},enabled=!state.busy) { Text("Render to library") } }
            Eyebrow("Scene history")
            revisions.forEach { r -> TextButton(onClick={json=r.manifest;revisionToken++}) { Text("Restore revision ${r.revision} · ${formatTime(r.createdAt)}") } }
            if(revisions.isEmpty()) Text("Save a scene, then select its saved entry to inspect revision history.",style=MaterialTheme.typography.bodySmall)
            Text("Neutral sequence metadata (epoch / sequenceIndex) is editable in the manifest. No Starsilk characters, events, or canon are fabricated.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable private fun LayerSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,change: (Float)->Unit) {
    Column { Text("$label ${"%.2f".format(value)}",style=MaterialTheme.typography.bodySmall); Slider(value.coerceIn(range),change,valueRange=range) }
}

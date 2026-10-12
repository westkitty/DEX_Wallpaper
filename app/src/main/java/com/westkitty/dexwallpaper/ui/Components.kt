package com.westkitty.dexwallpaper.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.westkitty.dexwallpaper.core.ImageEdit
import com.westkitty.dexwallpaper.core.VideoClips
import com.westkitty.dexwallpaper.data.AssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun Eyebrow(text: String) { Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
@Composable fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun DataLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun FlagRow(label: String, checked: Boolean, detail: String, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = onChecked, enabled = enabled)
    }
}
@Composable fun ArtworkPreview(vm: DexViewModel, asset: AssetEntity?, edit: ImageEdit? = null, revision: Int = 0,
    modifier: Modifier = Modifier, portrait: Boolean = false, original: Boolean = false) {
    val image by produceState<android.graphics.Bitmap?>(null, asset?.id, edit, revision, portrait, original) {
        value = null
        if (asset != null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val file = vm.graph.importer.file(asset)
                    if (VideoClips.isVideo(asset)) VideoClips.poster(file,if(portrait) 625 else 1000,if(portrait) 1000 else 625)
                    else if (original) vm.graph.pipeline.decode(file, 1000, 1000)
                    else vm.graph.pipeline.render(file, if (portrait) 625 else 1000, if (portrait) 1000 else 625,
                        edit ?: vm.graph.repository.dao.edit(asset.id)?.model() ?: ImageEdit(), vm.state.value.preferences.safeMode)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                  catch (_: Exception) { null }
            }
        }
    }
    // Compose's Image painter shares the backing Android Bitmap. Disposal of the
    // producer state does not mean the draw node has finished using those pixels.
    // Let GC reclaim the bitmap when Compose releases its final reference.
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).aspectRatio(if (portrait) .625f else 1.6f), contentAlignment = Alignment.Center) {
        if (image != null) Image(image!!.asImageBitmap(), "Preview of ${asset?.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(if (asset == null) "Your wall, your artwork" else "Preview unavailable")
            Text(if (asset == null) "Import a folder or a few images to begin." else "File missing, corrupt, or not yet decoded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun CollectionPicker(state: UiState, onCollection: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(state.collection?.name ?: "Personal library") }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            state.collections.forEach { c -> DropdownMenuItem(text = { Text(if (c.pinned) "Pinned · ${c.name}" else c.name) }, onClick = { onCollection(c.id); open = false }) }
        }
    }
}
fun formatTime(time: Long) = if (time <= 0) "Not yet" else java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm"))

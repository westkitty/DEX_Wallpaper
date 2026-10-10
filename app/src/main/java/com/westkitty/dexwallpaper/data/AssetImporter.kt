package com.westkitty.dexwallpaper.data

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.westkitty.dexwallpaper.wallpaper.BitmapPipeline
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class ImportProgress(val processed: Int = 0, val imported: Int = 0, val duplicates: Int = 0, val rejected: Int = 0, val message: String = "")
class AssetImporter(private val context: Context, private val db: DexDatabase, private val pipeline: BitmapPipeline) {
    val originals = File(context.filesDir, "originals").apply { mkdirs() }
    val thumbnails = File(context.cacheDir, "thumbnails").apply { mkdirs() }
    fun file(asset: AssetEntity) = File(originals, asset.relativePath)

    suspend fun importUris(uris: List<Uri>, collection: String, progress: (ImportProgress) -> Unit): ImportProgress = withContext(Dispatchers.IO) {
        var state = ImportProgress()
        for (uri in uris.take(5000)) {
            currentCoroutineContext().ensureActive()
            try {
                val result = importOne(uri, collection)
                state = state.copy(processed = state.processed + 1, imported = state.imported + if (result) 1 else 0,
                    duplicates = state.duplicates + if (result) 0 else 1, message = "Imported ${state.processed + 1} files")
            } catch (e: java.io.IOException) { state = state.copy(processed = state.processed + 1, rejected = state.rejected + 1, message = e.message ?: "I/O error") }
              catch (e: IllegalArgumentException) { state = state.copy(processed = state.processed + 1, rejected = state.rejected + 1, message = e.message ?: "Invalid image") }
              catch (e: SecurityException) { state = state.copy(processed = state.processed + 1, rejected = state.rejected + 1, message = "Source access denied; choose it again") }
            progress(state)
        }
        state
    }
    suspend fun scanTree(uri: Uri, collection: String, includeHidden: Boolean, progress: (ImportProgress) -> Unit): ImportProgress = withContext(Dispatchers.IO) {
        val root = requireNotNull(DocumentFile.fromTreeUri(context, uri)) { "Folder provider unavailable" }
        val uris = mutableListOf<Uri>()
        suspend fun walk(node: DocumentFile, depth: Int) {
            currentCoroutineContext().ensureActive()
            if (depth > 16 || uris.size >= 5000) return
            node.listFiles().forEach { child ->
                if (!includeHidden && (child.name ?: "").startsWith('.')) return@forEach
                if (child.isDirectory) walk(child, depth + 1)
                else if (child.isFile && ((child.type ?: "").startsWith("image/") || (child.name ?: "").substringAfterLast('.').lowercase() in setOf("jpg","jpeg","png","webp","heic","heif","bmp","gif"))) {
                    if (uris.size < 5000) uris += child.uri
                }
            }
        }
        progress(ImportProgress(message = "Scanning authorized folder…"))
        walk(root, 0)
        importUris(uris, collection, progress)
    }
    private suspend fun importOne(uri: Uri, collection: String): Boolean {
        val id = UUID.randomUUID().toString()
        val temp = File(originals, "$id.partial")
        val target = File(originals, "$id.img")
        var committed = false
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            context.contentResolver.openInputStream(uri).use { source ->
                requireNotNull(source) { "Source cannot be read" }
                temp.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val size = source.read(buffer)
                        if (size < 0) break
                        count += size
                        require(count <= 80L * 1024 * 1024) { "Image exceeds 80 MB import limit" }
                        digest.update(buffer, 0, size)
                        out.write(buffer, 0, size)
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val existing = db.library().byHash(hash)
            if (existing != null) {
                db.library().addMembership(Membership(collection, existing.id))
                return false
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Corrupt or unsupported image" }
            val decoded = pipeline.decode(temp, 320, 320)
            val phash: Long
            try {
                phash = pipeline.perceptualHash(decoded)
                File(thumbnails, "$id.jpg").outputStream().use { decoded.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
            } finally { decoded.recycle() }
            require(temp.renameTo(target)) { "Could not finalize imported image" }
            val name = if (uri.scheme == "file") File(requireNotNull(uri.path)).name else DocumentFile.fromSingleUri(context, uri)?.name ?: "Imported artwork"
            db.withTransaction {
                db.library().insertAsset(AssetEntity(id, name.take(200), target.name, uri.toString(), hash,
                    bounds.outWidth, bounds.outHeight, count, System.currentTimeMillis(), perceptualHash = phash))
                db.library().addMembership(Membership(collection, id))
            }
            committed = true
            return true
        } finally {
            temp.delete()
            if (!committed) { target.delete(); File(thumbnails, "$id.jpg").delete() }
        }
    }
    suspend fun validateAssets(): Int = withContext(Dispatchers.IO) {
        var missing = 0
        db.library().allAssets().forEach { asset ->
            val broken = !file(asset).isFile
            if (broken) missing++
            if (asset.quarantined != broken) db.library().updateAsset(asset.copy(quarantined = broken))
        }
        missing
    }
    suspend fun thumbnail(asset: AssetEntity): File = withContext(Dispatchers.IO) {
        val target = File(thumbnails, "${asset.id}.jpg")
        if (!target.isFile && file(asset).isFile) {
            val bitmap = pipeline.decode(file(asset), 320, 320)
            try { target.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) } }
            finally { bitmap.recycle() }
        }
        target
    }
}

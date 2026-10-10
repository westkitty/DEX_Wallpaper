package com.westkitty.dexwallpaper.data

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.westkitty.dexwallpaper.wallpaper.BitmapPipeline
import com.westkitty.dexwallpaper.core.CachePolicy
import com.westkitty.dexwallpaper.core.CacheEntry
import com.westkitty.dexwallpaper.core.ImportTraversalBudget
import com.westkitty.dexwallpaper.core.LibraryFilePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class ImportProgress(
    val processed: Int = 0,
    val imported: Int = 0,
    val duplicates: Int = 0,
    val rejected: Int = 0,
    val message: String = "",
    val scannedEntries: Int = 0,
    val truncated: Boolean = false,
)
class AssetImporter(private val context: Context, private val db: DexDatabase, private val pipeline: BitmapPipeline) {
    val originals = File(context.filesDir, "originals").apply { mkdirs() }
    val thumbnails = File(context.cacheDir, "thumbnails").apply { mkdirs() }
    private val thumbnailLocks = Array(16) { Mutex() }

    fun file(asset: AssetEntity): File = LibraryFilePath.resolve(originals, asset.relativePath)

    suspend fun importUris(uris: List<Uri>, collection: String, progress: (ImportProgress) -> Unit): ImportProgress = withContext(Dispatchers.IO) {
        val wasCapped = uris.size > ImportTraversalBudget.DEFAULT_MAXIMUM_ENTRIES
        var state = ImportProgress(truncated = wasCapped)
        for (uri in uris.take(ImportTraversalBudget.DEFAULT_MAXIMUM_ENTRIES)) {
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
        require(uri.scheme == "content") { "Choose a folder from Android's document picker." }
        val rootId = DocumentsContract.getTreeDocumentId(uri)
        require(rootId.isNotBlank()) { "The selected folder has no document identity." }
        val uris = mutableListOf<Uri>()
        val budget = ImportTraversalBudget()
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")

        suspend fun walk(documentId: String, depth: Int) {
            if (budget.entryLimitReached) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            ) ?: throw java.io.IOException("Folder provider could not enumerate this authorized folder.")
            cursor.use { children ->
                val idColumn = children.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = children.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val typeColumn = children.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (children.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (!budget.visit(depth + 1)) {
                        if (budget.entryLimitReached) break
                        continue // A too-deep branch is skipped; permitted siblings can still be scanned.
                    }
                    if (budget.entriesVisited % 128 == 0) {
                        progress(ImportProgress(scannedEntries = budget.entriesVisited, message = "Scanning authorized folder…", truncated = budget.truncated))
                    }
                    val childId = children.getString(idColumn) ?: continue
                    val name = children.getString(nameColumn).orEmpty()
                    if (!includeHidden && name.startsWith('.')) continue
                    val mimeType = children.getString(typeColumn).orEmpty()
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walk(childId, depth + 1)
                    } else if (mimeType.startsWith("image/") || name.substringAfterLast('.', "").lowercase() in imageExtensions) {
                        uris += DocumentsContract.buildDocumentUriUsingTree(uri, childId)
                    }
                    if (budget.entryLimitReached) break
                }
            }
        }

        progress(ImportProgress(message = "Scanning authorized folder…"))
        walk(rootId, 0)
        val imported = importUris(uris, collection) { update ->
            progress(update.copy(scannedEntries = budget.entriesVisited, truncated = update.truncated || budget.truncated))
        }
        imported.copy(scannedEntries = budget.entriesVisited, truncated = imported.truncated || budget.truncated)
            .also(progress)
    }
    private suspend fun importOne(uri: Uri, collection: String): Boolean {
        val id = UUID.randomUUID().toString()
        val temp = LibraryFilePath.resolve(originals, "$id.partial")
        val target = LibraryFilePath.resolve(originals, "$id.img")
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
                if (!file(existing).isFile || existing.quarantined) {
                    val decoded = pipeline.decode(temp, 320, 320)
                    try { writeThumbnail(decoded, LibraryFilePath.resolve(thumbnails, "${existing.id}.jpg")) } finally { decoded.recycle() }
                    java.nio.file.Files.move(temp.toPath(), file(existing).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
                    db.library().updateAsset(existing.copy(quarantined = false, sourceUri = uri.toString()))
                }
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
                writeThumbnail(decoded, LibraryFilePath.resolve(thumbnails, "$id.jpg"))
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
            if (!committed) { target.delete(); LibraryFilePath.resolve(thumbnails, "$id.jpg").delete() }
        }
    }
    suspend fun validateAssets(): Int = withContext(Dispatchers.IO) {
        var missing = 0
        db.library().allAssets().forEach { asset ->
            val broken = if (!file(asset).isFile) true else try { pipeline.decode(file(asset), 160, 160).also { it.recycle() }; false } catch (_: Exception) { true }
            if (broken) missing++
            if (asset.quarantined != broken) db.library().updateAsset(asset.copy(quarantined = broken))
        }
        missing
    }
    suspend fun thumbnail(asset: AssetEntity): File = withContext(Dispatchers.IO) {
        val target = LibraryFilePath.resolve(thumbnails, "${asset.id}.jpg")
        val lock = thumbnailLocks[(asset.id.hashCode() and Int.MAX_VALUE) % thumbnailLocks.size]
        lock.withLock {
            if (!thumbnailIsReadable(target)) {
                target.delete()
                val original = file(asset)
                if (original.isFile) {
                    val bitmap = pipeline.decode(original, 320, 320)
                    try { writeThumbnail(bitmap, target) }
                    finally { bitmap.recycle() }
                }
            }
            if (target.isFile) target.setLastModified(System.currentTimeMillis())
            target
        }
    }
    private fun thumbnailIsReadable(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }
    private fun writeThumbnail(bitmap: android.graphics.Bitmap, file: File) {
        val ratio = 512f / maxOf(bitmap.width, bitmap.height)
        val small = if (ratio < 1f) android.graphics.Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true) else bitmap
        val temp = File(file.parentFile, "${file.name}.${UUID.randomUUID()}.partial")
        try {
            temp.outputStream().use { check(small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it)) }
            java.nio.file.Files.move(temp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temp.delete()
            if (small !== bitmap) small.recycle()
        }
    }
    suspend fun enforceCacheBudget(maximumMb: Int): Long = withContext(Dispatchers.IO) {
        val files = thumbnails.listFiles()?.filter { it.isFile } ?: emptyList()
        val evict = CachePolicy.evict(files.map { CacheEntry(it.name,it.length(),it.lastModified()) }, maximumMb.coerceIn(16,512)*1024L*1024)
        var freed=0L
        files.filter { it.name in evict }.forEach { f -> val size=f.length(); if(f.delete()) freed+=size }
        freed
    }
}

package com.westkitty.dexwallpaper.creative

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class CreativeRepository(private val context: Context, private val wall: WallRepository) {
    val generator = ProceduralArtEngine()
    val renderer = SceneRenderer(wall)
    suspend fun saveGenerated(composition: Composition, collection: String, width: Int, height: Int): String = withContext(Dispatchers.IO) {
        val bitmap=generator.render(composition,width,height)
        val id=saveBitmap(bitmap,collection,"${composition.pattern.title} / seed ${composition.seed}")
        wall.dao.asset(id)?.let { wall.dao.updateAsset(it.copy(attribution="Generated offline by DEX / ${composition.pattern.name} / seed ${composition.seed}",license="Original procedural composition; user-controlled")) }
        id
    }
    suspend fun saveScene(id: String?, scene: SceneManifest): String {
        val encoded=SceneCodec.encode(scene)
        val sceneId=id ?: UUID.randomUUID().toString()
        wall.db.withTransaction {
            val previous=wall.dao.scene(sceneId)
            val revision=(previous?.revision ?: 0)+1
            if(previous?.manifest != encoded) {
                wall.dao.putScene(SceneEntity(sceneId,scene.title,encoded,System.currentTimeMillis(),revision))
                wall.dao.addRevision(SceneRevisionEntity(UUID.randomUUID().toString(),sceneId,encoded,System.currentTimeMillis(),revision))
                wall.dao.trimRevisions(sceneId)
            }
        }
        return sceneId
    }
    suspend fun renderToLibrary(scene: SceneManifest, collection: String, elapsed: Double = 0.0): String = withContext(Dispatchers.IO) {
        val id=saveBitmap(renderer.render(scene,scene.width,scene.height,elapsed),collection,scene.title)
        wall.dao.asset(id)?.let { wall.dao.updateAsset(it.copy(attribution="User-authored DEX scene / ${scene.title} / schema ${scene.schemaVersion}",license="User-provided layers; retains original artwork rights")) }
        id
    }
    private suspend fun saveBitmap(bitmap: Bitmap, collection: String, title: String): String {
        val temp=File(context.cacheDir,"composition-${UUID.randomUUID()}.png")
        try {
            temp.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) { "Composition export failed" } }
            val digest=MessageDigest.getInstance("SHA-256")
            temp.inputStream().use { input -> val buffer=ByteArray(64*1024); while(true) { val n=input.read(buffer); if(n<0) break; digest.update(buffer,0,n) } }
            val hash=digest.digest().joinToString("") { "%02x".format(it) }
            val imported=wall.importer.importUris(listOf(Uri.fromFile(temp)),collection) {}
            check(imported.rejected==0) { "Generated composition could not be imported" }
            val asset=requireNotNull(wall.dao.byHash(hash))
            wall.dao.updateAsset(asset.copy(name=title.take(200),sourceUri="dex-generated:${asset.id}"))
            return asset.id
        } finally { bitmap.recycle(); temp.delete() }
    }
}

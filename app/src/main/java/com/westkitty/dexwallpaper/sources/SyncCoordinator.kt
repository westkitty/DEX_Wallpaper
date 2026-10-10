package com.westkitty.dexwallpaper.sources

import android.content.Context
import androidx.room.withTransaction
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.westkitty.dexwallpaper.core.*
import com.westkitty.dexwallpaper.data.*
import com.westkitty.dexwallpaper.security.CredentialVault
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Explicit trusted-source sync, not an always-on polling backend. Core never needs this module. */
class SyncCoordinator(private val context: Context,private val wall: WallRepository,private val vault: CredentialVault) {
    private val cache=File(context.cacheDir,"remote").apply { mkdirs() }
    private val syncMutex=kotlinx.coroutines.sync.Mutex()
    suspend fun sync(id: String,progress: (String)->Unit): String = withContext(Dispatchers.IO) {
        syncMutex.lock()
        try {
            val source=requireNotNull(wall.dao.source(id)) { "Remote source was removed" }
            require(source.enabled) { "Source is disabled" }
            val endpoint=when(val parsed=NetworkPolicy.endpoint(source.endpoint,source.allowPrivateNetwork)) { is Outcome.Error -> throw IllegalArgumentException(parsed.message);is Outcome.Ok -> parsed.value }
            val cm=context.getSystemService(ConnectivityManager::class.java)
            require(wall.dao.allCollections().any { it.id==source.collectionId }) { "Source collection no longer exists" }
            val selectedNetwork=cm.activeNetwork
            if(selectedNetwork==null || cm.getNetworkCapabilities(selectedNetwork)==null) return@withContext offline(source,progress)
            val budget=source.budgetMb.coerceIn(1,80)*1024L*1024
            fun allowed(used: Long,incoming: Long): Boolean {
                val current=cm.getNetworkCapabilities(selectedNetwork) ?: return false
                return cm.activeNetwork==selectedNetwork && NetworkPolicy.mayTransfer(source.wifiOnly,source.allowMetered,
                    current.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),cm.isActiveNetworkMetered,used,incoming,budget)
            }
            require(allowed(0,0)) { "Source network policy requires Wi-Fi/unmetered access" }
            val credentials=vault.get(id)
            val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).writeTimeout(15,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS)
                .socketFactory(selectedNetwork.socketFactory)
                .dns { host -> selectedNetwork.getAllByName(host).toList().also { addresses -> if(addresses.isEmpty() || addresses.any { !NetworkPolicy.addressAllowed(it,source.allowPrivateNetwork) }) throw java.net.UnknownHostException("Trusted endpoint resolves to a prohibited address") } }.build()
            var used=0L
            var lastRequestAt=0L
            var requestCount=0
            suspend fun fetch(uri: URI,method: String="GET",limit: Long=budget): File {
                require(NetworkPolicy.requestAllowed(endpoint,uri)) { "Request escaped source allowlist" }
                val file=File(cache,"${source.id}-${UUID.randomUUID()}.partial")
                try {
                    var response: Response?=null
                    var activeCall: Call?=null
                    for(attempt in 0..2) {
                        currentCoroutineContext().ensureActive()
                        require(allowed(used,0)) { "Selected network changed or no longer meets transfer policy" }
                        require(++requestCount<=303) { "Per-sync request count exhausted" }
                        val pacing=(250-(android.os.SystemClock.elapsedRealtime()-lastRequestAt)).coerceAtLeast(0)
                        if(pacing>0) delay(pacing)
                        lastRequestAt=android.os.SystemClock.elapsedRealtime()
                        val builder=Request.Builder().url(uri.toString())
                        credentials?.let { builder.header("Authorization",Credentials.basic(it.username,it.password)) }
                        if(method=="PROPFIND") builder.header("Depth","1").method(method,DAV_BODY.toRequestBody("text/xml; charset=utf-8".toMediaType()))
                        else builder.get()
                        progress("${source.name}: request ${attempt+1}/3")
                        activeCall=client.newCall(builder.build())
                        val result=execute(requireNotNull(activeCall))
                        if(result.code==429 || result.code in 500..599) {
                            val retryHeader=result.header("Retry-After");result.close()
                            val retryAfter=NetworkPolicy.retryAfterSeconds(retryHeader)
                            require(retryAfter==null || retryAfter<=60) { "Provider postpones beyond the bounded retry window; retry manually later" }
                            if(attempt==2) throw java.io.IOException("Provider retry budget exhausted")
                            delay(NetworkPolicy.retryDelayMillis(attempt,retryAfter));continue
                        }
                        if(!result.isSuccessful) { val code=result.code;result.close();throw java.io.IOException("Provider HTTP $code; redirects and untrusted endpoints are not followed") }
                        response=result;break
                    }
                    val readGuard=Job(currentCoroutineContext()[Job])
                    readGuard.invokeOnCompletion { cause -> if(cause!=null) activeCall?.cancel() }
                    try { requireNotNull(response).use { r ->
                        val body=requireNotNull(r.body) { "Provider response is empty" }
                        val length=body.contentLength()
                        require(allowed(used,0) && (length<0 || allowed(used,length) && length<=limit)) { "Provider response exceeds transfer budget" }
                        var bytes=0L
                        body.byteStream().use { input -> file.outputStream().use { output -> val buffer=ByteArray(64*1024);while(true) { currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0) break;bytes+=n
                            require(bytes<=limit && allowed(used,bytes)) { "Transfer budget exceeded" };output.write(buffer,0,n) } } }
                        currentCoroutineContext().ensureActive()
                        used+=bytes
                    } } finally { readGuard.complete() }
                    return file
                } catch(e: Exception) { file.delete();throw e }
            }
            var message: String
            try {
                message=when(source.kind) {
                    "WEBDAV","NEXTCLOUD" -> {
                        require(endpoint.uri.path.orEmpty().endsWith('/')) { "WebDAV collection URL must end with /" }
                        val known=wall.dao.allAssets().filter { !it.quarantined && wall.importer.file(it).isFile }.map { it.sourceUri }.toSet()
                        val listing=fetch(endpoint.uri,"PROPFIND",1024*1024)
                        val links=try { XmlFeeds.webDav(listing.readText()).filter { it.image && !it.directory }.map { endpoint.uri.resolve(it.href).normalize() }
                            .filter { NetworkPolicy.requestAllowed(endpoint,it) && it.toString() !in known }.distinct().take(100) } finally { listing.delete() }
                        var imported=0;var duplicates=0;var rejected=0
                        for((index,link) in links.withIndex()) {
                            progress("${source.name}: image ${index+1}/${links.size}")
                            val temp=fetch(link,limit=(budget-used).coerceAtLeast(0))
                            try { val result=importRemote(temp,source,link);imported+=result.imported;duplicates+=result.duplicates;rejected+=result.rejected } finally { temp.delete() }
                        }
                        "$imported new, $duplicates duplicates, $rejected rejected / ${links.size} bounded WebDAV entries"
                    }
                    "IMAGE" -> {
                        val temp=fetch(endpoint.uri)
                        try {
                            val result=importRemote(temp,source,endpoint.uri)
                            require(result.rejected==0) { "Remote file is corrupt or not a supported image" }
                            val target=File(cache,"${source.id}.image")
                            java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                            "Verified image imported; offline cache retained"
                        } finally { temp.delete() }
                    }
                    "RSS" -> {
                        val temp=fetch(endpoint.uri,limit=1024*1024)
                        val items=try { XmlFeeds.rss(temp.readText()) } finally { temp.delete() }
                        val data=JSONObject().put("items",org.json.JSONArray().also { a -> items.forEach { a.put(JSONObject().put("title",it.title).put("link",it.link).put("date",it.date)) } })
                        writeData(source.id,data.toString())
                        "${items.size} RSS/Atom items cached locally"
                    }
                    "JSON" -> {
                        val temp=fetch(endpoint.uri,limit=64*1024)
                        val fields=try { when(val parsed=NumericDataCodec.decode(temp.readText())) { is Outcome.Ok -> parsed.value;is Outcome.Error -> throw IllegalArgumentException(parsed.message) } } finally { temp.delete() }
                        writeData(source.id,JSONObject(fields).toString())
                        "${fields.size} numeric data fields cached; bind explicitly in Scene composer"
                    }
                    else -> throw IllegalArgumentException("Unsupported source type")
                }
                wall.dao.putSource(source.copy(lastSuccess=System.currentTimeMillis(),status=message))
                pruneBudget(wall.preferences.flow.first().cacheMb)
            } finally { client.connectionPool.evictAll();client.dispatcher.executorService.shutdown() }
            message
        } catch(e: Exception) {
            if(e is CancellationException) throw e
            wall.dao.source(id)?.let { source -> wall.dao.putSource(source.copy(status="Sync failed; partial validated imports kept. Review endpoint, network budget, or encrypted credentials")) }
            throw e
        } finally { syncMutex.unlock() }
    }
    private suspend fun importRemote(file: File,source: RemoteSourceEntity,uri: URI): ImportProgress {
        val digest=java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer=ByteArray(64*1024);while(true) { val n=input.read(buffer);if(n<0) break;digest.update(buffer,0,n) } }
        val hash=digest.digest().joinToString("") { "%02x".format(it) }
        val previous=wall.dao.byHash(hash)
        val result=wall.importer.importUris(listOf(Uri.fromFile(file)),source.collectionId) {}
        if(previous!=null && previous.quarantined) wall.dao.byHash(hash)?.let { a -> wall.dao.updateAsset(a.copy(sourceUri=previous.sourceUri)) }
        if(previous==null) wall.dao.byHash(hash)?.let { a -> wall.dao.updateAsset(a.copy(name=uri.path.substringAfterLast('/').ifBlank { source.name }.take(200),sourceUri=uri.toString(),
            attribution="Imported from trusted source: ${source.name}",license="Remote license unspecified; owner must supply rights/attribution")) }
        return result
    }
    private suspend fun offline(source: RemoteSourceEntity,progress: (String)->Unit): String {
        progress("Offline: inspecting verified local cache")
        val image=File(cache,"${source.id}.image")
        if(source.kind=="IMAGE" && image.isFile) { val result=wall.importer.importUris(listOf(Uri.fromFile(image)),source.collectionId) {};require(result.rejected==0) { "Cached image no longer validates; synchronize again" };return "Offline cached image reused; no network request" }
        if(source.kind in setOf("RSS","JSON") && File(cache,"${source.id}.json").isFile) {
            val data=readData(source.id)
            if(source.kind=="JSON") { require(NumericDataCodec.decode(data) is Outcome.Ok) { "Cached numeric data is invalid" } }
            else { require(JSONObject(data).has("items")) { "Cached feed is invalid" } }
            return "Offline data cache available; no network request"
        }
        throw java.io.IOException("Offline and no verified cache exists for this source")
    }
    suspend fun readData(id: String): String = withContext(Dispatchers.IO) {
        val file=File(cache,"$id.json");require(id.matches(Regex("[A-Za-z0-9_-]{1,80}")) && file.isFile && file.length()<=2*1024*1024) { "No cached data for this source" };file.setLastModified(System.currentTimeMillis());file.readText()
    }
    suspend fun bindVariables(sourceId: String,sceneId: String): Int {
        val source=requireNotNull(wall.dao.source(sourceId));require(source.kind=="JSON") { "Only numeric JSON sources bind to safe formulas" }
        val fields=when(val parsed=NumericDataCodec.decode(readData(sourceId))) { is Outcome.Ok -> parsed.value;is Outcome.Error -> throw IllegalArgumentException(parsed.message) }
        val entity=requireNotNull(wall.dao.scene(sceneId));val scene=(SceneCodec.decode(entity.manifest) as? Outcome.Ok)?.value ?: throw IllegalArgumentException("Invalid scene")
        val encoded=SceneCodec.encode(scene.copy(variables=scene.variables+fields))
        require(SceneCodec.decode(encoded) is Outcome.Ok) { "Merged variables exceed the scene's bounded schema" }
        wall.db.withTransaction {
            wall.dao.putScene(entity.copy(manifest=encoded,revision=entity.revision+1,updatedAt=System.currentTimeMillis()))
            wall.dao.addRevision(SceneRevisionEntity(UUID.randomUUID().toString(),sceneId,encoded,System.currentTimeMillis(),entity.revision+1));wall.dao.trimRevisions(sceneId)
        }
        return fields.size
    }
    private fun writeData(id: String,content: String) {
        val atomic=android.util.AtomicFile(File(cache,"$id.json"))
        val stream=atomic.startWrite()
        try { stream.write(content.toByteArray());atomic.finishWrite(stream) }
        catch(e: Exception) { atomic.failWrite(stream);throw e }
    }
    suspend fun removeSource(id: String) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            vault.remove(id)
            cache.listFiles()?.filter { it.name=="$id.image" || it.name=="$id.json" || it.name=="$id.json.bak" || it.name.startsWith("$id-") }?.forEach { it.delete() }
            wall.dao.deleteSource(id)
        }
    }
    suspend fun enforceCacheBudget(mb: Int) = withContext(Dispatchers.IO) { syncMutex.withLock { pruneBudget(mb) } }
    private fun pruneBudget(mb: Int) {
        // Only process-death leftovers age beyond a full day; active transfer stages remain protected.
        cache.listFiles()?.filter { it.extension=="partial" && System.currentTimeMillis()-it.lastModified()>86_400_000L }?.forEach { it.delete() }
        val files=(cache.listFiles()?.toList() ?: emptyList())+(wall.importer.thumbnails.listFiles()?.toList() ?: emptyList())
        val entries=files.filter { it.isFile }.map { CacheEntry(it.path,it.length(),it.lastModified(),it.extension in setOf("partial","new","bak")) }
        val evictions=CachePolicy.evict(entries,mb.coerceIn(16,512)*1024L*1024)
        files.filter { it.path in evictions }.forEach { it.delete() }
    }
    private suspend fun execute(call: Call): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object: Callback {
            override fun onFailure(call: Call,e: java.io.IOException) { if(continuation.isActive) continuation.resumeWith(Result.failure(e)) }
            override fun onResponse(call: Call,response: Response) {
                if(continuation.isActive) continuation.resume(response,onCancellation={ _,value,_ -> value.close() }) else response.close()
            }
        })
    }
    companion object {
        private const val DAV_BODY="""<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:getcontenttype/><d:resourcetype/><d:getcontentlength/></d:prop></d:propfind>"""
    }
}

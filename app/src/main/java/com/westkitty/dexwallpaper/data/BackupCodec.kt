package com.westkitty.dexwallpaper.data

import com.westkitty.dexwallpaper.core.SceneCodec
import com.westkitty.dexwallpaper.core.Outcome
import org.json.JSONArray
import org.json.JSONObject

/** Explicit, versioned portable metadata, separate from DB internals and device-specific URI grants. */
data class BackupSnapshot(val assets: List<AssetEntity>, val collections: List<CollectionEntity>, val memberships: List<Membership>,
    val edits: List<EditEntity>, val screens: List<ScreenEntity>, val rules: List<RuleEntity>, val scenes: List<SceneEntity>,
    val preferences: WorkspacePreferences)
object BackupCodec {
    const val MAX_MANIFEST_BYTES=8*1024*1024
    fun encode(snapshot: BackupSnapshot): String {
        val root=JSONObject().put("format","dex-wallpaper-backup").put("version",1)
        fun <T> array(items: List<T>, encode: (T)->JSONObject) = JSONArray().also { a -> items.forEach { a.put(encode(it)) } }
        root.put("assets",array(snapshot.assets) { a -> JSONObject().put("id",a.id).put("name",a.name).put("sha256",a.sha256)
            .put("width",a.width).put("height",a.height).put("bytes",a.bytes).put("importedAt",a.importedAt).put("favorite",a.favorite)
            .put("excluded",a.excluded).put("quarantined",a.quarantined).put("tags",a.tags).put("perceptualHash",a.perceptualHash.toString()).put("attribution",a.attribution).put("license",a.license) })
        root.put("collections",array(snapshot.collections) { c -> JSONObject().put("id",c.id).put("name",c.name).put("pinned",c.pinned) })
        root.put("memberships",array(snapshot.memberships) { m -> JSONObject().put("collectionId",m.collectionId).put("assetId",m.assetId) })
        root.put("edits",array(snapshot.edits) { e -> JSONObject().put("assetId",e.assetId).put("scale",e.scale).put("zoom",e.zoom)
            .put("focalX",e.focalX).put("focalY",e.focalY).put("portraitX",e.portraitX).put("portraitY",e.portraitY)
            .put("brightness",e.brightness).put("contrast",e.contrast).put("saturation",e.saturation).put("dim",e.dim).put("vignette",e.vignette)
            .put("blur",e.blur).put("safeTop",e.safeTop).put("safeBottom",e.safeBottom) })
        root.put("screens",array(snapshot.screens) { s -> JSONObject().put("destination",s.destination).put("collectionId",s.collectionId).put("mode",s.mode)
            .put("intervalMinutes",s.intervalMinutes).put("favoritesOnly",s.favoritesOnly).put("minimumSeparation",s.minimumSeparation)
            .put("currentAssetId",s.currentAssetId ?: "").put("pending",s.pending).put("recent",s.recent).put("cycle",s.cycle) })
        root.put("rules",array(snapshot.rules) { r -> JSONObject().put("id",r.id).put("name",r.name).put("collectionId",r.collectionId).put("priority",r.priority)
            .put("enabled",r.enabled).put("startMinute",r.startMinute).put("endMinute",r.endMinute).put("days",r.days).put("charging",r.charging)
            .put("darkTheme",r.darkTheme).put("minimumBattery",r.minimumBattery).put("maximumLux",r.maximumLux).put("destination",r.destination) })
        root.put("scenes",array(snapshot.scenes) { s -> JSONObject().put("id",s.id).put("name",s.name).put("manifest",s.manifest).put("updatedAt",s.updatedAt).put("revision",s.revision) })
        val p=snapshot.preferences
        root.put("preferences",JSONObject().put("reducedMotion",p.reducedMotion).put("highContrast",p.highContrast).put("compact",p.compact)
            .put("safeMode",p.safeMode).put("frameRate",p.frameRate).put("cacheMb",p.cacheMb).put("dynamicColors",p.dynamicColors).put("clockOverlay",p.clockOverlay)
            .put("batteryOverlay",p.batteryOverlay).put("parallax",p.parallax))
        return root.toString().also { require(it.toByteArray().size <= MAX_MANIFEST_BYTES) { "Backup metadata exceeds 8MB" } }
    }
    fun decode(input: String): BackupSnapshot {
        require(input.toByteArray().size <= MAX_MANIFEST_BYTES) { "Backup metadata exceeds 8MB" }
        // Bound JSON nesting before handing untrusted input to org.json.
        var depth=0; var quoted=false; var escaped=false
        input.forEach { c -> if(quoted) { if(escaped) escaped=false else if(c=='\\') escaped=true else if(c=='"') quoted=false }
            else if(c=='"') quoted=true else if(c=='{' || c=='[') { require(++depth<=16) { "Backup nesting too deep" } } else if(c=='}' || c==']') depth--
            require(depth>=0) { "Invalid backup structure" } }
        require(depth==0 && !quoted) { "Incomplete backup" }
        val root=JSONObject(input)
        require(root.getString("format")=="dex-wallpaper-backup" && root.getInt("version")==1) { "Unsupported backup format/version" }
        fun <T> rows(name: String, max: Int=5000, decode: (JSONObject)->T): List<T> {
            val a=root.optJSONArray(name) ?: JSONArray(); require(a.length()<=max) { "Too many $name" }
            return (0 until a.length()).map { decode(a.getJSONObject(it)) }
        }
        fun id(value: String): String { require(value.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid portable identity" }; return value }
        val assets=rows("assets") { a ->
            val aid=id(a.getString("id")); val hash=a.getString("sha256"); require(hash.matches(Regex("[0-9a-f]{64}"))) { "Invalid asset hash" }
            val w=a.getInt("width"); val h=a.getInt("height"); val size=a.getLong("bytes")
            require(w in 1..200000 && h in 1..200000 && size in 1..80L*1024*1024) { "Invalid asset bounds" }
            AssetEntity(aid,a.getString("name").take(200),"$aid.img","",hash,w,h,size,a.optLong("importedAt",0),a.optBoolean("favorite"),a.optBoolean("excluded"),
                true,a.optString("tags").take(400),a.optString("perceptualHash","0").toLong(),a.optString("attribution").take(1000),a.optString("license").take(1000))
        }
        require(assets.map { it.id }.distinct().size==assets.size && assets.map { it.sha256 }.distinct().size==assets.size) { "Duplicate asset identities/hashes" }
        val collections=rows("collections",1000) { c -> CollectionEntity(id(c.getString("id")),c.getString("name").take(100),pinned=c.optBoolean("pinned")) }
        val assetIds=assets.map { it.id }.toSet(); val collectionIds=collections.map { it.id }.toSet()
        require(collections.size==collectionIds.size) { "Duplicate collection identities" }
        val memberships=rows("memberships",50000) { m -> Membership(id(m.getString("collectionId")),id(m.getString("assetId"))).also { require(it.collectionId in collectionIds && it.assetId in assetIds) { "Dangling membership" } } }
        val edits=rows("edits") { e ->
            val aid=id(e.getString("assetId")); require(aid in assetIds) { "Dangling edit" }
            fun f(key: String, default: Double)=e.optDouble(key,default).also { require(it.isFinite()) { "Nonfinite edit" } }.toFloat()
            EditEntity.from(aid,com.westkitty.dexwallpaper.core.ImageEdit(scale=com.westkitty.dexwallpaper.core.ScaleMode.valueOf(e.optString("scale","FILL")),
                zoom=f("zoom",1.0),focalX=f("focalX",.5),focalY=f("focalY",.5),portraitX=f("portraitX",.5),portraitY=f("portraitY",.5),brightness=f("brightness",0.0),
                contrast=f("contrast",1.0),saturation=f("saturation",1.0),dim=f("dim",0.0),vignette=f("vignette",0.0),blur=f("blur",0.0),safeTop=f("safeTop",0.0),safeBottom=f("safeBottom",0.0)))
        }
        val screens=rows("screens",3) { s ->
            val dest=s.getString("destination"); require(dest in setOf("HOME","LOCK","BOTH")) { "Unknown destination" }
            val cid=id(s.getString("collectionId")); require(cid in collectionIds) { "Dangling screen collection" }
            val mode=s.getString("mode"); require(mode in setOf("SHUFFLE","SEQUENTIAL","RANDOM")) { "Invalid rotation mode" }
            fun queue(key: String)=s.optString(key).split('|').filter { it in assetIds }.distinct().take(5000).joinToString("|")
            ScreenEntity(dest,cid,mode,s.optLong("intervalMinutes",60).coerceIn(15,10080),enabled=false,paused=true,favoritesOnly=s.optBoolean("favoritesOnly"),
                minimumSeparation=s.optInt("minimumSeparation",1).coerceIn(0,100),currentAssetId=s.optString("currentAssetId").takeIf { it in assetIds },
                pending=queue("pending"),recent=queue("recent").split('|').takeLast(100).joinToString("|"),cycle=s.optLong("cycle",0).coerceAtLeast(0),error="Restored configuration; review destination and re-enable rotation explicitly.")
        }
        require(screens.map { it.destination }.distinct().size==screens.size) { "Duplicate destination records" }
        val rules=rows("rules",500) { r ->
            val cid=id(r.getString("collectionId")); require(cid in collectionIds) { "Dangling rule collection" }
            val destination=r.optString("destination","HOME"); require(destination in setOf("HOME","LOCK")) { "Invalid automatic destination" }
            RuleEntity(id(r.getString("id")),r.getString("name").take(120),cid,r.getInt("priority"),enabled=false,
                startMinute=r.optInt("startMinute",0).coerceIn(0,1439),endMinute=r.optInt("endMinute",1439).coerceIn(0,1439),days=r.optString("days","1,2,3,4,5,6,7"),
                charging=r.optInt("charging",-1).coerceIn(-1,1),darkTheme=r.optInt("darkTheme",-1).coerceIn(-1,1),minimumBattery=r.optInt("minimumBattery",0).coerceIn(0,100),
                maximumLux=if(r.isNull("maximumLux")) null else r.getDouble("maximumLux").also { require(it.isFinite() && it>=0) }.toFloat(),destination=destination)
        }
        val scenes=rows("scenes",500) { s ->
            val manifest=s.getString("manifest"); require(SceneCodec.decode(manifest) is Outcome.Ok) { "Invalid scene in backup" }
            SceneEntity(id(s.getString("id")),s.getString("name").take(120),manifest,s.optLong("updatedAt",0),s.optInt("revision",1).coerceAtLeast(1))
        }
        val p=root.optJSONObject("preferences") ?: JSONObject()
        val prefs=WorkspacePreferences(p.optBoolean("reducedMotion"),p.optBoolean("highContrast"),p.optBoolean("compact"),p.optBoolean("safeMode"),
            p.optInt("frameRate",30).coerceIn(1,60),p.optInt("cacheMb",128).coerceIn(16,512),p.optBoolean("dynamicColors"),p.optBoolean("clockOverlay"),p.optBoolean("batteryOverlay"),p.optBoolean("parallax"))
        return BackupSnapshot(assets,collections,memberships,edits,screens,rules,scenes,prefs)
    }
}

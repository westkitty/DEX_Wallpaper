package com.westkitty.dexwallpaper.core

import org.json.JSONArray
import org.json.JSONObject

enum class LayerKind { RECTANGLE, CIRCLE, TEXT, IMAGE, CLOCK, BATTERY }
data class SceneLayer(val id: String, val kind: LayerKind, val x: Float = .1f, val y: Float = .1f,
    val width: Float = .3f, val height: Float = .1f, val color: String = "#FFFFFF", val text: String = "",
    val assetId: String = "", val opacity: Float = 1f, val rotation: Float = 0f, val visibleWhen: String = "1",
    val xFormula: String = "", val font: String = "sans-serif", val blend: String = "NORMAL")
data class SceneManifest(val schemaVersion: Int = 1, val title: String = "Untitled scene", val width: Int = 2960,
    val height: Int = 1848, val background: String = "#101014", val layers: List<SceneLayer> = emptyList(),
    val variables: Map<String,Double> = emptyMap(), val durationSeconds: Int = 10, val animate: Boolean = false,
    val epoch: String = "", val sequenceIndex: Int = 0)

/** Portable declarative format. File paths, URIs, executable scripts, and binary plugins are not accepted. */
object SceneCodec {
    const val MAX_BYTES = 256 * 1024
    fun encode(scene: SceneManifest): String {
        validate(scene)
        val json = JSONObject().put("schemaVersion",scene.schemaVersion).put("title",scene.title).put("width",scene.width).put("height",scene.height)
            .put("background",scene.background).put("durationSeconds",scene.durationSeconds).put("animate",scene.animate).put("epoch",scene.epoch).put("sequenceIndex",scene.sequenceIndex)
        json.put("variables", JSONObject(scene.variables))
        json.put("layers",JSONArray().also { array -> scene.layers.forEach { l -> array.put(JSONObject().put("id",l.id).put("kind",l.kind.name).put("x",l.x).put("y",l.y)
            .put("width",l.width).put("height",l.height).put("color",l.color).put("text",l.text).put("assetId",l.assetId).put("opacity",l.opacity)
            .put("rotation",l.rotation).put("visibleWhen",l.visibleWhen).put("xFormula",l.xFormula).put("font",l.font).put("blend",l.blend)) } })
        return json.toString(2).also { require(it.toByteArray().size <= MAX_BYTES) { "Scene manifest too large" } }
    }
    fun decode(input: String): Outcome<SceneManifest> = try {
        require(input.toByteArray().size <= MAX_BYTES) { "Manifest exceeds 256 KB" }
        // A nesting preflight prevents stack exhaustion in the platform JSON parser.
        var depth = 0; var quoted = false; var escaped = false
        input.forEach { c ->
            if (quoted) { if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false }
            else if (c == '"') quoted = true else if (c == '{' || c == '[') { require(++depth <= 16) { "Manifest nesting too deep" } } else if (c == '}' || c == ']') depth--
            require(depth >= 0) { "Invalid manifest structure" }
        }
        require(depth == 0 && !quoted) { "Incomplete manifest" }
        val json = JSONObject(input)
        require(json.getInt("schemaVersion") == 1) { "Unsupported scene schema version" }
        val variables = json.optJSONObject("variables") ?: JSONObject()
        require(variables.length() <= 64) { "Too many scene variables" }
        val vars = variables.keys().asSequence().associateWith { variables.getDouble(it) }
        val array = json.optJSONArray("layers") ?: JSONArray()
        require(array.length() <= 64) { "At most 64 layers" }
        val layers = (0 until array.length()).map { n -> array.getJSONObject(n).let { l ->
            SceneLayer(l.getString("id"),LayerKind.valueOf(l.getString("kind")), l.optDouble("x",.1).toFloat(),l.optDouble("y",.1).toFloat(),
                l.optDouble("width",.3).toFloat(),l.optDouble("height",.1).toFloat(),l.optString("color","#FFFFFF"),l.optString("text",""),l.optString("assetId",""),
                l.optDouble("opacity",1.0).toFloat(),l.optDouble("rotation",0.0).toFloat(),l.optString("visibleWhen","1"),l.optString("xFormula",""),l.optString("font","sans-serif"),l.optString("blend","NORMAL"))
        } }
        val scene = SceneManifest(1,json.optString("title","Untitled scene"),json.optInt("width",2960),json.optInt("height",1848),json.optString("background","#101014"),
            layers,vars,json.optInt("durationSeconds",10),json.optBoolean("animate",false),json.optString("epoch",""),json.optInt("sequenceIndex",0))
        validate(scene)
        Outcome.Ok(scene)
    } catch (e: Exception) { Outcome.Error(e.message?.take(200) ?: "Invalid scene manifest") }

    fun validate(scene: SceneManifest) {
        require(scene.schemaVersion == 1 && scene.width in 64..8192 && scene.height in 64..8192 && scene.width.toLong()*scene.height <= 16_000_000) { "Invalid schema or scene geometry (16MP maximum)" }
        require(scene.title.length in 1..120 && scene.epoch.length <= 120 && scene.sequenceIndex in 0..10000) { "Invalid scene metadata" }
        require(scene.background.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) { "Invalid background color" }
        require(scene.layers.size <= 64 && scene.layers.map { it.id }.distinct().size == scene.layers.size) { "Too many or duplicate layers" }
        require(scene.variables.size <= 64 && scene.variables.all { (k,v) -> k.matches(Regex("[A-Za-z_][A-Za-z0-9_]{0,40}")) && v.isFinite() && kotlin.math.abs(v) <= 1e9 }) { "Invalid persistent variables" }
        require(scene.durationSeconds in 1..3600) { "Invalid animation duration" }
        val vars = scene.variables + mapOf("time" to 0.0,"hour" to 12.0,"battery" to 100.0,"charging" to 0.0)
        scene.layers.forEach { l ->
            require(l.id.matches(Regex("[A-Za-z0-9_-]{1,80}")) && l.text.length <= 1000 && l.assetId.length <= 80) { "Invalid layer identity or text" }
            require(listOf(l.x,l.y,l.width,l.height,l.opacity,l.rotation).all { it.isFinite() }) { "Nonfinite layer geometry" }
            require(l.x in -1f..2f && l.y in -1f..2f && l.width in 0.001f..2f && l.height in 0.001f..2f && l.opacity in 0f..1f && l.rotation in -360f..360f) { "Layer geometry out of bounds" }
            require(l.color.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) && l.font in setOf("sans-serif","serif","monospace") && l.blend in setOf("NORMAL","MULTIPLY","SCREEN","ADD")) { "Invalid style" }
            require(l.assetId.isEmpty() || l.assetId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Asset IDs only; external paths are forbidden" }
            require(SafeFormula.evaluate(l.visibleWhen,vars) is Outcome.Ok) { "Invalid layer condition" }
            if (l.xFormula.isNotBlank()) require(SafeFormula.evaluate(l.xFormula,vars) is Outcome.Ok) { "Invalid position formula" }
        }
    }
}

package com.westkitty.dexwallpaper.platform

import android.content.Context
import android.net.Uri
import android.os.Build
import com.westkitty.dexwallpaper.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

/** Sanitized by construction: counts/policies only, no image/name/path/URI/location/credential fields. */
class DiagnosticsRepository(private val context: Context, private val graph: AppGraph) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val assets=graph.repository.dao.allAssets()
        val report=JSONObject().put("format","dex-sanitized-diagnostics").put("version",1).put("api",Build.VERSION.SDK_INT)
            .put("assets",assets.size).put("missingOrQuarantined",assets.count { it.quarantined }).put("originalBytes",assets.sumOf { it.bytes })
            .put("collections",graph.repository.dao.allCollections().size).put("rules",graph.repository.dao.allRules().size)
        val metrics=com.westkitty.dexwallpaper.wallpaper.RenderTelemetry.snapshot()
        report.put("renderedFrames",metrics.totalFrames).put("activeEngines",metrics.engines).put("eglContexts",metrics.contexts)
            .put("textures",metrics.textures).put("meanFrameMs",metrics.meanFrameMs).put("contextRecoveryAttempts",metrics.contextRecoveries)
        val capabilities=CompatibilityRegistry(context).inspect()
        report.put("capabilities",JSONArray().also { array -> capabilities.forEach { array.put(JSONObject().put("capability",it.name).put("state",it.state.name)) } })
        context.contentResolver.openOutputStream(uri,"wt").use { requireNotNull(it) { "Diagnostic destination cannot be written" }.write(report.toString(2).toByteArray()) }
    }
}

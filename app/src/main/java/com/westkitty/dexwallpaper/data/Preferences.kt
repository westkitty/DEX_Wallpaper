package com.westkitty.dexwallpaper.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dexPreferences by preferencesDataStore(name = "workspace")
data class WorkspacePreferences(val reducedMotion: Boolean = false, val highContrast: Boolean = false, val compact: Boolean = false,
    val safeMode: Boolean = false, val frameRate: Int = 30, val cacheMb: Int = 128, val dynamicColors: Boolean = false,
    val clockOverlay: Boolean = false, val batteryOverlay: Boolean = false, val parallax: Boolean = false, val externalCommands: Boolean = false,
    val shaderEffect: String = "NONE", val effectIntensity: Float = .65f, val transition: String = "CROSSFADE", val transitionMs: Int = 700,
    val gyroParallax: Boolean = false, val zoomAnimation: Boolean = false, val externalLockCommands: Boolean = false, val externalBothCommands: Boolean = false,
    val liveHome: String = "", val liveLock: String = "", val liveShared: String = "", val livePreview: String = "", val livePending: String = "", val revision: Long = 0, val liveBeforeHome: Int = 0, val liveBeforeLock: Int = 0)
class Preferences(private val context: Context) {
    val flow = context.dexPreferences.data.map { p -> WorkspacePreferences(
        p[booleanPreferencesKey("reducedMotion")] ?: false, p[booleanPreferencesKey("highContrast")] ?: false,
        p[booleanPreferencesKey("compact")] ?: false, p[booleanPreferencesKey("safeMode")] ?: false,
        p[intPreferencesKey("frameRate")] ?: 30, p[intPreferencesKey("cacheMb")] ?: 128,
        p[booleanPreferencesKey("dynamicColors")] ?: false, p[booleanPreferencesKey("clockOverlay")] ?: false,
        p[booleanPreferencesKey("batteryOverlay")] ?: false, p[booleanPreferencesKey("parallax")] ?: false, p[booleanPreferencesKey("externalCommands")] ?: false,
        p[stringPreferencesKey("shaderEffect")] ?: "NONE", (p[floatPreferencesKey("effectIntensity")] ?: .65f).coerceIn(0f,1f),
        p[stringPreferencesKey("transition")] ?: "CROSSFADE", (p[intPreferencesKey("transitionMs")] ?: 700).coerceIn(100,5000),
        p[booleanPreferencesKey("gyroParallax")] ?: false, p[booleanPreferencesKey("zoomAnimation")] ?: false, p[booleanPreferencesKey("externalLockCommands")] ?: false, p[booleanPreferencesKey("externalBothCommands")] ?: false,
        p[stringPreferencesKey("liveHome")] ?: "",p[stringPreferencesKey("liveLock")] ?: "",p[stringPreferencesKey("liveShared")] ?: "",
        p[stringPreferencesKey("livePreview")] ?: "",p[stringPreferencesKey("livePending")] ?: "",p[longPreferencesKey("revision")] ?: 0,p[intPreferencesKey("liveBeforeHome")] ?: 0,p[intPreferencesKey("liveBeforeLock")] ?: 0) }
    private fun bump(p: MutablePreferences) { p[longPreferencesKey("revision")]=(p[longPreferencesKey("revision")] ?: 0)+1 }
    suspend fun flag(key: String, value: Boolean) { context.dexPreferences.edit { it[booleanPreferencesKey(key)] = value;bump(it) } }
    suspend fun commitLive(target: com.westkitty.dexwallpaper.core.Destination,encoded: String) {
        require(com.westkitty.dexwallpaper.core.LiveConfigCodec.decode(encoded) is com.westkitty.dexwallpaper.core.Outcome.Ok)
        context.dexPreferences.edit { p ->
            bump(p)
            when(target) {
                com.westkitty.dexwallpaper.core.Destination.HOME -> p[stringPreferencesKey("liveHome")]=encoded
                com.westkitty.dexwallpaper.core.Destination.LOCK -> p[stringPreferencesKey("liveLock")]=encoded
                com.westkitty.dexwallpaper.core.Destination.BOTH -> { p[stringPreferencesKey("liveHome")]=encoded;p[stringPreferencesKey("liveLock")]=encoded;p[stringPreferencesKey("liveShared")]=encoded }
            }
        }
    }
    suspend fun preserveLiveScope(target: com.westkitty.dexwallpaper.core.Destination,shared: String) {
        require(target!=com.westkitty.dexwallpaper.core.Destination.BOTH)
        context.dexPreferences.edit { p ->
            // A later explicit Both commit wins over a stale ownership callback.
            if(p[stringPreferencesKey("liveShared")]==shared) {
                p[stringPreferencesKey(if(target==com.westkitty.dexwallpaper.core.Destination.HOME) "liveHome" else "liveLock")]=shared
                bump(p)
            }
        }
    }
    suspend fun prepareLivePreview(target: com.westkitty.dexwallpaper.core.Destination,encoded: String,before: com.westkitty.dexwallpaper.core.WallpaperSnapshot) {
        require(com.westkitty.dexwallpaper.core.LiveConfigCodec.decode(encoded) is com.westkitty.dexwallpaper.core.Outcome.Ok)
        context.dexPreferences.edit { it[stringPreferencesKey("livePreview")]=encoded;it[stringPreferencesKey("livePending")]=target.name;it[intPreferencesKey("liveBeforeHome")]=before.homeId;it[intPreferencesKey("liveBeforeLock")]=before.lockId;bump(it) }
    }
    suspend fun text(key: String, value: String) { context.dexPreferences.edit { it[stringPreferencesKey(key)] = value;bump(it) } }
    suspend fun decimal(key: String, value: Float) { require(value.isFinite()); context.dexPreferences.edit { it[floatPreferencesKey(key)] = value;bump(it) } }
    suspend fun number(key: String, value: Int) { context.dexPreferences.edit { it[intPreferencesKey(key)] = value;bump(it) } }
}

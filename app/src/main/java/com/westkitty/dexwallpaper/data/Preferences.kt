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
    val gyroParallax: Boolean = false, val zoomAnimation: Boolean = false, val externalLockCommands: Boolean = false, val externalBothCommands: Boolean = false)
class Preferences(private val context: Context) {
    val flow = context.dexPreferences.data.map { p -> WorkspacePreferences(
        p[booleanPreferencesKey("reducedMotion")] ?: false, p[booleanPreferencesKey("highContrast")] ?: false,
        p[booleanPreferencesKey("compact")] ?: false, p[booleanPreferencesKey("safeMode")] ?: false,
        p[intPreferencesKey("frameRate")] ?: 30, p[intPreferencesKey("cacheMb")] ?: 128,
        p[booleanPreferencesKey("dynamicColors")] ?: false, p[booleanPreferencesKey("clockOverlay")] ?: false,
        p[booleanPreferencesKey("batteryOverlay")] ?: false, p[booleanPreferencesKey("parallax")] ?: false, p[booleanPreferencesKey("externalCommands")] ?: false,
        p[stringPreferencesKey("shaderEffect")] ?: "NONE", (p[floatPreferencesKey("effectIntensity")] ?: .65f).coerceIn(0f,1f),
        p[stringPreferencesKey("transition")] ?: "CROSSFADE", (p[intPreferencesKey("transitionMs")] ?: 700).coerceIn(100,5000),
        p[booleanPreferencesKey("gyroParallax")] ?: false, p[booleanPreferencesKey("zoomAnimation")] ?: false, p[booleanPreferencesKey("externalLockCommands")] ?: false, p[booleanPreferencesKey("externalBothCommands")] ?: false) }
    suspend fun flag(key: String, value: Boolean) { context.dexPreferences.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun text(key: String, value: String) { context.dexPreferences.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun decimal(key: String, value: Float) { require(value.isFinite()); context.dexPreferences.edit { it[floatPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Int) { context.dexPreferences.edit { it[intPreferencesKey(key)] = value } }
}

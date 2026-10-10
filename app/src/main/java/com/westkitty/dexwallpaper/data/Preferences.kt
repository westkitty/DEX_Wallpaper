package com.westkitty.dexwallpaper.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dexPreferences by preferencesDataStore(name = "workspace")
data class WorkspacePreferences(val reducedMotion: Boolean = false, val highContrast: Boolean = false, val compact: Boolean = false,
    val safeMode: Boolean = false, val frameRate: Int = 30, val cacheMb: Int = 128, val dynamicColors: Boolean = false,
    val clockOverlay: Boolean = false, val batteryOverlay: Boolean = false, val parallax: Boolean = false)
class Preferences(private val context: Context) {
    val flow = context.dexPreferences.data.map { p -> WorkspacePreferences(
        p[booleanPreferencesKey("reducedMotion")] ?: false, p[booleanPreferencesKey("highContrast")] ?: false,
        p[booleanPreferencesKey("compact")] ?: false, p[booleanPreferencesKey("safeMode")] ?: false,
        p[intPreferencesKey("frameRate")] ?: 30, p[intPreferencesKey("cacheMb")] ?: 128,
        p[booleanPreferencesKey("dynamicColors")] ?: false, p[booleanPreferencesKey("clockOverlay")] ?: false,
        p[booleanPreferencesKey("batteryOverlay")] ?: false, p[booleanPreferencesKey("parallax")] ?: false) }
    suspend fun flag(key: String, value: Boolean) { context.dexPreferences.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Int) { context.dexPreferences.edit { it[intPreferencesKey(key)] = value } }
}

package com.westkitty.dexwallpaper.ui

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.westkitty.dexwallpaper.data.WorkspacePreferences

val DexRed = Color(0xFFFF4655)
private val DexColors = darkColorScheme(primary = DexRed, onPrimary = Color(0xFF180004), secondary = Color(0xFFE8E8EC),
    background = Color(0xFF0D0D10), surface = Color(0xFF141418), surfaceContainer = Color(0xFF1C1C22),
    onSurface = Color(0xFFF4F4F6), onSurfaceVariant = Color(0xFFB1B1BC), outline = Color(0xFF40404A), error = Color(0xFFFFB4AB))
@Composable fun DexTheme(prefs: WorkspacePreferences, content: @Composable () -> Unit) {
    val colors = when {
        prefs.highContrast -> DexColors.copy(primary = Color(0xFFFF7580), onSurfaceVariant = Color.White, outline = Color(0xFFB0B0BA))
        prefs.dynamicColors && Build.VERSION.SDK_INT >= 31 -> dynamicDarkColorScheme(LocalContext.current)
        else -> DexColors
    }
    MaterialTheme(colorScheme = colors, typography = Typography(
        headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 34.sp),
        headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 26.sp),
        titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
        labelSmall = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp)
    ), content = content)
}

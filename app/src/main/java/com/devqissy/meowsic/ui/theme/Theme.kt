package com.devqissy.meowsic.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devqissy.meowsic.data.preferences.PrefsFiles

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

@Immutable
data class AppThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    val neonPurple: Color,
    val neonBlue: Color,
    val neonPink: Color,
    val neonCyan: Color,
    val deepPurple: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val glassWhite: Color,
    val glassWhiteStrong: Color,
    val glassBorder: Color
)

object ThemePalettes {

    val galaxyDark = AppThemeColors(
        primary = Color(0xFFB691FF),
        secondary = Color(0xFF8FB8FF),
        tertiary = Color(0xFFFF9ED2),

        background = Color(0xFF0C0918),
        surface = Color(0xFF191330),

        onPrimary = Color(0xFF170B33),
        onSecondary = Color(0xFF0A1230),
        onBackground = Color(0xFFF4F0FF),
        onSurface = Color(0xFFEDE8FF),

        gradientStart = Color(0xFF1B1140),
        gradientMiddle = Color(0xFF3A2377),
        gradientEnd = Color(0xFF1F6FB8),

        neonPurple = Color(0xFFB388FF),
        neonBlue = Color(0xFF7BD5FF),
        neonPink = Color(0xFFFF7EB6),
        neonCyan = Color(0xFF7AF5D8),

        deepPurple = Color(0xFF120C26),

        textPrimary = Color(0xFFF6F2FF),
        textSecondary = Color(0xFFB7ACE0),
        textDisabled = Color(0xFF6F6591),

        glassWhite = Color(0x1FFFFFFF),
        glassWhiteStrong = Color(0x33FFFFFF),
        glassBorder = Color(0x30FFFFFF)
    )

    val galaxyLight = AppThemeColors(
        primary = Color(0xFF6C3BFF),
        secondary = Color(0xFF3D6BFF),
        tertiary = Color(0xFFD64FA5),

        background = Color(0xFFF5F2FF),
        surface = Color(0xFFFDFCFF),

        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1D1633),
        onSurface = Color(0xFF262046),

        gradientStart = Color(0xFFE9E2FF),
        gradientMiddle = Color(0xFFDED2FF),
        gradientEnd = Color(0xFFB993FF),

        neonPurple = Color(0xFF7C4DFF),
        neonBlue = Color(0xFF2E86FF),
        neonPink = Color(0xFFE6379A),
        neonCyan = Color(0xFF00A884),

        deepPurple = Color(0xFFE0D6FF),

        textPrimary = Color(0xFF1D1633),
        textSecondary = Color(0xFF675D8F),
        textDisabled = Color(0xFFA79EC7),

        glassWhite = Color(0x8CFFFFFF),
        glassWhiteStrong = Color(0xDBFFFFFF),
        glassBorder = Color(0x2E000000)
    )
}

val LocalAppThemeColors = compositionLocalOf {
    ThemePalettes.galaxyDark
}

val AppThemeColors.isDarkTheme: Boolean
    get() = background.luminance() < 0.5f

val AppThemeColors.glassPanelColor: Color
    get() = glassWhite

val AppThemeColors.glassSheen: Color
    get() = if (isDarkTheme) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }

val AppThemeColors.accentGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(primary, gradientEnd)
    )

val AppThemeColors.glassBorderAccent: Color
    get() = lerp(glassBorder, primary, 0.35f)

object GlassShapes {
    val panel = RoundedCornerShape(28.dp)
    val header = RoundedCornerShape(26.dp)
    val chip = RoundedCornerShape(50)
    val dialog = RoundedCornerShape(28.dp)
    val small = RoundedCornerShape(16.dp)
}

fun Modifier.glassSurface(
    shape: Shape,
    fillColor: Color,
    borderColor: Color,
    sheenColor: Color = Color.Transparent,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)

        drawOutline(outline = outline, color = fillColor)

        if (sheenColor != Color.Transparent) {
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(sheenColor, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.55f
                )
            )

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        sheenColor,
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = size.width
                ),
                start = Offset(size.width * 0.18f, borderWidth.toPx() / 2f),
                end = Offset(size.width * 0.82f, borderWidth.toPx() / 2f),
                strokeWidth = 1.2f
            )
        }

        drawOutline(
            outline = outline,
            color = borderColor,
            style = Stroke(width = borderWidth.toPx())
        )
    }

@Composable
fun NavaTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(PrefsFiles.SETTINGS, Context.MODE_PRIVATE)
    }

    var themeMode by remember {
        mutableStateOf(readThemeMode(prefs))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefsFiles.KEY_THEME_MODE) {
                themeMode = readThemeMode(prefs)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val systemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val themeColors = if (isDark) {
        ThemePalettes.galaxyDark
    } else {
        ThemePalettes.galaxyLight
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themeColors.primary,
            secondary = themeColors.secondary,
            tertiary = themeColors.tertiary,
            background = themeColors.background,
            surface = themeColors.surface,
            onPrimary = themeColors.onPrimary,
            onSecondary = themeColors.onSecondary,
            onBackground = themeColors.onBackground,
            onSurface = themeColors.onSurface
        )
    } else {
        lightColorScheme(
            primary = themeColors.primary,
            secondary = themeColors.secondary,
            tertiary = themeColors.tertiary,
            background = themeColors.background,
            surface = themeColors.surface,
            onPrimary = themeColors.onPrimary,
            onSecondary = themeColors.onSecondary,
            onBackground = themeColors.onBackground,
            onSurface = themeColors.onSurface
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalAppThemeColors provides themeColors,
                content = content
            )
        }
    )
}

private fun readThemeMode(prefs: SharedPreferences): ThemeMode {
    return when (prefs.getString(PrefsFiles.KEY_THEME_MODE, "system")) {
        "dark" -> ThemeMode.DARK
        "light" -> ThemeMode.LIGHT
        else -> ThemeMode.SYSTEM
    }
}
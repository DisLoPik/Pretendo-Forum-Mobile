package com.dislopik.pretendo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.dislopik.pretendo.data.AccentColor
import com.dislopik.pretendo.data.AccessibilitySettings
import com.dislopik.pretendo.data.FontChoice
import com.dislopik.pretendo.data.LetterSpacing
import com.dislopik.pretendo.data.ThemeMode

/**
 * Pretendo Network's own palette, taken from the site's stylesheet
 * (`--bg-shade-*`, `--text-shade-*`, `--accent-shade-*` in `_nuxt/entry.css`).
 */
object PretendoColors {
    val BgDarkest = Color(0xFF131733)
    val BgDark = Color(0xFF1B1F3B)
    val BgMid = Color(0xFF23274A)
    val BgRaised = Color(0xFF2D3258)
    val BgEdge = Color(0xFF373C65)
    val BgEdgeBright = Color(0xFF404673)
    val BgHighest = Color(0xFF494F81)

    val TextMuted = Color(0xFF8990C1)
    val TextSecondary = Color(0xFFA1A8D9)
    val TextLilac = Color(0xFFCAC1F5)

    val Green = Color(0xFF37A985)
    val GreenBright = Color(0xFF59C9A5)
    val Red = Color(0xFFA9375B)
    val RedBright = Color(0xFFE84059)
    val Yellow = Color(0xFFDAA401)
    val YellowBright = Color(0xFFFFD966)
}

/**
 * Spacing that answers to the density and tap-target accessibility options, so a single
 * setting widens every row in the app rather than only the ones someone remembered.
 */
@Immutable
data class Dimens(
    val screenPadding: Dp,
    val cardPadding: Dp,
    val itemSpacing: Dp,
    val sectionSpacing: Dp,
    val minTouchTarget: Dp,
    val avatarSize: Dp,
    val corner: Dp
)

val LocalDimens = staticCompositionLocalOf {
    dimensFor(AccessibilitySettings())
}

/** The settings in force, so any screen can honour them without threading a parameter. */
val LocalSettings = staticCompositionLocalOf { AccessibilitySettings() }

fun dimensFor(settings: AccessibilitySettings): Dimens {
    val scale = settings.density.scale
    val touch = if (settings.largeTapTargets) 56.dp else 48.dp
    return Dimens(
        screenPadding = (16 * scale).dp,
        cardPadding = (14 * scale).dp,
        itemSpacing = (10 * scale).dp,
        sectionSpacing = (20 * scale).dp,
        minTouchTarget = touch,
        avatarSize = if (settings.largeTapTargets) 44.dp else 38.dp,
        corner = 14.dp
    )
}

@Composable
fun PretendoTheme(
    settings: AccessibilitySettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Black -> true
    }

    val colorScheme = remember(settings.themeMode, settings.accentColor, settings.highContrast, dark) {
        buildColorScheme(settings, dark)
    }
    val typography = remember(
        settings.effectiveTextScale,
        settings.fontChoice,
        settings.boldText,
        settings.lineSpacing,
        settings.letterSpacing
    ) {
        buildTypography(settings)
    }
    val dimens = remember(settings.density, settings.largeTapTargets) { dimensFor(settings) }

    CompositionLocalProvider(
        LocalDimens provides dimens,
        LocalSettings provides settings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}


private fun accentFor(accent: AccentColor, dark: Boolean): Color =
    Color(0xFF000000L or (if (dark) accent.onDark else accent.onLight))

private fun buildColorScheme(settings: AccessibilitySettings, dark: Boolean): ColorScheme {
    val accent = accentFor(settings.accentColor, dark)
    return if (dark) {
        darkScheme(settings, accent)
    } else {
        lightScheme(settings, accent)
    }
}

private fun darkScheme(settings: AccessibilitySettings, accent: Color): ColorScheme {
    val black = settings.themeMode == ThemeMode.Black
    val contrast = settings.highContrast

    // Black mode drops to true black for OLED; high contrast deepens the background and
    // brightens the text so the gap between them is as wide as the palette allows.
    val background = when {
        black -> Color.Black
        contrast -> PretendoColors.BgDarkest
        else -> PretendoColors.BgDark
    }
    val surface = when {
        black -> Color(0xFF0C0C0F)
        contrast -> PretendoColors.BgDark
        else -> PretendoColors.BgMid
    }
    val surfaceHigh = if (black) Color(0xFF17171C) else PretendoColors.BgRaised
    val onSurface = if (contrast || black) Color.White else Color(0xFFF2F3FA)
    val onSurfaceVariant = if (contrast) Color(0xFFD5DAF2) else PretendoColors.TextSecondary
    val outline = if (black) Color(0xFF3A3A44) else PretendoColors.BgEdge

    return darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF16183A),
        primaryContainer = accent,
        onPrimaryContainer = Color(0xFF11132E),

        secondary = PretendoColors.TextLilac,
        onSecondary = Color(0xFF16183A),
        secondaryContainer = if (black) Color(0xFF23232B) else PretendoColors.BgEdge,
        onSecondaryContainer = Color.White,

        tertiary = PretendoColors.GreenBright,
        onTertiary = Color(0xFF06251C),
        tertiaryContainer = PretendoColors.Green,
        onTertiaryContainer = Color.White,

        background = background,
        onBackground = onSurface,

        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surface,

        surfaceContainerLowest = background,
        surfaceContainerLow = if (black) Color(0xFF0A0A0D) else PretendoColors.BgDarkest,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = if (black) Color(0xFF1F1F26) else PretendoColors.BgEdge,

        outline = outline,
        outlineVariant = if (black) Color(0xFF2A2A31) else PretendoColors.BgEdgeBright,

        inverseSurface = Color(0xFFE4E5F2),
        inverseOnSurface = PretendoColors.BgDark,
        inversePrimary = accentFor(settings.accentColor, dark = false),

        error = PretendoColors.RedBright,
        onError = Color.White,
        errorContainer = PretendoColors.Red,
        onErrorContainer = Color.White,

        scrim = Color(0xCC05060F)
    )
}

private fun lightScheme(settings: AccessibilitySettings, accent: Color): ColorScheme {
    val contrast = settings.highContrast
    val onSurface = if (contrast) Color(0xFF07070C) else Color(0xFF1A1B26)
    val onSurfaceVariant = if (contrast) Color(0xFF2A2B38) else Color(0xFF565873)

    return lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent,
        onPrimaryContainer = Color.White,

        secondary = Color(0xFF4A4C6B),
        onSecondary = Color.White,
        secondaryContainer = if (contrast) Color(0xFFD8DAEC) else Color(0xFFE6E7F4),
        onSecondaryContainer = onSurface,

        tertiary = Color(0xFF0F6B53),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFB8EBDA),
        onTertiaryContainer = Color(0xFF04231B),

        background = if (contrast) Color.White else Color(0xFFF6F6FB),
        onBackground = onSurface,

        surface = Color.White,
        onSurface = onSurface,
        surfaceVariant = if (contrast) Color(0xFFEDEEF6) else Color(0xFFF0F1F8),
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = Color.White,

        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAFAFE),
        surfaceContainer = if (contrast) Color(0xFFF2F2F8) else Color(0xFFF1F2F9),
        surfaceContainerHigh = Color(0xFFE9EAF4),
        surfaceContainerHighest = Color(0xFFE1E3F0),

        outline = if (contrast) Color(0xFF585A6B) else Color(0xFFC5C7DA),
        outlineVariant = if (contrast) Color(0xFF8E90A3) else Color(0xFFDDDEEB),

        inverseSurface = PretendoColors.BgDark,
        inverseOnSurface = Color.White,
        inversePrimary = accentFor(settings.accentColor, dark = true),

        error = Color(0xFFB3172F),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD9),
        onErrorContainer = Color(0xFF410004),

        scrim = Color(0x99000000)
    )
}


private fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.System -> FontFamily.Default
    FontChoice.Serif -> FontFamily.Serif
    FontChoice.SansSerif -> FontFamily.SansSerif
    FontChoice.Monospace -> FontFamily.Monospace
}

/**
 * A "bold text" setting has to move every weight, not just body text, or headings stop
 * standing out from the paragraphs underneath them.
 */
private fun FontWeight.bolder(): FontWeight = when {
    weight >= 700 -> FontWeight.Black
    weight >= 500 -> FontWeight.ExtraBold
    else -> FontWeight.Bold
}

private fun buildTypography(settings: AccessibilitySettings): Typography {
    val base = Typography()
    val scale = settings.effectiveTextScale
    val family = fontFamilyFor(settings.fontChoice)

    fun TextStyle.adjust(): TextStyle {
        val size: TextUnit = if (fontSize.isUnspecified) 14.sp else fontSize * scale
        return copy(
            fontFamily = family,
            fontSize = size,
            fontWeight = if (settings.boldText) (fontWeight ?: FontWeight.Normal).bolder() else fontWeight,
            lineHeight = size * settings.lineSpacing.multiplier,
            letterSpacing = if (settings.letterSpacing == LetterSpacing.Normal) {
                letterSpacing
            } else {
                settings.letterSpacing.emValue.em
            }
        )
    }

    return Typography(
        displayLarge = base.displayLarge.adjust(),
        displayMedium = base.displayMedium.adjust(),
        displaySmall = base.displaySmall.adjust(),
        headlineLarge = base.headlineLarge.adjust(),
        headlineMedium = base.headlineMedium.adjust(),
        headlineSmall = base.headlineSmall.adjust(),
        titleLarge = base.titleLarge.adjust(),
        titleMedium = base.titleMedium.adjust(),
        titleSmall = base.titleSmall.adjust(),
        bodyLarge = base.bodyLarge.adjust(),
        bodyMedium = base.bodyMedium.adjust(),
        bodySmall = base.bodySmall.adjust(),
        labelLarge = base.labelLarge.adjust(),
        labelMedium = base.labelMedium.adjust(),
        labelSmall = base.labelSmall.adjust()
    )
}

/** The colour a category chip draws with, from the hex Discourse stores for it. */
fun categoryColor(hex: String?): Color {
    val cleaned = hex?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return PretendoColors.TextMuted
    val value = cleaned.toLongOrNull(16) ?: return PretendoColors.TextMuted
    return Color(0xFF000000L or value)
}

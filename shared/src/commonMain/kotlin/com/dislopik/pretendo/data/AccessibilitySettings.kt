package com.dislopik.pretendo.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which colour scheme the app draws with. */
enum class ThemeMode(val label: String, val description: String) {
    System("Follow system", "Match the phone's light or dark setting"),
    Dark("Dark", "The Pretendo Network navy, like the website"),
    Light("Light", "Dark text on a pale background"),
    Black("Black", "True black, easier on OLED screens and on the eyes at night")
}

/** The typeface used for everything the app draws. */
enum class FontChoice(val label: String, val description: String) {
    System("System", "The phone's normal typeface"),
    Serif("Serif", "Letters with strokes on the ends, which some people track more easily"),
    SansSerif("Sans serif", "Plain letters with even strokes"),
    Monospace("Monospace", "Every letter the same width, which keeps columns steady")
}

/** Space between lines inside a paragraph. */
enum class LineSpacing(val label: String, val multiplier: Float) {
    Tight("Tight", 1.15f),
    Normal("Normal", 1.4f),
    Relaxed("Relaxed", 1.7f),
    Loose("Loose", 2.0f)
}

/** Space between letters, which can make dense text easier to pick apart. */
enum class LetterSpacing(val label: String, val emValue: Float) {
    Normal("Normal", 0f),
    Wide("Wide", 0.03f),
    Wider("Wider", 0.07f)
}

/** How much padding sits around and between things. */
enum class UiDensity(val label: String, val scale: Float) {
    Compact("Compact", 0.75f),
    Comfortable("Comfortable", 1f),
    Spacious("Spacious", 1.35f)
}

/**
 * The colour used for buttons, links and highlights.
 *
 * Each option carries two values because a colour that is readable on the navy background
 * is too pale to read on white, and the other way round.
 */
enum class AccentColor(val label: String, val onDark: Long, val onLight: Long) {
    Purple("Pretendo purple", 0x9D6FF3, 0x5B34A8),
    Blue("Blue", 0x5CB3F7, 0x0B62A4),
    Teal("Teal", 0x59C9A5, 0x0F6B53),
    Amber("Amber", 0xFFD966, 0x855C00),
    Pink("Pink", 0xF57BB0, 0xA3175C)
}

/** How much of a post to show before a "Show more" control appears. */
enum class PostLength(val label: String, val collapsedLines: Int) {
    Full("Show every post in full", 0),
    Medium("Collapse long posts", 18),
    Short("Collapse posts hard", 8)
}

/**
 * Everything on the Accessibility page.
 *
 * Kept as one immutable value so a change redraws the app in a single recomposition, and
 * so the whole thing can be written to disk field by field without a serializer.
 */
@Immutable
data class AccessibilitySettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accentColor: AccentColor = AccentColor.Purple,
    val textScale: Float = 1f,
    val fontChoice: FontChoice = FontChoice.System,
    val boldText: Boolean = false,
    val lineSpacing: LineSpacing = LineSpacing.Normal,
    val letterSpacing: LetterSpacing = LetterSpacing.Normal,
    val density: UiDensity = UiDensity.Comfortable,
    val highContrast: Boolean = false,
    val underlineLinks: Boolean = true,
    val reduceMotion: Boolean = false,
    val showAvatars: Boolean = true,
    val showImages: Boolean = true,
    val largeTapTargets: Boolean = false,
    val absoluteDates: Boolean = false,
    val keepScreenOn: Boolean = false,
    val postLength: PostLength = PostLength.Full,
    val showQuotesExpanded: Boolean = false
) {
    /** Clamped so a stored value from a future version can never make the app unusable. */
    val effectiveTextScale: Float get() = textScale.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)

    companion object {
        const val MIN_TEXT_SCALE = 0.85f
        const val MAX_TEXT_SCALE = 2.2f
    }
}

/**
 * Reads and writes [AccessibilitySettings] on the device.
 *
 * Discourse accounts have nowhere to store any of this, so it is deliberately per-device:
 * the phone that needs bigger text is not necessarily the only place the account is used.
 */
class SettingsController(private val store: KeyValueStore = KeyValueStore()) {

    var settings: AccessibilitySettings by mutableStateOf(load())
        private set

    /** Applies a change and writes it straight through, so nothing is lost on a hard close. */
    fun update(transform: (AccessibilitySettings) -> AccessibilitySettings) {
        val updated = transform(settings)
        settings = updated
        save(updated)
    }

    fun resetToDefaults() {
        val defaults = AccessibilitySettings()
        settings = defaults
        save(defaults)
    }

    private fun load(): AccessibilitySettings {
        val defaults = AccessibilitySettings()
        return AccessibilitySettings(
            themeMode = store.getEnum(THEME, defaults.themeMode),
            accentColor = store.getEnum(ACCENT, defaults.accentColor),
            textScale = store.getFloat(TEXT_SCALE, defaults.textScale),
            fontChoice = store.getEnum(FONT, defaults.fontChoice),
            boldText = store.getBoolean(BOLD, defaults.boldText),
            lineSpacing = store.getEnum(LINE_SPACING, defaults.lineSpacing),
            letterSpacing = store.getEnum(LETTER_SPACING, defaults.letterSpacing),
            density = store.getEnum(DENSITY, defaults.density),
            highContrast = store.getBoolean(CONTRAST, defaults.highContrast),
            underlineLinks = store.getBoolean(UNDERLINE, defaults.underlineLinks),
            reduceMotion = store.getBoolean(REDUCE_MOTION, defaults.reduceMotion),
            showAvatars = store.getBoolean(AVATARS, defaults.showAvatars),
            showImages = store.getBoolean(IMAGES, defaults.showImages),
            largeTapTargets = store.getBoolean(TAP_TARGETS, defaults.largeTapTargets),
            absoluteDates = store.getBoolean(ABSOLUTE_DATES, defaults.absoluteDates),
            keepScreenOn = store.getBoolean(KEEP_AWAKE, defaults.keepScreenOn),
            postLength = store.getEnum(POST_LENGTH, defaults.postLength),
            showQuotesExpanded = store.getBoolean(QUOTES, defaults.showQuotesExpanded)
        )
    }

    private fun save(value: AccessibilitySettings) {
        store.putEnum(THEME, value.themeMode)
        store.putEnum(ACCENT, value.accentColor)
        store.putFloat(TEXT_SCALE, value.textScale)
        store.putEnum(FONT, value.fontChoice)
        store.putBoolean(BOLD, value.boldText)
        store.putEnum(LINE_SPACING, value.lineSpacing)
        store.putEnum(LETTER_SPACING, value.letterSpacing)
        store.putEnum(DENSITY, value.density)
        store.putBoolean(CONTRAST, value.highContrast)
        store.putBoolean(UNDERLINE, value.underlineLinks)
        store.putBoolean(REDUCE_MOTION, value.reduceMotion)
        store.putBoolean(AVATARS, value.showAvatars)
        store.putBoolean(IMAGES, value.showImages)
        store.putBoolean(TAP_TARGETS, value.largeTapTargets)
        store.putBoolean(ABSOLUTE_DATES, value.absoluteDates)
        store.putBoolean(KEEP_AWAKE, value.keepScreenOn)
        store.putEnum(POST_LENGTH, value.postLength)
        store.putBoolean(QUOTES, value.showQuotesExpanded)
    }

    private companion object {
        const val THEME = "a11y.theme"
        const val ACCENT = "a11y.accent"
        const val TEXT_SCALE = "a11y.text_scale"
        const val FONT = "a11y.font"
        const val BOLD = "a11y.bold"
        const val LINE_SPACING = "a11y.line_spacing"
        const val LETTER_SPACING = "a11y.letter_spacing"
        const val DENSITY = "a11y.density"
        const val CONTRAST = "a11y.high_contrast"
        const val UNDERLINE = "a11y.underline_links"
        const val REDUCE_MOTION = "a11y.reduce_motion"
        const val AVATARS = "a11y.show_avatars"
        const val IMAGES = "a11y.show_images"
        const val TAP_TARGETS = "a11y.large_tap_targets"
        const val ABSOLUTE_DATES = "a11y.absolute_dates"
        const val KEEP_AWAKE = "a11y.keep_screen_on"
        const val POST_LENGTH = "a11y.post_length"
        const val QUOTES = "a11y.quotes_expanded"
    }
}

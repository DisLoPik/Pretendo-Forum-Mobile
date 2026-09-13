package com.dislopik.pretendo

import com.dislopik.pretendo.data.AccessibilitySettings
import com.dislopik.pretendo.data.LineSpacing
import com.dislopik.pretendo.data.PostLength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The accessibility settings are written to the phone field by field, so a value stored by
 * some future version has to be survivable rather than able to make the app unreadable.
 */
class AccessibilitySettingsTest {

    @Test
    fun textScaleIsClampedToSomethingReadable() {
        assertEquals(
            AccessibilitySettings.MIN_TEXT_SCALE,
            AccessibilitySettings(textScale = 0.1f).effectiveTextScale
        )
        assertEquals(
            AccessibilitySettings.MAX_TEXT_SCALE,
            AccessibilitySettings(textScale = 99f).effectiveTextScale
        )
        assertEquals(1.5f, AccessibilitySettings(textScale = 1.5f).effectiveTextScale)
    }

    @Test
    fun defaultsAreTheUnchangedApp() {
        val defaults = AccessibilitySettings()
        assertEquals(1f, defaults.effectiveTextScale)
        assertEquals(LineSpacing.Normal, defaults.lineSpacing)
        assertEquals(PostLength.Full, defaults.postLength)
        assertTrue(defaults.showImages)
        assertTrue(defaults.showAvatars)
        assertTrue(defaults.underlineLinks)
    }

    @Test
    fun lineSpacingOptionsOnlyEverAddSpace() {
        val multipliers = LineSpacing.entries.map { it.multiplier }
        assertEquals(multipliers.sorted(), multipliers)
        assertTrue(multipliers.all { it >= 1f })
    }

    @Test
    fun collapsingPostsMeansAnActualLineLimit() {
        assertEquals(0, PostLength.Full.collapsedLines)
        assertTrue(PostLength.Medium.collapsedLines > PostLength.Short.collapsedLines)
        assertTrue(PostLength.Short.collapsedLines > 0)
    }
}

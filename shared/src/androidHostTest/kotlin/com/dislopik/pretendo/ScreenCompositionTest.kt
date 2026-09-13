package com.dislopik.pretendo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.dislopik.pretendo.data.AccessibilitySettings
import com.dislopik.pretendo.data.FontChoice
import com.dislopik.pretendo.data.ThemeMode
import com.dislopik.pretendo.data.UiDensity
import com.dislopik.pretendo.data.initPretendoAndroid
import com.dislopik.pretendo.ui.ComposerRequest
import com.dislopik.pretendo.ui.PretendoTheme
import com.dislopik.pretendo.ui.screens.AboutScreen
import com.dislopik.pretendo.ui.screens.AccessibilityScreen
import com.dislopik.pretendo.ui.screens.ComposerScreen
import com.dislopik.pretendo.ui.screens.SearchTab
import com.dislopik.pretendo.ui.screens.SettingsScreen
import com.dislopik.pretendo.ui.screens.SignInScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Draws each screen on the JVM.
 *
 * A screen that compiles can still die the moment it is laid out, which is how a
 * mismatched Material version once took out every screen with a text field on it. These
 * catch that class of failure without a device attached, so they are worth keeping even
 * though they assert little beyond "this draws".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenCompositionTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        initPretendoAndroid(ApplicationProvider.getApplicationContext())
    }

    private fun screen(content: @Composable () -> Unit) {
        compose.setContent {
            PretendoTheme(AccessibilitySettings()) { content() }
        }
        compose.waitForIdle()
    }

    @Test
    fun signInDraws() {
        screen { SignInScreen(onBack = {}, onSignedIn = {}) }
        compose.onNodeWithText("Username").assertExists()
        compose.onNodeWithText("Password").assertExists()
    }

    @Test
    fun composerDrawsForANewTopic() {
        screen {
            ComposerScreen(
                request = ComposerRequest.NewTopic(null, null),
                onBack = {},
                onPosted = { _, _ -> }
            )
        }
        compose.onNodeWithText("New topic").assertExists()
        compose.onNodeWithText("Choose a category").assertExists()
    }

    @Test
    fun composerDrawsForAReply() {
        screen {
            ComposerScreen(
                request = ComposerRequest.Reply(
                    topicId = 1,
                    topicTitle = "A topic",
                    replyToPostNumber = 2,
                    replyToUsername = "someone"
                ),
                onBack = {},
                onPosted = { _, _ -> }
            )
        }
        compose.onNodeWithText("Reply").assertExists()
    }

    @Test
    fun searchDrawsItsEmptyState() {
        // An empty query never reaches the network, so this stays a pure layout test.
        screen { SearchTab(onOpenTopic = { _, _ -> }, onOpenProfile = {}) }
        compose.onNodeWithText("Search the forum").assertExists()
    }

    @Test
    fun accessibilityPageDraws() {
        screen { AccessibilityScreen(onBack = {}) }
        compose.onNodeWithText("Text size").assertExists()
        compose.onNodeWithText("Bold text").assertExists()
    }

    /**
     * Every theme has to survive being built, at the extremes of the other settings.
     *
     * The theme is switched through state rather than by composing again, because a test
     * gets one composition and swapping the theme under a live one is what the app does
     * anyway when the setting is changed.
     */
    @Test
    fun accessibilityPageDrawsUnderEveryTheme() {
        val theme = mutableStateOf(ThemeMode.System)
        compose.setContent {
            PretendoTheme(
                AccessibilitySettings(
                    themeMode = theme.value,
                    fontChoice = FontChoice.Serif,
                    textScale = AccessibilitySettings.MAX_TEXT_SCALE,
                    boldText = true,
                    highContrast = true,
                    density = UiDensity.Spacious
                )
            ) {
                AboutScreen(onBack = {})
            }
        }
        for (mode in ThemeMode.entries) {
            theme.value = mode
            compose.waitForIdle()
            compose.onNodeWithText("Pretendo Forum").assertExists()
        }
    }

    @Test
    fun settingsDrawsWhileSignedOut() {
        screen {
            SettingsScreen(
                onBack = {},
                onOpenAccessibility = {},
                onOpenAbout = {},
                onOpenProfile = {},
                onSignIn = {},
                onSignOut = {}
            )
        }
        compose.onNodeWithText("Accessibility").assertExists()
    }

    @Test
    fun aboutDraws() {
        screen { AboutScreen(onBack = {}) }
        compose.onNodeWithText("Pretendo Forum").assertExists()
    }
}

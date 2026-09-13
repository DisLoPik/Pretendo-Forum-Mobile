package com.dislopik.pretendo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.KeepScreenOn
import com.dislopik.pretendo.ui.ComposerRequest
import com.dislopik.pretendo.ui.PretendoTheme
import com.dislopik.pretendo.ui.Screen
import com.dislopik.pretendo.ui.rememberNavigator
import com.dislopik.pretendo.ui.screens.AboutScreen
import com.dislopik.pretendo.ui.screens.AccessibilityScreen
import com.dislopik.pretendo.ui.screens.CategoryTopicsScreen
import com.dislopik.pretendo.ui.screens.ComposerScreen
import com.dislopik.pretendo.ui.screens.HomeScreen
import com.dislopik.pretendo.ui.screens.MessagesScreen
import com.dislopik.pretendo.ui.screens.ProfileScreen
import com.dislopik.pretendo.ui.screens.SettingsScreen
import com.dislopik.pretendo.ui.screens.SignInScreen
import com.dislopik.pretendo.ui.screens.TagTopicsScreen
import com.dislopik.pretendo.ui.screens.TopicScreen

// BackHandler is deprecated in favour of NavigationEventHandler, which this Compose version
// does not ship yet, so it stays until the replacement is available.
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(!Forum.settings.settings.reduceMotion)
            .build()
    }

    val settings = Forum.settings.settings

    PretendoTheme(settings) {
        val navigator = rememberNavigator()

        // The session is what tells the app whose name to show and how many alerts are
        // waiting; reading the forum works whether or not it resolves.
        LaunchedEffect(Forum.session.refreshKey) {
            Forum.session.refresh()
            Forum.site.ensureLoaded()
        }

        KeepScreenOn(settings.keepScreenOn)

        BackHandler(enabled = navigator.canGoBack) { navigator.pop() }

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            AppContent(navigator, Modifier.safeDrawingPadding())
        }
    }
}

@Composable
private fun AppContent(
    navigator: com.dislopik.pretendo.ui.Navigator,
    modifier: Modifier = Modifier
) {
    val refreshKey = Forum.session.refreshKey

    when (val screen = navigator.current) {
        Screen.Home -> HomeScreen(
            tab = navigator.homeTab,
            onTabChange = navigator::selectTab,
            filter = navigator.topicFilter,
            onFilterChange = navigator::selectFilter,
            refreshKey = refreshKey,
            onOpenTopic = { id, postNumber, title ->
                navigator.push(Screen.Topic(id, postNumber, title))
            },
            onOpenCategory = { slug, id, name ->
                navigator.push(Screen.CategoryTopics(slug, id, name))
            },
            onOpenTag = { navigator.push(Screen.TagTopics(it)) },
            onOpenProfile = { navigator.push(Screen.Profile(it)) },
            onOpenLink = navigator::open,
            onOpenMessages = { navigator.push(Screen.Messages) },
            onOpenSettings = { navigator.push(Screen.Settings) },
            onOpenAccessibility = { navigator.push(Screen.Accessibility) },
            onCompose = { navigator.push(Screen.Composer(it)) },
            onSignIn = { navigator.push(Screen.SignIn) },
            modifier = modifier
        )

        is Screen.CategoryTopics -> CategoryTopicsScreen(
            slug = screen.slug,
            categoryId = screen.categoryId,
            name = screen.name,
            onBack = { navigator.pop() },
            onOpenTopic = { id, title -> navigator.push(Screen.Topic(id, null, title)) },
            onOpenTag = { navigator.push(Screen.TagTopics(it)) },
            onNewTopic = {
                navigator.push(
                    Screen.Composer(ComposerRequest.NewTopic(screen.categoryId, screen.name))
                )
            },
            modifier = modifier
        )

        is Screen.TagTopics -> TagTopicsScreen(
            tag = screen.tag,
            onBack = { navigator.pop() },
            onOpenTopic = { id, title -> navigator.push(Screen.Topic(id, null, title)) },
            onOpenCategory = { slug, id, name ->
                navigator.push(Screen.CategoryTopics(slug, id, name))
            },
            onOpenTag = { navigator.push(Screen.TagTopics(it)) },
            modifier = modifier
        )

        is Screen.Topic -> TopicScreen(
            topicId = screen.topicId,
            startPostNumber = screen.postNumber,
            fallbackTitle = screen.title,
            refreshKey = refreshKey,
            onBack = { navigator.pop() },
            onOpenProfile = { navigator.push(Screen.Profile(it)) },
            onOpenCategory = { slug, id, name ->
                navigator.push(Screen.CategoryTopics(slug, id, name))
            },
            onOpenTag = { navigator.push(Screen.TagTopics(it)) },
            onOpenLink = navigator::open,
            onCompose = { navigator.push(Screen.Composer(it)) },
            onSignIn = { navigator.push(Screen.SignIn) },
            modifier = modifier
        )

        is Screen.Profile -> ProfileScreen(
            username = screen.username,
            onBack = { navigator.pop() },
            onOpenTopic = { id, postNumber, title ->
                navigator.push(Screen.Topic(id, postNumber, title))
            },
            onOpenLink = navigator::open,
            onSendMessage = {
                navigator.push(Screen.Composer(ComposerRequest.NewMessage(it)))
            },
            modifier = modifier
        )

        Screen.Messages -> MessagesScreen(
            refreshKey = refreshKey,
            onBack = { navigator.pop() },
            onOpenMessage = { id, title -> navigator.push(Screen.Topic(id, null, title)) },
            onNewMessage = {
                navigator.push(Screen.Composer(ComposerRequest.NewMessage(null)))
            },
            onSignIn = { navigator.push(Screen.SignIn) },
            modifier = modifier
        )

        Screen.Settings -> SettingsScreen(
            onBack = { navigator.pop() },
            onOpenAccessibility = { navigator.push(Screen.Accessibility) },
            onOpenAbout = { navigator.push(Screen.About) },
            onOpenProfile = { navigator.push(Screen.Profile(it)) },
            onSignIn = { navigator.push(Screen.SignIn) },
            onSignOut = {
                Forum.session.signOut()
                navigator.popToHome()
            },
            modifier = modifier
        )

        Screen.Accessibility -> AccessibilityScreen(
            onBack = { navigator.pop() },
            modifier = modifier
        )

        Screen.About -> AboutScreen(onBack = { navigator.pop() }, modifier = modifier)

        Screen.SignIn -> SignInScreen(
            onBack = { navigator.pop() },
            onSignedIn = { navigator.popToHome() },
            modifier = modifier
        )

        is Screen.Composer -> ComposerScreen(
            request = screen.request,
            onBack = { navigator.pop() },
            onPosted = { topicId, postNumber ->
                navigator.pop()
                // A brand new topic or message is worth opening; a reply lands back in the
                // topic that is already underneath.
                val request = screen.request
                if (topicId != null &&
                    (request is ComposerRequest.NewTopic || request is ComposerRequest.NewMessage)
                ) {
                    navigator.push(Screen.Topic(topicId, postNumber, null))
                }
            },
            modifier = modifier
        )
    }
}

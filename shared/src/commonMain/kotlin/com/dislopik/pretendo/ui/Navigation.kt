package com.dislopik.pretendo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dislopik.pretendo.model.TopicFilter

/** Every place the app can be. */
sealed interface Screen {
    /** The tabbed shell: topic lists, categories, search, notifications and the user. */
    data object Home : Screen

    data class CategoryTopics(val slug: String, val categoryId: Int, val name: String) : Screen
    data class TagTopics(val tag: String) : Screen
    data class Topic(val topicId: Long, val postNumber: Int? = null, val title: String? = null) : Screen
    data class Profile(val username: String) : Screen
    data object Messages : Screen
    data object Settings : Screen
    data object Accessibility : Screen
    data object About : Screen
    data object SignIn : Screen
    data class Composer(val request: ComposerRequest) : Screen
}

/** What the composer is being opened for. One screen covers every kind of writing. */
sealed interface ComposerRequest {
    /** A brand new topic, optionally starting in a chosen category. */
    data class NewTopic(val categoryId: Int?, val categoryName: String?) : ComposerRequest

    data class Reply(
        val topicId: Long,
        val topicTitle: String,
        val replyToPostNumber: Int?,
        val replyToUsername: String?,
        val quoted: String? = null,
        val isPrivateMessage: Boolean = false
    ) : ComposerRequest

    data class Edit(val postId: Long, val topicTitle: String, val existingRaw: String?) : ComposerRequest

    /** A new private message, optionally already addressed to someone. */
    data class NewMessage(val to: String?) : ComposerRequest
}

/** The tabs in the home shell. */
enum class HomeTab(val label: String) {
    Topics("Topics"),
    Categories("Categories"),
    Search("Search"),
    Notifications("Alerts"),
    You("You")
}

/**
 * A small back stack. The app has a handful of screens and no deep-link tree, so this
 * beats pulling in a navigation library that would need its own serialization setup.
 */
@Stable
class Navigator(initial: Screen = Screen.Home) {
    private val stack = mutableStateListOf(initial)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    /** Which tab the home shell is showing, kept here so it survives a trip to a topic. */
    var homeTab: HomeTab by mutableStateOf(HomeTab.Topics)
        private set

    /** Which list the Topics tab is showing. */
    var topicFilter: TopicFilter by mutableStateOf(TopicFilter.Latest)
        private set

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Drops everything above the home shell, used after signing in or out. */
    fun popToHome() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun selectTab(tab: HomeTab) {
        homeTab = tab
    }

    fun selectFilter(filter: TopicFilter) {
        topicFilter = filter
    }

    /** Sends the reader wherever a link inside a post points. */
    fun open(link: com.dislopik.pretendo.ui.html.ForumLink) {
        when (link) {
            is com.dislopik.pretendo.ui.html.ForumLink.Topic ->
                push(Screen.Topic(link.id, link.postNumber))

            is com.dislopik.pretendo.ui.html.ForumLink.User ->
                push(Screen.Profile(link.username))

            is com.dislopik.pretendo.ui.html.ForumLink.Category ->
                push(Screen.CategoryTopics(link.slug, link.id ?: 0, link.slug))

            is com.dislopik.pretendo.ui.html.ForumLink.Tag ->
                push(Screen.TagTopics(link.name))

            is com.dislopik.pretendo.ui.html.ForumLink.Search -> {
                selectTab(HomeTab.Search)
                popToHome()
            }

            is com.dislopik.pretendo.ui.html.ForumLink.External ->
                com.dislopik.pretendo.data.openInBrowser(link.url)
        }
    }
}

@Composable
fun rememberNavigator(): Navigator = remember { Navigator() }

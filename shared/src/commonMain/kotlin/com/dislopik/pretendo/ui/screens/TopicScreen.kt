package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.data.ForumAuthException
import com.dislopik.pretendo.data.copyToClipboard
import com.dislopik.pretendo.data.shareText
import com.dislopik.pretendo.model.ActionSummaryDto
import com.dislopik.pretendo.model.FlagReason
import com.dislopik.pretendo.model.PostActionType
import com.dislopik.pretendo.model.PostDto
import com.dislopik.pretendo.model.TopicDetailDto
import com.dislopik.pretendo.model.canUnlike
import com.dislopik.pretendo.model.isPrivateMessage
import com.dislopik.pretendo.model.likedByMe
import com.dislopik.pretendo.ui.ComposerRequest
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.CategoryChip
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.ForumTopBar
import com.dislopik.pretendo.ui.components.InlineLoading
import com.dislopik.pretendo.ui.components.LoadingBox
import com.dislopik.pretendo.ui.components.PostActions
import com.dislopik.pretendo.ui.components.PostCard
import com.dislopik.pretendo.ui.components.StatLabel
import com.dislopik.pretendo.ui.components.TagChip
import com.dislopik.pretendo.ui.html.ForumLink
import kotlinx.coroutines.launch

/**
 * One topic and its posts.
 *
 * Discourse sends the topic with only the first chunk of posts and a full list of every
 * post id, so the rest is fetched in chunks as the reader scrolls.
 */
@Stable
class TopicState(private val topicId: Long, private val startPostNumber: Int?) {

    var topic: TopicDetailDto? by mutableStateOf(null)
        private set

    var posts: List<PostDto> by mutableStateOf(emptyList())
        private set

    var loading: Boolean by mutableStateOf(true)
        private set

    var loadingMore: Boolean by mutableStateOf(false)
        private set

    var error: String? by mutableStateOf(null)
        private set

    var authLost: Boolean by mutableStateOf(false)
        private set

    var bookmarked: Boolean by mutableStateOf(false)
        private set

    private var stream: List<Long> = emptyList()

    val hasMore: Boolean get() = posts.size < stream.size

    val canReply: Boolean get() = topic?.details?.canCreatePost == true && Forum.auth.isSignedIn

    suspend fun load() {
        loading = true
        error = null
        try {
            runCatchingUnlessCancelled { Forum.api.topic(topicId, startPostNumber) }
                .onSuccess { detail ->
                    topic = detail
                    stream = detail.postStream.stream.ifEmpty {
                        detail.postStream.posts.map { it.id }
                    }
                    posts = detail.postStream.posts
                    bookmarked = detail.bookmarked == true
                }
                .onFailure { failure ->
                    if (failure is ForumAuthException) authLost = true
                    error = failure.message ?: "Could not open this topic."
                }
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        val chunk = topic?.chunkSize?.coerceAtLeast(5) ?: 20
        val loaded = posts.mapTo(mutableSetOf()) { it.id }
        val next = stream.filterNot { it in loaded }.take(chunk)
        if (next.isEmpty()) {
            loadingMore = false
            return
        }
        try {
            runCatchingUnlessCancelled { Forum.api.postsByIds(topicId, next) }
                .onSuccess { fetched ->
                    posts = (posts + fetched.filter { loaded.add(it.id) })
                        .sortedBy { it.postNumber }
                }
                .onFailure { /* keep what is already on screen; scrolling again retries */ }
        } finally {
            loadingMore = false
        }
    }

    fun updateBookmark(value: Boolean) {
        bookmarked = value
    }

    /** Swaps one post in place, used after a like or an edit. */
    fun replacePost(updated: PostDto) {
        posts = posts.map { if (it.id == updated.id) updated else it }
    }

    fun removePost(postId: Long) {
        posts = posts.filterNot { it.id == postId }
    }
}

@Composable
fun TopicScreen(
    topicId: Long,
    startPostNumber: Int?,
    fallbackTitle: String?,
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenLink: (ForumLink) -> Unit,
    onCompose: (ComposerRequest) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = remember(topicId, refreshKey) { TopicState(topicId, startPostNumber) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val dimens = LocalDimens.current

    var flagTarget by remember { mutableStateOf<PostDto?>(null) }
    var deleteTarget by remember { mutableStateOf<PostDto?>(null) }

    LaunchedEffect(state) { state.load() }

    val shouldLoadMore by remember(state) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.posts.size - 3 && state.posts.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadMore, state) {
        if (shouldLoadMore) state.loadMore()
    }

    val topic = state.topic
    val isPm = topic?.isPrivateMessage == true

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ForumTopBar(
                title = topic?.title ?: fallbackTitle ?: "Topic",
                subtitle = topic?.let { "${it.postsCount} posts" },
                onBack = onBack,
                actions = {
                    if (topic != null) {
                        IconButton(onClick = {
                            shareText(Forum.topicUrl(topic.id, topic.slug))
                        }) {
                            Icon(PretendoIcons.Share, contentDescription = "Share this topic")
                        }
                        if (Forum.auth.isSignedIn) {
                            val bookmarked = state.bookmarked
                            IconButton(onClick = {
                                scope.launch {
                                    // Flip it on screen first, and put it back if the
                                    // forum refuses.
                                    state.updateBookmark(!bookmarked)
                                    runCatchingUnlessCancelled {
                                        Forum.api.setTopicBookmarked(topic.id, !bookmarked)
                                    }.onSuccess {
                                        snackbar.showSnackbar(
                                            if (bookmarked) "Bookmark removed" else "Bookmarked"
                                        )
                                    }.onFailure {
                                        state.updateBookmark(bookmarked)
                                        snackbar.showSnackbar(it.message ?: "That did not work.")
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (bookmarked) {
                                        PretendoIcons.BookmarkFilled
                                    } else {
                                        PretendoIcons.Bookmark
                                    },
                                    contentDescription = if (bookmarked) {
                                        "Remove bookmark"
                                    } else {
                                        "Bookmark this topic"
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (topic != null && !topic.closed) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!Forum.auth.isSignedIn) {
                            onSignIn()
                        } else {
                            onCompose(
                                ComposerRequest.Reply(
                                    topicId = topic.id,
                                    topicTitle = topic.title,
                                    replyToPostNumber = null,
                                    replyToUsername = null,
                                    isPrivateMessage = isPm
                                )
                            )
                        }
                    },
                    icon = { Icon(PretendoIcons.Reply, contentDescription = null) },
                    text = { Text("Reply") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && state.posts.isEmpty() -> LoadingBox()

                state.error != null && state.posts.isEmpty() -> ErrorBox(
                    message = state.error ?: "Something went wrong.",
                    onRetry = { scope.launch { state.load() } }
                )

                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (topic != null) {
                        item(key = "header") {
                            TopicHeader(topic, onOpenCategory, onOpenTag)
                        }
                    }
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            canReply = state.canReply,
                            onLinkClick = onOpenLink,
                            onOpenProfile = onOpenProfile,
                            actions = PostActions(
                                onLike = {
                                    scope.launch {
                                        toggleLike(state, post) { message ->
                                            snackbar.showSnackbar(message)
                                        }
                                    }
                                },
                                onReply = {
                                    if (topic == null) return@PostActions
                                    onCompose(
                                        ComposerRequest.Reply(
                                            topicId = topic.id,
                                            topicTitle = topic.title,
                                            replyToPostNumber = post.postNumber,
                                            replyToUsername = post.username,
                                            isPrivateMessage = isPm
                                        )
                                    )
                                },
                                onQuoteReply = {
                                    if (topic == null) return@PostActions
                                    onCompose(
                                        ComposerRequest.Reply(
                                            topicId = topic.id,
                                            topicTitle = topic.title,
                                            replyToPostNumber = post.postNumber,
                                            replyToUsername = post.username,
                                            quoted = quoteFor(post, topic),
                                            isPrivateMessage = isPm
                                        )
                                    )
                                },
                                onEdit = {
                                    onCompose(
                                        ComposerRequest.Edit(
                                            postId = post.id,
                                            topicTitle = topic?.title ?: "",
                                            existingRaw = post.raw
                                        )
                                    )
                                },
                                onDelete = { deleteTarget = post },
                                onFlag = { flagTarget = post },
                                onCopyLink = {
                                    copyToClipboard(
                                        Forum.topicUrl(topicId, topic?.slug, post.postNumber)
                                    )
                                    scope.launch { snackbar.showSnackbar("Link copied") }
                                },
                                onShare = {
                                    shareText(Forum.topicUrl(topicId, topic?.slug, post.postNumber))
                                }
                            )
                        )
                    }
                    if (state.loadingMore) {
                        item(key = "loading-more") { InlineLoading() }
                    }
                    item(key = "tail") { Spacer(Modifier.size(dimens.sectionSpacing * 3)) }
                }
            }
        }
    }

    flagTarget?.let { post ->
        FlagDialog(
            onDismiss = { flagTarget = null },
            onSubmit = { reason, message ->
                flagTarget = null
                scope.launch {
                    runCatchingUnlessCancelled { Forum.api.flagPost(post.id, reason, message) }
                        .onSuccess { snackbar.showSnackbar("Sent to the staff. Thank you.") }
                        .onFailure { snackbar.showSnackbar(it.message ?: "Could not send that.") }
                }
            }
        )
    }

    deleteTarget?.let { post ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this post?") },
            text = { Text("This cannot be undone from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        runCatchingUnlessCancelled { Forum.api.deletePost(post.id) }
                            .onSuccess {
                                state.removePost(post.id)
                                snackbar.showSnackbar("Post deleted")
                            }
                            .onFailure {
                                snackbar.showSnackbar(it.message ?: "Could not delete that.")
                            }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * The topic's own details, above the first post: title at a readable size, then the
 * category, tags and counts that the website squeezes into a narrow sidebar.
 */
@Composable
private fun TopicHeader(
    topic: TopicDetailDto,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit
) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    val category = Forum.site.byId(topic.categoryId)

    Column(modifier = Modifier.fillMaxWidth().padding(dimens.screenPadding)) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(dimens.itemSpacing))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (category != null) {
                CategoryChip(
                    name = category.name,
                    colorHex = category.color,
                    onClick = { onOpenCategory(category.slug, category.id, category.name) }
                )
            }
            StatLabel(
                icon = PretendoIcons.Eye,
                value = Format.count(topic.views),
                contentDescription = "${topic.views} views"
            )
            StatLabel(
                icon = PretendoIcons.Person,
                value = Format.count(topic.participantCount),
                contentDescription = "${topic.participantCount} people in this topic"
            )
        }

        if (topic.tags.isNotEmpty()) {
            Spacer(Modifier.size(dimens.itemSpacing))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                topic.tags.take(5).forEach { tag ->
                    TagChip(tag = tag, onClick = { onOpenTag(tag) })
                }
            }
        }

        if (topic.closed || topic.archived) {
            Spacer(Modifier.size(dimens.itemSpacing))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PretendoIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "This topic is closed to new replies.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FlagDialog(
    onDismiss: () -> Unit,
    onSubmit: (FlagReason, String) -> Unit
) {
    var reason by remember { mutableStateOf(FlagReason.Inappropriate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tell the staff about this post") },
        text = {
            Column {
                FlagReason.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = reason == option,
                            onClick = { reason = option }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason, "") }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Likes are applied on screen first and rolled back if the forum refuses, so tapping a
 * heart feels immediate on a slow connection.
 */
private suspend fun toggleLike(
    state: TopicState,
    post: PostDto,
    onMessage: suspend (String) -> Unit
) {
    if (!Forum.auth.isSignedIn) {
        onMessage("Sign in to like posts.")
        return
    }
    val liked = post.likedByMe
    val optimistic = post.withLike(!liked)
    state.replacePost(optimistic)

    val result = runCatchingUnlessCancelled {
        if (liked) Forum.api.unlike(post.id) else Forum.api.like(post.id)
    }
    if (result.isFailure) {
        state.replacePost(post)
        onMessage(result.exceptionOrNull()?.message ?: "Could not register that.")
    }
}

private fun PostDto.withLike(liked: Boolean): PostDto {
    val summaries = actionsSummary.toMutableList()
    val index = summaries.indexOfFirst { it.id == PostActionType.LIKE }
    val current = if (index >= 0) summaries[index] else ActionSummaryDto(id = PostActionType.LIKE)
    val updated = current.copy(
        acted = liked,
        count = (current.count + if (liked) 1 else -1).coerceAtLeast(0),
        canAct = !liked,
        canUndo = liked
    )
    if (index >= 0) summaries[index] = updated else summaries.add(updated)
    return copy(actionsSummary = summaries)
}

/** Builds the Markdown quote Discourse expects when replying with a quote. */
private fun quoteFor(post: PostDto, topic: TopicDetailDto): String {
    val body = com.dislopik.pretendo.ui.html.CookedHtml.plainSummary(post.cooked, limit = 1000)
    return buildString {
        append("[quote=\"")
        append(post.username)
        append(", post:")
        append(post.postNumber)
        append(", topic:")
        append(topic.id)
        append("\"]\n")
        append(body)
        append("\n[/quote]\n\n")
    }
}

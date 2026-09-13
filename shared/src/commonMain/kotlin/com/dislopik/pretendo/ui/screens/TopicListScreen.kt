package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.model.TopPeriod
import com.dislopik.pretendo.model.TopicFilter
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.TopicListState
import com.dislopik.pretendo.ui.components.EmptyState
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.ForumTopBar
import com.dislopik.pretendo.ui.components.InlineLoading
import com.dislopik.pretendo.ui.components.LoadingBox
import com.dislopik.pretendo.ui.components.TopicRow

/**
 * The list of topics shown on the Topics tab, with the forum's own filters across the top.
 */
@Composable
fun TopicsTab(
    filter: TopicFilter,
    onFilterChange: (TopicFilter) -> Unit,
    refreshKey: Int,
    signedIn: Boolean,
    onOpenTopic: (Long, String) -> Unit,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var period by remember { mutableStateOf(TopPeriod.Week) }
    val state = remember(filter, period, refreshKey) {
        TopicListState { page -> Forum.api.topics(filter, page, period) }
    }

    LaunchedEffect(state) {
        Forum.site.ensureLoaded()
        state.refresh()
    }

    Column(modifier = modifier.fillMaxSize()) {
        FilterRow(
            selected = filter,
            signedIn = signedIn,
            onSelect = onFilterChange
        )
        if (filter == TopicFilter.Top) {
            PeriodRow(selected = period, onSelect = { period = it })
        }
        TopicListBody(
            state = state,
            emptyTitle = emptyTitleFor(filter),
            emptyMessage = emptyMessageFor(filter, signedIn),
            onOpenTopic = onOpenTopic,
            onOpenCategory = onOpenCategory,
            onOpenTag = onOpenTag,
            onSignIn = if (filter.requiresAuth && !signedIn) onSignIn else null
        )
    }
}

@Composable
private fun FilterRow(
    selected: TopicFilter,
    signedIn: Boolean,
    onSelect: (TopicFilter) -> Unit
) {
    val dimens = LocalDimens.current
    val available = TopicFilter.entries.filter { !it.requiresAuth || signedIn }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPadding, vertical = 8.dp)
    ) {
        available.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

/** Which stretch of time the Top list covers. */
@Composable
private fun PeriodRow(selected: TopPeriod, onSelect: (TopPeriod) -> Unit) {
    val dimens = LocalDimens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPadding, vertical = 4.dp)
    ) {
        TopPeriod.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) }
            )
        }
    }
}

/** A category's topics, opened from the Categories tab or from a link in a post. */
@Composable
fun CategoryTopicsScreen(
    slug: String,
    categoryId: Int,
    name: String,
    onBack: () -> Unit,
    onOpenTopic: (Long, String) -> Unit,
    onOpenTag: (String) -> Unit,
    onNewTopic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = remember(slug, categoryId) {
        TopicListState { page -> Forum.api.categoryTopics(slug, categoryId, page) }
    }
    val category = Forum.site.byId(categoryId)

    LaunchedEffect(state) {
        Forum.site.ensureLoaded()
        state.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ForumTopBar(
                title = name,
                subtitle = category?.descriptionExcerpt?.takeIf { it.isNotBlank() },
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (Forum.auth.isSignedIn) {
                ExtendedFloatingActionButton(
                    onClick = onNewTopic,
                    icon = { Icon(PretendoIcons.Add, contentDescription = null) },
                    text = { Text("New topic") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TopicListBody(
                state = state,
                emptyTitle = "Nothing here yet",
                emptyMessage = "No one has started a topic in $name.",
                onOpenTopic = onOpenTopic,
                onOpenCategory = { _, _, _ -> },
                onOpenTag = onOpenTag,
                onSignIn = null
            )
        }
    }
}

/** Every topic carrying one tag. */
@Composable
fun TagTopicsScreen(
    tag: String,
    onBack: () -> Unit,
    onOpenTopic: (Long, String) -> Unit,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = remember(tag) { TopicListState { page -> Forum.api.tagTopics(tag, page) } }

    LaunchedEffect(state) {
        Forum.site.ensureLoaded()
        state.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = { ForumTopBar(title = tag, subtitle = "Tagged topics", onBack = onBack) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TopicListBody(
                state = state,
                emptyTitle = "No topics",
                emptyMessage = "Nothing is tagged $tag right now.",
                onOpenTopic = onOpenTopic,
                onOpenCategory = onOpenCategory,
                onOpenTag = onOpenTag,
                onSignIn = null
            )
        }
    }
}

/**
 * The list itself, shared by every screen that shows topics.
 *
 * Paging happens when the reader is a few rows from the end rather than at the very
 * bottom, so the next page is usually already there by the time they reach it.
 */
@Composable
fun TopicListBody(
    state: TopicListState,
    emptyTitle: String,
    emptyMessage: String,
    onOpenTopic: (Long, String) -> Unit,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit,
    onSignIn: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(state) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.topics.size - 4 && state.topics.isNotEmpty()
        }
    }

    LaunchedEffect(shouldLoadMore, state) {
        if (shouldLoadMore) state.loadMore()
    }

    when {
        state.loading && state.topics.isEmpty() -> LoadingBox(modifier)

        state.error != null && state.topics.isEmpty() -> {
            if (onSignIn != null) {
                EmptyState(
                    icon = PretendoIcons.Login,
                    title = "Sign in to see this",
                    message = emptyMessage,
                    modifier = modifier,
                    action = {
                        Button(onClick = onSignIn) { Text("Sign in") }
                    }
                )
            } else {
                val retry = rememberRetry(state)
                ErrorBox(
                    message = state.error ?: "Something went wrong.",
                    modifier = modifier,
                    onRetry = retry
                )
            }
        }

        state.isEmpty -> EmptyState(
            icon = PretendoIcons.Categories,
            title = emptyTitle,
            message = emptyMessage,
            modifier = modifier
        )

        else -> LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
            items(state.topics, key = { it.id }) { topic ->
                val category = Forum.site.byId(topic.categoryId)
                TopicRow(
                    topic = topic,
                    category = category,
                    usersById = state.usersById,
                    onClick = { onOpenTopic(topic.id, topic.title) },
                    onCategoryClick = category?.let {
                        { onOpenCategory(it.slug, it.id, it.name) }
                    },
                    onTagClick = onOpenTag
                )
            }
            if (state.loadingMore) {
                item { InlineLoading() }
            }
        }
    }
}

/** Lets a retry button re-run a suspending refresh without owning a scope itself. */
@Composable
private fun rememberRetry(state: TopicListState): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember(state, scope) { { scope.launch { state.refresh() } } }
}

private fun emptyTitleFor(filter: TopicFilter): String = when (filter) {
    TopicFilter.Bookmarks -> "No bookmarks"
    TopicFilter.Unread -> "Nothing unread"
    TopicFilter.New -> "Nothing new"
    else -> "No topics"
}

private fun emptyMessageFor(filter: TopicFilter, signedIn: Boolean): String = when {
    filter.requiresAuth && !signedIn ->
        "This list is about your own reading, so it needs you to be signed in."
    filter == TopicFilter.Bookmarks -> "Topics you bookmark will be kept here."
    filter == TopicFilter.Unread -> "You are up to date with everything you follow."
    filter == TopicFilter.New -> "Nothing has been posted since your last visit."
    else -> "Nothing to show right now."
}

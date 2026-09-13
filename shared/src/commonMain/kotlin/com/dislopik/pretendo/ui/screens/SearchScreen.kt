package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.model.SearchResponse
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.Avatar
import com.dislopik.pretendo.ui.components.CategoryChip
import com.dislopik.pretendo.ui.components.EmptyState
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.InlineLoading
import com.dislopik.pretendo.ui.components.SectionHeader
import kotlinx.coroutines.delay

/**
 * Search across topics, posts and people.
 *
 * Typing runs the search on its own after a short pause, which on a phone saves a trip to
 * the keyboard's search key for every query.
 */
@Composable
fun SearchTab(
    onOpenTopic: (Long, String) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY) {
            results = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        // Wait for a gap in typing before asking the forum.
        delay(450)
        loading = true
        error = null
        runCatchingUnlessCancelled { Forum.api.search(trimmed) }
            .onSuccess { results = it }
            .onFailure {
                error = it.message ?: "Could not search right now."
                results = null
            }
        loading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search the forum") },
            singleLine = true,
            leadingIcon = { Icon(PretendoIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(PretendoIcons.Close, contentDescription = "Clear the search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding, vertical = 8.dp)
        )

        val current = results
        when {
            loading -> InlineLoading()

            error != null -> ErrorBox(error!!)

            query.trim().length < MIN_QUERY -> EmptyState(
                icon = PretendoIcons.Search,
                title = "Find something",
                message = "Type at least $MIN_QUERY letters to search topics, posts and people."
            )

            current == null -> Unit

            current.topics.isEmpty() && current.posts.isEmpty() && current.users.isEmpty() ->
                EmptyState(
                    icon = PretendoIcons.Search,
                    title = "Nothing found",
                    message = "No topics, posts or people match \"${query.trim()}\"."
                )

            else -> SearchResults(current, onOpenTopic, onOpenProfile)
        }
    }
}

@Composable
private fun SearchResults(
    results: SearchResponse,
    onOpenTopic: (Long, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val topicsById = remember(results) { results.topics.associateBy { it.id } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (results.users.isNotEmpty()) {
            item(key = "people-header") { SectionHeader("People") }
            items(results.users, key = { "user-${it.id}" }) { user ->
                UserResultRow(
                    username = user.username,
                    name = user.name,
                    avatarTemplate = user.avatarTemplate,
                    onClick = { onOpenProfile(user.username) }
                )
            }
        }

        if (results.topics.isNotEmpty()) {
            item(key = "topics-header") { SectionHeader("Topics") }
            items(results.topics, key = { "topic-${it.id}" }) { topic ->
                TopicResultRow(
                    title = topic.title,
                    categoryId = topic.categoryId,
                    detail = "${Format.count(topic.postsCount)} posts",
                    onClick = { onOpenTopic(topic.id, topic.title) }
                )
            }
        }

        // Posts come back separately from the topics they belong to.
        val standalonePosts = results.posts.filter { it.topicId !in topicsById.keys }
        if (standalonePosts.isNotEmpty()) {
            item(key = "posts-header") { SectionHeader("Posts") }
            items(standalonePosts, key = { "post-${it.id}" }) { post ->
                PostResultRow(
                    username = post.username,
                    blurb = post.blurb,
                    createdAt = post.createdAt,
                    onClick = { onOpenTopic(post.topicId, "") }
                )
            }
        }
    }
}

@Composable
private fun TopicResultRow(
    title: String,
    categoryId: Int?,
    detail: String,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val category = Forum.site.byId(categoryId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                CategoryChip(name = category.name, colorHex = category.color)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PostResultRow(
    username: String,
    blurb: String,
    createdAt: String?,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        Text(
            text = "$username · ${Format.timestamp(createdAt, settings.absoluteDates)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = blurb,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun UserResultRow(
    username: String,
    name: String?,
    avatarTemplate: String?,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        Avatar(avatarTemplate = avatarTemplate, username = username)
        Spacer(Modifier.width(dimens.itemSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = username, style = MaterialTheme.typography.bodyLarge)
            if (!name.isNullOrBlank()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = PretendoIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private const val MIN_QUERY = 3

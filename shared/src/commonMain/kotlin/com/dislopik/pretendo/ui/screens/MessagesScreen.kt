package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.model.TopicDto
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.TopicListState
import com.dislopik.pretendo.ui.components.Avatar
import com.dislopik.pretendo.ui.components.EmptyState
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.ForumTopBar
import com.dislopik.pretendo.ui.components.InlineLoading
import com.dislopik.pretendo.ui.components.LoadingBox
import kotlinx.coroutines.launch

/**
 * Private messages.
 *
 * A message thread is a topic on the Discourse side, so opening one goes to the same
 * screen a public topic does; only the list needs its own shape, because who a message is
 * with matters more than which category it is in.
 */
@Composable
fun MessagesScreen(
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenMessage: (Long, String) -> Unit,
    onNewMessage: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (!Forum.auth.isSignedIn) {
        Scaffold(
            modifier = modifier,
            topBar = { ForumTopBar(title = "Messages", onBack = onBack) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    icon = PretendoIcons.Mail,
                    title = "Sign in for messages",
                    message = "Private messages need an account.",
                    action = { Button(onClick = onSignIn) { Text("Sign in") } }
                )
            }
        }
        return
    }

    val state = remember(sent, refreshKey) {
        TopicListState { page -> Forum.api.privateMessages(sent = sent, page = page) }
    }

    LaunchedEffect(state) {
        // The inbox endpoints are keyed by username, so the session has to be known first.
        if (Forum.session.username == null) Forum.session.refresh()
        state.refresh()
    }

    Scaffold(
        modifier = modifier,
        topBar = { ForumTopBar(title = "Messages", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewMessage) {
                Icon(PretendoIcons.Edit, contentDescription = "Write a message")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = if (sent) 1 else 0) {
                Tab(
                    selected = !sent,
                    onClick = { sent = false },
                    text = { Text("Inbox") }
                )
                Tab(
                    selected = sent,
                    onClick = { sent = true },
                    text = { Text("Sent") }
                )
            }

            when {
                state.loading && state.topics.isEmpty() -> LoadingBox()

                state.error != null && state.topics.isEmpty() -> ErrorBox(
                    message = state.error ?: "Could not load your messages.",
                    onRetry = { scope.launch { state.refresh() } }
                )

                state.isEmpty -> EmptyState(
                    icon = PretendoIcons.Mail,
                    title = if (sent) "Nothing sent" else "No messages",
                    message = if (sent) {
                        "Messages you send will be listed here."
                    } else {
                        "When someone writes to you, it will land here."
                    }
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.topics, key = { it.id }) { message ->
                        MessageRow(
                            topic = message,
                            onClick = { onOpenMessage(message.id, message.title) }
                        )
                    }
                    if (state.loadingMore) item { InlineLoading() }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(topic: TopicDto, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    val unread = (topic.unreadPosts ?: 0) + (topic.newPosts ?: 0) > 0
    val others = topic.participants.filter { it.username != Forum.session.username }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        val first = others.firstOrNull() ?: topic.participants.firstOrNull()
        Avatar(
            avatarTemplate = first?.avatarTemplate,
            username = first?.username ?: "?"
        )
        Spacer(Modifier.width(dimens.itemSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = others.joinToString(", ") { it.username }.ifBlank { "You" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${topic.postsCount} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Format.timestamp(topic.bumpedAt, settings.absoluteDates),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (unread) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

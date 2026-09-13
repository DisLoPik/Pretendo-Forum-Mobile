package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.model.NotificationDto
import com.dislopik.pretendo.model.NotificationType
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.EmptyState
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.LoadingBox
import kotlinx.coroutines.launch

/**
 * What has happened to the reader since last time.
 *
 * The website shows these in a dropdown that is unusable on a phone. Here each one is a
 * full row that says in plain words what happened, who did it, and where.
 */
@Composable
fun NotificationsTab(
    signedIn: Boolean,
    refreshKey: Int,
    onOpenTopic: (Long, Int?, String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!signedIn) {
        EmptyState(
            icon = PretendoIcons.Bell,
            title = "Sign in for alerts",
            message = "Replies, mentions and messages show up here once you are signed in.",
            modifier = modifier,
            action = { Button(onClick = onSignIn) { Text("Sign in") } }
        )
        return
    }

    val scope = rememberCoroutineScope()
    var notifications by remember(refreshKey) { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var loading by remember(refreshKey) { mutableStateOf(true) }
    var error by remember(refreshKey) { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        try {
            runCatchingUnlessCancelled { Forum.api.notifications() }
                .onSuccess { notifications = it.notifications }
                .onFailure { error = it.message ?: "Could not load your alerts." }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(refreshKey) { reload() }

    when {
        loading && notifications.isEmpty() -> LoadingBox(modifier)

        error != null && notifications.isEmpty() -> ErrorBox(
            message = error!!,
            modifier = modifier,
            onRetry = { scope.launch { reload() } }
        )

        notifications.isEmpty() -> EmptyState(
            icon = PretendoIcons.Bell,
            title = "Nothing new",
            message = "When someone replies to you or mentions you, it will show up here.",
            modifier = modifier
        )

        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            if (notifications.any { !it.read }) {
                item(key = "mark-read") {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatchingUnlessCancelled { Forum.api.markNotificationsRead() }
                                reload()
                                Forum.session.refresh()
                            }
                        }) { Text("Mark all as read") }
                    }
                }
            }
            items(notifications, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        val topicId = notification.topicId
                        val username = notification.data.displayUsername
                            ?: notification.data.username
                        when {
                            topicId != null -> onOpenTopic(
                                topicId,
                                notification.postNumber,
                                notification.data.topicTitle ?: notification.fancyTitle.orEmpty()
                            )
                            username != null -> onOpenProfile(username)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationDto, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    val who = notification.data.displayUsername ?: notification.data.username

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (notification.read) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        Icon(
            imageVector = iconFor(notification.notificationType),
            contentDescription = null,
            tint = if (notification.read) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(dimens.itemSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = describe(notification, who),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold
            )
            val title = notification.data.topicTitle ?: notification.fancyTitle
            if (!title.isNullOrBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = Format.timestamp(notification.createdAt, settings.absoluteDates),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Discourse sends a numeric type; this is the sentence a person would say. */
private fun describe(notification: NotificationDto, who: String?): String {
    val name = who ?: "Someone"
    return when (notification.notificationType) {
        NotificationType.MENTIONED -> "$name mentioned you"
        NotificationType.REPLIED -> "$name replied to you"
        NotificationType.QUOTED -> "$name quoted you"
        NotificationType.EDITED -> "$name edited your post"
        NotificationType.LIKED -> "$name liked your post"
        NotificationType.LIKED_CONSOLIDATED -> {
            val count = notification.data.count
            if (count != null) "$name liked $count of your posts" else "$name liked your posts"
        }
        NotificationType.PRIVATE_MESSAGE -> "$name sent you a message"
        NotificationType.INVITED_TO_PRIVATE_MESSAGE -> "$name invited you to a message"
        NotificationType.INVITED_TO_TOPIC -> "$name invited you to a topic"
        NotificationType.INVITEE_ACCEPTED -> "$name accepted your invitation"
        NotificationType.POSTED -> "$name posted in a topic you follow"
        NotificationType.MOVED_POST -> "$name moved your post"
        NotificationType.LINKED -> "$name linked to your post"
        NotificationType.GRANTED_BADGE ->
            notification.data.badgeName?.let { "You earned the $it badge" } ?: "You earned a badge"
        NotificationType.GROUP_MENTIONED -> "$name mentioned your group"
        NotificationType.GROUP_MESSAGE_SUMMARY -> "New messages for your group"
        NotificationType.WATCHING_FIRST_POST -> "A new topic in something you watch"
        NotificationType.TOPIC_REMINDER -> "A reminder about a topic"
        NotificationType.BOOKMARK_REMINDER -> "A reminder about a bookmark"
        NotificationType.POST_APPROVED -> "Your post was approved"
        NotificationType.REACTION -> "$name reacted to your post"
        NotificationType.CUSTOM -> notification.data.message ?: "Something happened"
        else -> "$name did something"
    }
}

private fun iconFor(type: Int): ImageVector = when (type) {
    NotificationType.LIKED, NotificationType.LIKED_CONSOLIDATED, NotificationType.REACTION ->
        PretendoIcons.Heart
    NotificationType.PRIVATE_MESSAGE, NotificationType.INVITED_TO_PRIVATE_MESSAGE ->
        PretendoIcons.Mail
    NotificationType.REPLIED, NotificationType.QUOTED, NotificationType.POSTED ->
        PretendoIcons.Reply
    NotificationType.GRANTED_BADGE -> PretendoIcons.Star
    NotificationType.MENTIONED, NotificationType.GROUP_MENTIONED -> PretendoIcons.Person
    else -> PretendoIcons.Bell
}

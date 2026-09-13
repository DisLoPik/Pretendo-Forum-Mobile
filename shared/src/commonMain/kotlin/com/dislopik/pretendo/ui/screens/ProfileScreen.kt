package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.model.ProfileTab
import com.dislopik.pretendo.model.UserActionDto
import com.dislopik.pretendo.model.UserActionType
import com.dislopik.pretendo.model.UserDto
import com.dislopik.pretendo.model.UserSummaryDto
import com.dislopik.pretendo.model.staffLabel
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.Avatar
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.ForumTopBar
import com.dislopik.pretendo.ui.components.InlineLoading
import com.dislopik.pretendo.ui.components.LoadingBox
import com.dislopik.pretendo.ui.components.RoleBadge
import com.dislopik.pretendo.ui.html.CookedContent
import com.dislopik.pretendo.ui.html.ForumLink
import com.dislopik.pretendo.ui.html.rememberCookedBlocks
import kotlinx.coroutines.launch

/**
 * A member's page: who they are, what they have done, and a way to message them.
 *
 * The website spreads this across a wide banner with floating cards. Here it reads top to
 * bottom, so nothing needs sideways scrolling to make sense.
 */
@Composable
fun ProfileScreen(
    username: String,
    onBack: () -> Unit,
    onOpenTopic: (Long, Int?, String) -> Unit,
    onOpenLink: (ForumLink) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var user by remember(username) { mutableStateOf<UserDto?>(null) }
    var summary by remember(username) { mutableStateOf<UserSummaryDto?>(null) }
    var loading by remember(username) { mutableStateOf(true) }
    var error by remember(username) { mutableStateOf<String?>(null) }
    var tab by remember(username) { mutableStateOf(ProfileTab.Summary) }

    suspend fun load() {
        loading = true
        error = null
        try {
            runCatchingUnlessCancelled { Forum.api.user(username) }
                .onSuccess { user = it }
                .onFailure { error = it.message ?: "Could not open this profile." }
            summary = runCatchingUnlessCancelled { Forum.api.userSummary(username) }.getOrNull()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(username) { load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            ForumTopBar(
                title = username,
                subtitle = user?.title?.takeIf { it.isNotBlank() },
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading && user == null -> LoadingBox()

                error != null && user == null -> ErrorBox(
                    message = error!!,
                    onRetry = { scope.launch { load() } }
                )

                user != null -> ProfileBody(
                    user = user!!,
                    summary = summary,
                    tab = tab,
                    onTabChange = { tab = it },
                    onOpenTopic = onOpenTopic,
                    onOpenLink = onOpenLink,
                    onSendMessage = onSendMessage
                )
            }
        }
    }
}

@Composable
private fun ProfileBody(
    user: UserDto,
    summary: UserSummaryDto?,
    tab: ProfileTab,
    onTabChange: (ProfileTab) -> Unit,
    onOpenTopic: (Long, Int?, String) -> Unit,
    onOpenLink: (ForumLink) -> Unit,
    onSendMessage: (String) -> Unit
) {
    val dimens = LocalDimens.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            ProfileHeader(user, summary, onSendMessage)
        }

        item(key = "tabs") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = dimens.screenPadding, vertical = 8.dp)
            ) {
                ProfileTab.entries.forEach { option ->
                    FilterChip(
                        selected = option == tab,
                        onClick = { onTabChange(option) },
                        label = { Text(option.label) }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (tab == ProfileTab.Summary) {
            item(key = "summary") {
                SummaryPanel(user, summary, onOpenLink)
            }
        } else {
            item(key = "activity-${tab.name}") {
                ActivityPanel(user.username, tab, onOpenTopic)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: UserDto,
    summary: UserSummaryDto?,
    onSendMessage: (String) -> Unit
) {
    val dimens = LocalDimens.current

    Column(modifier = Modifier.fillMaxWidth().padding(dimens.screenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                avatarTemplate = user.avatarTemplate,
                username = user.username,
                size = 64.dp
            )
            Spacer(Modifier.width(dimens.itemSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val role = staffLabel(user.admin, user.moderator)
                    if (role != null) {
                        Spacer(Modifier.width(8.dp))
                        RoleBadge(role)
                    }
                }
                if (user.name?.isNotBlank() == true) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!user.title.isNullOrBlank()) {
                    Text(
                        text = user.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.size(dimens.itemSpacing))
        Text(
            text = listOfNotNull(
                Format.monthAndYear(user.createdAt).takeIf { it.isNotBlank() }?.let { "Joined $it" },
                Format.relative(user.lastSeenAt).takeIf { it.isNotBlank() }?.let { "Last seen $it" }
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (user.canSendPm && Forum.auth.isSignedIn) {
            Spacer(Modifier.size(dimens.itemSpacing))
            Button(
                onClick = { onSendMessage(user.username) },
                modifier = Modifier.heightIn(min = dimens.minTouchTarget)
            ) {
                Icon(PretendoIcons.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send a message")
            }
        }

        if (summary != null) {
            Spacer(Modifier.size(dimens.sectionSpacing))
            StatsGrid(summary, user)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * The numbers a member's page is really about, two per row so each one keeps a label that
 * fits without truncation.
 */
@Composable
private fun StatsGrid(summary: UserSummaryDto, user: UserDto) {
    val dimens = LocalDimens.current
    val stats = listOf(
        "Topics" to Format.count(summary.topicCount),
        "Posts" to Format.count(summary.postCount),
        "Likes given" to Format.count(summary.likesGiven),
        "Likes received" to Format.count(summary.likesReceived),
        "Days visited" to Format.count(summary.daysVisited),
        "Time read" to Format.readingTime(summary.timeRead),
        "Solutions" to Format.count(summary.solvedCount),
        "Badges" to Format.count(user.badgeCount)
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.itemSpacing)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryPanel(
    user: UserDto,
    summary: UserSummaryDto?,
    onOpenLink: (ForumLink) -> Unit
) {
    val dimens = LocalDimens.current
    val bio = rememberCookedBlocks(user.bioCooked)

    Column(modifier = Modifier.fillMaxWidth().padding(dimens.screenPadding)) {
        if (bio.isNotEmpty()) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(dimens.itemSpacing))
            CookedContent(blocks = bio, onLinkClick = onOpenLink)
        } else {
            Text(
                text = "${user.username} has not written anything about themselves.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }

        if (user.groups.isNotEmpty()) {
            Spacer(Modifier.size(dimens.sectionSpacing))
            Text(
                text = "Groups",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(dimens.itemSpacing))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                user.groups.take(8).forEach { group ->
                    Text(
                        text = group.fullName?.takeIf { it.isNotBlank() } ?: group.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Spacer(Modifier.size(dimens.sectionSpacing))
    }
}

/** Topics, replies and likes, read from Discourse's user actions feed. */
@Composable
private fun ActivityPanel(
    username: String,
    tab: ProfileTab,
    onOpenTopic: (Long, Int?, String) -> Unit
) {
    var actions by remember(username, tab) { mutableStateOf<List<UserActionDto>>(emptyList()) }
    var loading by remember(username, tab) { mutableStateOf(true) }
    var error by remember(username, tab) { mutableStateOf<String?>(null) }

    LaunchedEffect(username, tab) {
        loading = true
        error = null
        val filter = UserActionType.filterFor(tab)
        if (filter == null) {
            loading = false
            return@LaunchedEffect
        }
        runCatchingUnlessCancelled { Forum.api.userActions(username, filter) }
            .onSuccess { actions = it.userActions }
            .onFailure { error = it.message ?: "Could not load this list." }
        loading = false
    }

    when {
        loading -> InlineLoading()
        error != null -> ErrorBox(error!!)
        actions.isEmpty() -> Text(
            text = "Nothing here yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(LocalDimens.current.screenPadding)
        )
        else -> Column {
            actions.forEach { action ->
                ActivityRow(action) {
                    onOpenTopic(action.topicId, action.postNumber, action.title)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(action: UserActionDto, onClick: () -> Unit) {
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
            text = action.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!action.excerpt.isNullOrBlank()) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = com.dislopik.pretendo.ui.html.CookedHtml.plainSummary(action.excerpt, 160),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = Format.timestamp(action.createdAt, settings.absoluteDates),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

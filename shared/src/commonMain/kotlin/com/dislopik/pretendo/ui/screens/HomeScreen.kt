package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.model.TopicFilter
import com.dislopik.pretendo.ui.ComposerRequest
import com.dislopik.pretendo.ui.HomeTab
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.Avatar
import com.dislopik.pretendo.ui.components.CountBadge
import com.dislopik.pretendo.ui.components.ForumTopBar
import com.dislopik.pretendo.ui.html.ForumLink

/**
 * The tabbed shell the app opens on.
 *
 * The website's navigation lives in a hamburger menu and a sidebar, neither of which is
 * reachable one-handed. A bottom bar puts the five places worth going within a thumb's
 * reach, and every one of them is labelled rather than left as a bare icon.
 */
@Composable
fun HomeScreen(
    tab: HomeTab,
    onTabChange: (HomeTab) -> Unit,
    filter: TopicFilter,
    onFilterChange: (TopicFilter) -> Unit,
    refreshKey: Int,
    onOpenTopic: (Long, Int?, String) -> Unit,
    onOpenCategory: (String, Int, String) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenLink: (ForumLink) -> Unit,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onCompose: (ComposerRequest) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signedIn = Forum.auth.isSignedIn
    val user = Forum.session.currentUser

    Scaffold(
        modifier = modifier,
        topBar = {
            if (tab != HomeTab.You) {
                ForumTopBar(
                    title = titleFor(tab),
                    actions = {
                        IconButton(onClick = onOpenAccessibility) {
                            Icon(
                                imageVector = PretendoIcons.TextSize,
                                contentDescription = "Accessibility settings"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(PretendoIcons.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = entry == tab,
                        onClick = { onTabChange(entry) },
                        icon = {
                            Box {
                                Icon(
                                    imageVector = iconFor(entry),
                                    contentDescription = null
                                )
                                if (entry == HomeTab.Notifications) {
                                    val unread = (user?.unreadNotifications ?: 0) +
                                        (user?.unreadPrivateMessages ?: 0)
                                    CountBadge(
                                        count = unread,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-4).dp)
                                    )
                                }
                            }
                        },
                        label = { Text(entry.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == HomeTab.Topics || tab == HomeTab.Categories) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (signedIn) {
                            onCompose(ComposerRequest.NewTopic(null, null))
                        } else {
                            onSignIn()
                        }
                    },
                    icon = { Icon(PretendoIcons.Add, contentDescription = null) },
                    text = { Text("New topic") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                HomeTab.Topics -> TopicsTab(
                    filter = filter,
                    onFilterChange = onFilterChange,
                    refreshKey = refreshKey,
                    signedIn = signedIn,
                    onOpenTopic = { id, title -> onOpenTopic(id, null, title) },
                    onOpenCategory = onOpenCategory,
                    onOpenTag = onOpenTag,
                    onSignIn = onSignIn
                )

                HomeTab.Categories -> CategoriesTab(onOpenCategory = onOpenCategory)

                HomeTab.Search -> SearchTab(
                    onOpenTopic = { id, title -> onOpenTopic(id, null, title) },
                    onOpenProfile = onOpenProfile
                )

                HomeTab.Notifications -> NotificationsTab(
                    signedIn = signedIn,
                    refreshKey = refreshKey,
                    onOpenTopic = onOpenTopic,
                    onOpenProfile = onOpenProfile,
                    onSignIn = onSignIn
                )

                HomeTab.You -> YouTab(
                    onOpenProfile = onOpenProfile,
                    onOpenMessages = onOpenMessages,
                    onOpenSettings = onOpenSettings,
                    onOpenAccessibility = onOpenAccessibility,
                    onSignIn = onSignIn
                )
            }
        }
    }
}

/** The reader's own corner: their profile, their messages, and the settings. */
@Composable
private fun YouTab(
    onOpenProfile: (String) -> Unit,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onSignIn: () -> Unit
) {
    val dimens = LocalDimens.current
    val user = Forum.session.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (user != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile(user.username) }
                    .padding(dimens.screenPadding)
            ) {
                Avatar(
                    avatarTemplate = user.avatarTemplate,
                    username = user.username,
                    size = 56.dp
                )
                Spacer(Modifier.width(dimens.itemSpacing))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = PretendoIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.padding(dimens.screenPadding)) {
                Text(
                    text = "Reading as a guest",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "Sign in to post, reply, like and send messages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (user != null) {
            YouRow(
                icon = PretendoIcons.Mail,
                title = "Messages",
                badge = user.unreadPrivateMessages,
                onClick = onOpenMessages
            )
        } else {
            YouRow(icon = PretendoIcons.Login, title = "Sign in", onClick = onSignIn)
        }

        YouRow(
            icon = PretendoIcons.TextSize,
            title = "Accessibility",
            onClick = onOpenAccessibility
        )
        YouRow(icon = PretendoIcons.Settings, title = "Settings", onClick = onOpenSettings)

        Spacer(Modifier.size(dimens.sectionSpacing * 2))
    }
}

@Composable
private fun YouRow(
    icon: ImageVector,
    title: String,
    badge: Int = 0,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(dimens.itemSpacing))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        CountBadge(count = badge)
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = PretendoIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun titleFor(tab: HomeTab): String = when (tab) {
    HomeTab.Topics -> "Pretendo Forum"
    HomeTab.Categories -> "Categories"
    HomeTab.Search -> "Search"
    HomeTab.Notifications -> "Alerts"
    HomeTab.You -> "You"
}

private fun iconFor(tab: HomeTab): ImageVector = when (tab) {
    HomeTab.Topics -> PretendoIcons.Home
    HomeTab.Categories -> PretendoIcons.Categories
    HomeTab.Search -> PretendoIcons.Search
    HomeTab.Notifications -> PretendoIcons.Bell
    HomeTab.You -> PretendoIcons.Person
}

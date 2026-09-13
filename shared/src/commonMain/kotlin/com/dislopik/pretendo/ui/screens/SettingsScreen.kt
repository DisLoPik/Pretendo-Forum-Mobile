package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.getPlatform
import com.dislopik.pretendo.data.openInBrowser
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.Avatar
import com.dislopik.pretendo.ui.components.ForumTopBar

/** The settings hub: the account, the accessibility page, and everything about the app. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val user = Forum.session.currentUser
    var confirmingSignOut by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { ForumTopBar(title = "Settings", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (user != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(user.username) }
                        .heightIn(min = dimens.minTouchTarget)
                        .padding(dimens.screenPadding)
                ) {
                    Avatar(
                        avatarTemplate = user.avatarTemplate,
                        username = user.username,
                        size = 48.dp
                    )
                    Spacer(Modifier.width(dimens.itemSpacing))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Signed in as @${user.username}",
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
                SettingsRow(
                    icon = PretendoIcons.Login,
                    title = "Sign in",
                    description = "Needed to post, reply, like and send messages.",
                    onClick = onSignIn
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsRow(
                icon = PretendoIcons.TextSize,
                title = "Accessibility",
                description = "Text size, typeface, colours, spacing and motion.",
                onClick = onOpenAccessibility
            )

            SettingsRow(
                icon = PretendoIcons.Settings,
                title = "Account settings on the website",
                description = "Notification preferences, your profile and your password.",
                onClick = { openInBrowser("${Forum.BASE_URL}/my/preferences") }
            )

            SettingsRow(
                icon = PretendoIcons.Info,
                title = "About this app",
                description = "What it is, and what it can and cannot do.",
                onClick = onOpenAbout
            )

            if (user != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = PretendoIcons.Logout,
                    title = "Sign out",
                    description = "Removes this app's key from the phone.",
                    onClick = { confirmingSignOut = true },
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.size(dimens.sectionSpacing * 2))
        }
    }

    if (confirmingSignOut) {
        AlertDialog(
            onDismissRequest = { confirmingSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "You can keep reading the forum without an account. Your accessibility " +
                        "settings stay as they are."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingSignOut = false
                    onSignOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingSignOut = false }) { Text("Cancel") }
            }
        )
    }
}

/** What the app is, and an honest note about the parts that still live on the website. */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current

    Scaffold(
        modifier = modifier,
        topBar = { ForumTopBar(title = "About", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            Text(
                text = "Pretendo Forum",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "A reader and writer for forum.pretendo.network, laid out for a phone " +
                    "screen instead of a desktop one.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.size(dimens.itemSpacing))
            Text("What works here", style = MaterialTheme.typography.titleSmall)
            Bullets(
                listOf(
                    "Reading every category, topic and post",
                    "Starting topics, replying, quoting and editing",
                    "Likes, bookmarks and flagging posts for staff",
                    "Private messages, in and out",
                    "Profiles, activity and search",
                    "Alerts for replies, mentions and messages"
                )
            )

            Spacer(Modifier.size(dimens.itemSpacing))
            Text("What still needs the website", style = MaterialTheme.typography.titleSmall)
            Bullets(
                listOf(
                    "Voting in polls",
                    "Uploading pictures to a post",
                    "Changing your profile, avatar and notification preferences",
                    "Moderation tools"
                )
            )
            TextButton(onClick = { openInBrowser(Forum.BASE_URL) }) {
                Text("Open the forum in a browser")
            }

            Spacer(Modifier.size(dimens.itemSpacing))
            Text("Signing in", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "The forum uses Pretendo Network accounts, so you sign in with the " +
                    "same username and password as on pretendo.network. The password is sent " +
                    "there to sign in and is not kept on this phone; signing out clears the " +
                    "session straight away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.size(dimens.itemSpacing))
            Text("Your settings", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Everything on the Accessibility page is stored on this phone. The " +
                    "forum has nowhere to keep it, so it does not travel with your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.size(dimens.sectionSpacing))
            Text(
                text = "This is a community app. It is not made by Pretendo Network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Running on ${getPlatform().name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(dimens.sectionSpacing))
        }
    }
}

@Composable
private fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(dimens.itemSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = tint)
            Spacer(Modifier.size(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = PretendoIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

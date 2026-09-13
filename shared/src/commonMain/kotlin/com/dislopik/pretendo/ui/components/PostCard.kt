package com.dislopik.pretendo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.model.PostDto
import com.dislopik.pretendo.model.canLike
import com.dislopik.pretendo.model.canUnlike
import com.dislopik.pretendo.model.isSmallAction
import com.dislopik.pretendo.model.likeCount
import com.dislopik.pretendo.model.likedByMe
import com.dislopik.pretendo.model.staffLabel
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.html.CookedContent
import com.dislopik.pretendo.ui.html.ForumLink
import com.dislopik.pretendo.ui.html.rememberCookedBlocks

/** What a reader is allowed to do with a post, decided by the screen that owns it. */
data class PostActions(
    val onLike: () -> Unit = {},
    val onReply: () -> Unit = {},
    val onQuoteReply: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onFlag: () -> Unit = {},
    val onCopyLink: () -> Unit = {},
    val onShare: () -> Unit = {}
)

/**
 * One post in a thread.
 *
 * The web forum puts the author in a column beside the text, which on a phone leaves the
 * text about half a screen wide. Here the author is a compact header line and the body
 * gets the whole width, which is most of what makes long threads readable on a phone.
 */
@Composable
fun PostCard(
    post: PostDto,
    canReply: Boolean,
    actions: PostActions,
    onLinkClick: (ForumLink) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current

    if (post.isSmallAction) {
        SmallActionRow(post)
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(dimens.screenPadding)) {
        PostHeader(post, onOpenProfile)

        if (post.replyToPostNumber != null) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = "In reply to post #${post.replyToPostNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.size(dimens.itemSpacing))

        if (post.deletedAt != null || post.userDeleted) {
            Text(
                text = "This post was removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (post.hidden) {
            Text(
                text = "This post is hidden because it was flagged.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            PostBody(post, onLinkClick)
        }

        if (post.acceptedAnswer) {
            Spacer(Modifier.size(dimens.itemSpacing))
            SolvedMarker()
        }

        if (post.reactions.isNotEmpty()) {
            Spacer(Modifier.size(dimens.itemSpacing))
            ReactionRow(post)
        }

        Spacer(Modifier.size(dimens.itemSpacing / 2))
        PostFooter(post, canReply, actions)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PostHeader(post: PostDto, onOpenProfile: (String) -> Unit) {
    val settings = LocalSettings.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Avatar(
            avatarTemplate = post.avatarTemplate,
            username = post.username,
            onClick = { onOpenProfile(post.username) }
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.name?.takeIf { it.isNotBlank() } ?: post.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onOpenProfile(post.username) }
                )
                val role = staffLabel(post.admin, post.moderator)
                if (role != null) {
                    Spacer(Modifier.width(6.dp))
                    RoleBadge(role)
                }
            }
            val subtitle = listOfNotNull(
                post.userTitle?.takeIf { it.isNotBlank() },
                Format.timestamp(post.createdAt, settings.absoluteDates)
            ).joinToString(" · ")
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "#${post.postNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The post body, collapsed when the reader has asked for shorter posts.
 *
 * The cap is worked out from the reader's own line height, so "collapse long posts" means
 * the same number of readable lines whatever the text size is set to.
 */
@Composable
private fun PostBody(post: PostDto, onLinkClick: (ForumLink) -> Unit) {
    val settings = LocalSettings.current
    val blocks = rememberCookedBlocks(post.cooked)
    val collapsedLines = settings.postLength.collapsedLines

    if (collapsedLines <= 0) {
        CookedContent(blocks = blocks, onLinkClick = onLinkClick)
        return
    }

    var expanded by remember(post.id) { mutableStateOf(false) }
    val lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
    val maxHeight = with(LocalDensity.current) {
        (lineHeight.toDp()) * collapsedLines
    }

    Column {
        Box(
            modifier = if (expanded) {
                Modifier
            } else {
                Modifier.heightIn(max = maxHeight).clipToBounds()
            }
        ) {
            CookedContent(blocks = blocks, onLinkClick = onLinkClick)
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Show more")
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) PretendoIcons.ChevronUp else PretendoIcons.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SolvedMarker() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = PretendoIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Marked as the solution",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/** The forum runs discourse-reactions, so a post can carry emoji as well as likes. */
@Composable
private fun ReactionRow(post: PostDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        post.reactions.take(6).forEach { reaction ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = ":${reaction.id}: ${reaction.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PostFooter(post: PostDto, canReply: Boolean, actions: PostActions) {
    val dimens = LocalDimens.current
    var menuOpen by remember { mutableStateOf(false) }
    val liked = post.likedByMe

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (post.canLike || post.canUnlike || post.likeCount > 0) {
            TextButton(
                onClick = actions.onLike,
                enabled = post.canLike || post.canUnlike
            ) {
                Icon(
                    imageVector = if (liked) PretendoIcons.HeartFilled else PretendoIcons.Heart,
                    contentDescription = if (liked) "Remove like" else "Like",
                    tint = if (liked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
                if (post.likeCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Format.count(post.likeCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (canReply) {
            TextButton(onClick = actions.onReply) {
                Icon(
                    imageVector = PretendoIcons.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Reply", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.weight(1f))

        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(dimens.minTouchTarget)
            ) {
                Icon(
                    imageVector = PretendoIcons.MoreVert,
                    contentDescription = "More actions for this post",
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (canReply) {
                    DropdownMenuItem(
                        text = { Text("Quote reply") },
                        onClick = { menuOpen = false; actions.onQuoteReply() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy link") },
                    onClick = { menuOpen = false; actions.onCopyLink() }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { menuOpen = false; actions.onShare() }
                )
                if (post.canEdit) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; actions.onEdit() }
                    )
                }
                if (post.canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; actions.onDelete() }
                    )
                }
                if (!post.yours) {
                    DropdownMenuItem(
                        text = { Text("Flag for staff") },
                        onClick = { menuOpen = false; actions.onFlag() }
                    )
                }
            }
        }
    }
}

/** Discourse's "topic was closed", "title changed" entries: quiet, one-line notes. */
@Composable
private fun SmallActionRow(post: PostDto) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding, vertical = dimens.itemSpacing)
    ) {
        Icon(
            imageVector = PretendoIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${post.username} · ${Format.timestamp(post.createdAt, settings.absoluteDates)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

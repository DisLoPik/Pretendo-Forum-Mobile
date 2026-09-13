package com.dislopik.pretendo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.model.CategoryDto
import com.dislopik.pretendo.model.TopicDto
import com.dislopik.pretendo.model.UserBriefDto
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.html.CookedHtml

/**
 * One topic in a list.
 *
 * The website puts the title, category, poster avatars, reply count, view count and last
 * activity in a single wide table row, which on a phone collapses into an unreadable
 * scrum. Here the same information is stacked: the title gets the full width and the
 * largest text, and everything else drops to a quieter line underneath.
 */
@Composable
fun TopicRow(
    topic: TopicDto,
    category: CategoryDto?,
    usersById: Map<Long, UserBriefDto>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCategoryClick: (() -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    showExcerpt: Boolean = true
) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    val unread = (topic.unreadPosts ?: 0) + (topic.newPosts ?: 0) > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.cardPadding)
    ) {
        // Status markers first, so a closed or pinned topic is obvious before the title.
        if (topic.pinned || topic.closed || topic.archived || topic.hasAcceptedAnswer) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                if (topic.pinned) StatusMark(PretendoIcons.Pin, "Pinned")
                if (topic.closed || topic.archived) StatusMark(PretendoIcons.Lock, "Closed")
                if (topic.hasAcceptedAnswer) {
                    StatusMark(
                        PretendoIcons.Check,
                        "Solved",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        if (showExcerpt && !topic.excerpt.isNullOrBlank()) {
            Spacer(Modifier.size(4.dp))
            val excerpt = remember(topic.excerpt) { CookedHtml.plainSummary(topic.excerpt, limit = 180) }
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.size(dimens.itemSpacing / 2))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            if (category != null) {
                CategoryChip(
                    name = category.name,
                    colorHex = category.color,
                    onClick = onCategoryClick
                )
            }
            StatLabel(
                icon = PretendoIcons.Comment,
                value = Format.count(topic.postsCount - 1),
                contentDescription = "${topic.postsCount - 1} replies"
            )
            StatLabel(
                icon = PretendoIcons.Eye,
                value = Format.count(topic.views),
                contentDescription = "${topic.views} views"
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Format.timestamp(topic.bumpedAt ?: topic.createdAt, settings.absoluteDates),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (topic.tags.isNotEmpty()) {
            Spacer(Modifier.size(dimens.itemSpacing / 2))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                topic.tags.take(4).forEach { tag ->
                    TagChip(tag = tag, onClick = onTagClick?.let { click -> { click(tag) } })
                }
            }
        }

        // Who is in the conversation, which the excerpt alone does not say.
        val posters = topic.posters.mapNotNull { usersById[it.userId] }
        if (posters.isNotEmpty() && settings.showAvatars && settings.showImages) {
            Spacer(Modifier.size(dimens.itemSpacing / 2))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                posters.take(5).forEach { poster ->
                    Avatar(
                        avatarTemplate = poster.avatarTemplate,
                        username = poster.username,
                        size = 20.dp
                    )
                }
                if (unread) {
                    Spacer(Modifier.weight(1f))
                    UnreadPill(count = (topic.unreadPosts ?: 0) + (topic.newPosts ?: 0))
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusMark(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun UnreadPill(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = "$count new",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

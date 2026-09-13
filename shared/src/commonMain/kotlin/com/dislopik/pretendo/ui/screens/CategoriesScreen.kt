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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Format
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.model.CategoryDto
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.categoryColor
import com.dislopik.pretendo.ui.components.ErrorBox
import com.dislopik.pretendo.ui.components.LoadingBox
import com.dislopik.pretendo.ui.html.CookedHtml

/**
 * The forum's categories, with their subcategories nested underneath.
 *
 * The website lays this out as a grid of boxes that turns into a single tall column of
 * near-identical cards on a phone. Here each category is one row, and subcategories are
 * indented under their parent so the shape of the forum is visible at a glance.
 */
@Composable
fun CategoriesTab(
    onOpenCategory: (String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var loading by remember { mutableStateOf(Forum.site.categories.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Forum.site.categories.isEmpty()) {
            loading = true
            runCatchingUnlessCancelled { Forum.site.ensureLoaded() }
                .onFailure { error = it.message ?: "Could not load the categories." }
            if (Forum.site.categories.isEmpty() && error == null) {
                error = "Could not load the categories."
            }
            loading = false
        }
    }

    val grouped = Forum.site.grouped()

    when {
        loading -> LoadingBox(modifier)
        error != null && grouped.isEmpty() -> ErrorBox(error!!, modifier)
        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            grouped.forEach { (parent, children) ->
                item(key = "cat-${parent.id}") {
                    CategoryRow(
                        category = parent,
                        indented = false,
                        onClick = { onOpenCategory(parent.slug, parent.id, parent.name) }
                    )
                }
                for (child in children) {
                    item(key = "cat-${child.id}") {
                        CategoryRow(
                            category = child,
                            indented = true,
                            onClick = { onOpenCategory(child.slug, child.id, child.name) }
                        )
                    }
                }
                item(key = "divider-${parent.id}") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryDto,
    indented: Boolean,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val description = remember(category.description, category.descriptionExcerpt) {
        category.descriptionExcerpt?.takeIf { it.isNotBlank() }
            ?: CookedHtml.plainSummary(category.description, limit = 140).takeIf { it.isNotBlank() }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = dimens.minTouchTarget)
            .padding(
                start = dimens.screenPadding + if (indented) 20.dp else 0.dp,
                end = dimens.screenPadding,
                top = dimens.cardPadding,
                bottom = dimens.cardPadding
            )
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .heightIn(min = if (indented) 26.dp else 34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(categoryColor(category.color))
        )
        Spacer(Modifier.width(dimens.itemSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = if (indented) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (indented) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (description != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = "${Format.count(category.topicCount)} topics",
                style = MaterialTheme.typography.labelMedium,
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

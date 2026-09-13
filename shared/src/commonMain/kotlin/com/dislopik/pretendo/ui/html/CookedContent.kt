package com.dislopik.pretendo.ui.html

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dislopik.pretendo.data.openInBrowser
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.LocalSettings

/** Parses a post body once per body, not once per recomposition. */
@Composable
fun rememberCookedBlocks(cooked: String?): List<Block> =
    remember(cooked) { CookedHtml.parse(cooked) }

/**
 * Draws a parsed post body.
 *
 * Everything is real Compose text, so the accessibility settings reach every word: the
 * text scale, the typeface, the line spacing and the bold setting all come from the theme
 * rather than from the forum's stylesheet.
 */
@Composable
fun CookedContent(
    blocks: List<Block>,
    onLinkClick: (ForumLink) -> Unit,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
    ) {
        blocks.forEach { block -> BlockContent(block, onLinkClick, baseStyle) }
    }
}

@Composable
private fun BlockContent(
    block: Block,
    onLinkClick: (ForumLink) -> Unit,
    baseStyle: TextStyle
) {
    when (block) {
        is Block.Paragraph -> InlineContentText(block.content, onLinkClick, baseStyle)

        is Block.Heading -> InlineContentText(
            text = block.content,
            onLinkClick = onLinkClick,
            style = headingStyle(block.level)
        )

        is Block.Quote -> QuoteBlock(block, onLinkClick, baseStyle)
        is Block.Code -> CodeBlock(block)
        is Block.Bullets -> BulletsBlock(block, onLinkClick, baseStyle)
        is Block.Picture -> PictureBlock(block)
        Block.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        is Block.Table -> TableBlock(block, onLinkClick, baseStyle)
        is Block.Expandable -> ExpandableBlock(block, onLinkClick, baseStyle)
        is Block.LinkPreview -> LinkPreviewBlock(block, onLinkClick)
        is Block.Video -> VideoBlock(block)
        is Block.Poll -> PollBlock(block, baseStyle)
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineSmall
    2 -> MaterialTheme.typography.titleLarge
    3 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}.copy(fontWeight = FontWeight.Bold)


/**
 * Turns parsed spans into an [AnnotatedString], wiring links to [onLinkClick] and pulling
 * Discourse's image-based emoji in as inline content.
 */
@Composable
fun InlineContentText(
    text: InlineText,
    onLinkClick: (ForumLink) -> Unit,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified
) {
    if (text.spans.isEmpty()) return
    val settings = LocalSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val inlineContent = remember(text, settings.showImages) { mutableMapOf<String, InlineTextContent>() }

    val annotated = remember(text, settings.underlineLinks, settings.showImages, colorScheme) {
        inlineContent.clear()
        buildAnnotatedString {
            text.spans.forEachIndexed { index, span ->
                val emojiUrl = span.emojiUrl
                if (emojiUrl != null && settings.showImages) {
                    val id = "emoji-$index"
                    inlineContent[id] = InlineTextContent(
                        androidx.compose.ui.text.Placeholder(
                            width = 1.25.em,
                            height = 1.25.em,
                            placeholderVerticalAlign =
                                androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        AsyncImage(
                            model = emojiUrl,
                            contentDescription = span.text,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    appendInlineContent(id, span.text)
                    return@forEachIndexed
                }

                val spanStyle = span.toSpanStyle(colorScheme.primary, settings.underlineLinks)
                val link = span.link
                if (link != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "link-$index",
                            linkInteractionListener = { onLinkClick(link) }
                        )
                    ) {
                        pushStyle(spanStyle)
                        append(span.text)
                        pop()
                    }
                } else {
                    pushStyle(spanStyle)
                    append(span.text)
                    pop()
                }
            }
        }
    }

    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        inlineContent = inlineContent,
        modifier = modifier
    )
}

private fun InlineSpan.toSpanStyle(linkColor: Color, underlineLinks: Boolean): SpanStyle {
    val decorations = buildList {
        if (strike) add(TextDecoration.LineThrough)
        if (link != null && underlineLinks) add(TextDecoration.Underline)
    }
    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontFamily = if (code) FontFamily.Monospace else null,
        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations),
        color = if (link != null) linkColor else Color.Unspecified,
        background = if (highlight) Color(0x40FFD966) else Color.Unspecified,
        baselineShift = when {
            superscript -> BaselineShift.Superscript
            subscript -> BaselineShift.Subscript
            else -> null
        },
        fontSize = if (superscript || subscript) 0.8.em else androidx.compose.ui.unit.TextUnit.Unspecified
    )
}


@Composable
private fun QuoteBlock(
    block: Block.Quote,
    onLinkClick: (ForumLink) -> Unit,
    baseStyle: TextStyle
) {
    val settings = LocalSettings.current
    val dimens = LocalDimens.current
    var expanded by remember(block) { mutableStateOf(settings.showQuotesExpanded || block.author == null) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 32.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                if (block.author != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    ) {
                        if (block.avatarUrl != null && settings.showAvatars && settings.showImages) {
                            AsyncImage(
                                model = block.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = block.author,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) PretendoIcons.ChevronUp else PretendoIcons.ChevronDown,
                            contentDescription = if (expanded) "Hide quote" else "Show quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expanded) Spacer(Modifier.size(dimens.itemSpacing))
                }
                if (expanded) {
                    CookedContent(
                        blocks = block.blocks,
                        onLinkClick = onLinkClick,
                        baseStyle = baseStyle
                    )
                } else {
                    Text(
                        text = quotePreview(block),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun quotePreview(block: Block.Quote): String {
    val first = block.blocks.firstOrNull()
    val text = when (first) {
        is Block.Paragraph -> first.content.plainText
        is Block.Heading -> first.content.plainText
        else -> ""
    }.trim()
    return if (text.length <= 80) text else text.take(80).trimEnd() + "…"
}

@Composable
private fun CodeBlock(block: Block.Code) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            if (block.language != null) {
                Text(
                    text = block.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
            }
            // Code is the one thing that must not wrap, so it scrolls sideways on its own.
            SelectionContainer {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun BulletsBlock(
    block: Block.Bullets,
    onLinkClick: (ForumLink) -> Unit,
    baseStyle: TextStyle
) {
    val dimens = LocalDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing / 2)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (block.ordered) "${block.start + index}." else "•",
                    style = baseStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(if (block.ordered) 28.dp else 18.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing / 2),
                    modifier = Modifier.weight(1f)
                ) {
                    item.forEach { BlockContent(it, onLinkClick, baseStyle) }
                }
            }
        }
    }
}

@Composable
private fun PictureBlock(block: Block.Picture) {
    val settings = LocalSettings.current
    val dimens = LocalDimens.current
    var revealed by remember(block.url) { mutableStateOf(settings.showImages) }

    if (!revealed) {
        HiddenMediaCard(
            label = block.alt?.takeIf { it.isNotBlank() } ?: "Image",
            onReveal = { revealed = true }
        )
        return
    }

    AsyncImage(
        model = block.url,
        contentDescription = block.alt,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .clip(RoundedCornerShape(dimens.corner))
            .clickable { openInBrowser(block.url) }
    )
}

@Composable
private fun HiddenMediaCard(label: String, onReveal: () -> Unit) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onReveal)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.cardPadding)
        ) {
            Icon(
                imageVector = PretendoIcons.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(dimens.itemSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Images are off. Tap to show this one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TableBlock(
    block: Block.Table,
    onLinkClick: (ForumLink) -> Unit,
    baseStyle: TextStyle
) {
    val dimens = LocalDimens.current
    // A table cannot be reflowed for a narrow screen without losing its meaning, so it
    // keeps its shape and scrolls instead.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        if (block.header.isNotEmpty()) {
            Row {
                block.header.forEach { cell ->
                    Box(modifier = Modifier.width(160.dp).padding(dimens.itemSpacing / 2)) {
                        InlineContentText(
                            text = cell,
                            onLinkClick = onLinkClick,
                            style = baseStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        block.rows.forEach { row ->
            Row {
                row.forEach { cell ->
                    Box(modifier = Modifier.width(160.dp).padding(dimens.itemSpacing / 2)) {
                        InlineContentText(cell, onLinkClick, baseStyle)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ExpandableBlock(
    block: Block.Expandable,
    onLinkClick: (ForumLink) -> Unit,
    baseStyle: TextStyle
) {
    val dimens = LocalDimens.current
    var expanded by remember(block) { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Text(
                    text = block.summary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) PretendoIcons.ChevronUp else PretendoIcons.ChevronDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (expanded) {
                Spacer(Modifier.size(dimens.itemSpacing))
                CookedContent(block.blocks, onLinkClick, baseStyle = baseStyle)
            }
        }
    }
}

@Composable
private fun LinkPreviewBlock(block: Block.LinkPreview, onLinkClick: (ForumLink) -> Unit) {
    val settings = LocalSettings.current
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(dimens.corner)
            )
            .clickable {
                ForumLinks.resolve(block.url)?.let(onLinkClick) ?: openInBrowser(block.url)
            }
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            if (block.imageUrl != null && settings.showImages) {
                AsyncImage(
                    model = block.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(dimens.corner))
                )
                Spacer(Modifier.size(dimens.itemSpacing))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = PretendoIcons.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (block.description != null) {
                Text(
                    text = block.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            if (block.siteName != null) {
                Text(
                    text = block.siteName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VideoBlock(block: Block.Video) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth().clickable { openInBrowser(block.url) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.cardPadding)
        ) {
            Icon(
                imageVector = PretendoIcons.Play,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(dimens.itemSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title ?: "Play video",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Opens in your browser",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PollBlock(block: Block.Poll, baseStyle: TextStyle) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PretendoIcons.Categories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(dimens.itemSpacing))
                Text(
                    text = block.title ?: "Poll",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.size(dimens.itemSpacing))
            block.options.forEach { option ->
                Text("• $option", style = baseStyle)
            }
            Text(
                text = "Voting is on the website for now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

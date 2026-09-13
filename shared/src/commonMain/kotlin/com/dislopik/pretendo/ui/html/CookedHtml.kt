package com.dislopik.pretendo.ui.html

import com.dislopik.pretendo.data.Forum
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

/**
 * Discourse renders every post to HTML on the server and sends it as `cooked`.
 *
 * A phone cannot usefully show that HTML as a web page: the forum's stylesheet assumes a
 * desktop width, and a WebView per post would cost a fortune in memory and break text
 * scaling. Instead this turns the HTML into a small list of blocks, which the app then
 * lays out with real Compose text so that every accessibility setting applies to it.
 *
 * Parsing is deliberately kept free of Compose types so it can be tested on its own.
 */
sealed interface Block {
    data class Paragraph(val content: InlineText) : Block
    data class Heading(val level: Int, val content: InlineText) : Block

    /** A `<blockquote>`, with the "quoting so-and-so" header when Discourse supplied one. */
    data class Quote(
        val author: String?,
        val avatarUrl: String?,
        val topicId: Long?,
        val postNumber: Int?,
        val blocks: List<Block>
    ) : Block

    data class Code(val text: String, val language: String?) : Block
    data class Bullets(val ordered: Boolean, val start: Int, val items: List<List<Block>>) : Block
    data class Picture(val url: String, val alt: String?, val width: Int?, val height: Int?) : Block
    data object Rule : Block
    data class Table(val header: List<InlineText>, val rows: List<List<InlineText>>) : Block

    /** `<details>`, which the forum uses for spoilers and long asides. */
    data class Expandable(val summary: String, val blocks: List<Block>) : Block

    /** A onebox: Discourse's rich preview of a linked page. */
    data class LinkPreview(
        val url: String,
        val title: String,
        val description: String?,
        val imageUrl: String?,
        val siteName: String?
    ) : Block

    /** An embedded video, shown as a tappable thumbnail rather than an inline player. */
    data class Video(val url: String, val thumbnailUrl: String?, val title: String?) : Block

    data class Poll(val title: String?, val options: List<String>) : Block
}

/** A run of text sharing one set of styles. */
data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val highlight: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    val link: ForumLink? = null,
    /** Set for Discourse's image-based emoji; [text] is then the `:shortcode:`. */
    val emojiUrl: String? = null
)

data class InlineText(val spans: List<InlineSpan>) {
    val plainText: String get() = spans.joinToString("") { it.text }
    val isBlank: Boolean get() = spans.all { it.emojiUrl == null && it.text.isBlank() }

    companion object {
        val Empty = InlineText(emptyList())
        fun of(text: String) = InlineText(listOf(InlineSpan(text)))
    }
}

object CookedHtml {

    /** Parses a post body. Returns an empty list for a blank or unparseable body. */
    fun parse(cooked: String?): List<Block> {
        if (cooked.isNullOrBlank()) return emptyList()
        val body = runCatching { Ksoup.parseBodyFragment(cooked).body() }.getOrNull() ?: return emptyList()
        return parseChildren(body).ifEmpty {
            // Nothing recognised: fall back to the text so a post is never silently empty.
            val text = body.text().trim()
            if (text.isEmpty()) emptyList() else listOf(Block.Paragraph(InlineText.of(text)))
        }
    }

    /** A one-line summary for a list row, with markup and quotes stripped out. */
    fun plainSummary(cooked: String?, limit: Int = 240): String {
        if (cooked.isNullOrBlank()) return ""
        val doc = runCatching { Ksoup.parseBodyFragment(cooked) }.getOrNull() ?: return ""
        doc.select("aside, blockquote, .poll, pre").forEach { it.remove() }
        val text = doc.body().text().replace(Regex("\\s+"), " ").trim()
        return if (text.length <= limit) text else text.take(limit).trimEnd() + "…"
    }


    private fun parseChildren(parent: Element): List<Block> {
        val blocks = mutableListOf<Block>()
        // Bare text between block elements still needs to be shown.
        val loose = mutableListOf<InlineSpan>()

        fun flushLoose() {
            if (loose.isNotEmpty()) {
                val content = InlineText(normalise(loose))
                if (!content.isBlank) blocks += Block.Paragraph(content)
                loose.clear()
            }
        }

        for (node in parent.childNodes()) {
            when (node) {
                is TextNode -> loose += InlineSpan(collapse(node.getWholeText()))
                is Element -> {
                    val parsed = parseElement(node)
                    if (parsed == null) {
                        loose += inlineSpans(node, InlineStyle())
                    } else {
                        flushLoose()
                        blocks += parsed
                    }
                }
                else -> Unit
            }
        }
        flushLoose()
        return blocks
    }

    /** Returns null when the element is inline and belongs in the surrounding paragraph. */
    private fun parseElement(element: Element): List<Block>? = when (element.tagName().lowercase()) {
        "p" -> paragraphBlocks(element)
        "h1" -> listOf(Block.Heading(1, inline(element)))
        "h2" -> listOf(Block.Heading(2, inline(element)))
        "h3" -> listOf(Block.Heading(3, inline(element)))
        "h4" -> listOf(Block.Heading(4, inline(element)))
        "h5", "h6" -> listOf(Block.Heading(5, inline(element)))
        "blockquote" -> listOf(Block.Quote(null, null, null, null, parseChildren(element)))
        "aside" -> asideBlocks(element)
        "pre" -> listOf(codeBlock(element))
        "ul" -> listOf(listBlock(element, ordered = false))
        "ol" -> listOf(listBlock(element, ordered = true))
        "hr" -> listOf(Block.Rule)
        "table" -> listOf(tableBlock(element))
        "details" -> listOf(detailsBlock(element))
        "div", "section", "article" -> divBlocks(element)
        "iframe" -> listOf(videoBlock(element))
        "img" -> if (isEmoji(element)) null else listOfNotNull(pictureBlock(element))
        "figure" -> divBlocks(element)
        else -> null
    }

    /**
     * A paragraph can hold a full-width image, which reads far better as its own block
     * than squeezed into a line of text.
     */
    private fun paragraphBlocks(element: Element): List<Block> {
        val pictures = element.select("img").filterNot { isEmoji(it) }
        if (pictures.isEmpty()) {
            val content = inline(element)
            return if (content.isBlank) emptyList() else listOf(Block.Paragraph(content))
        }

        val blocks = mutableListOf<Block>()
        pictures.forEach { it.remove() }
        val text = inline(element)
        if (!text.isBlank) blocks += Block.Paragraph(text)
        pictures.forEach { image -> pictureBlock(image)?.let { blocks += it } }
        return blocks
    }

    private fun divBlocks(element: Element): List<Block> = when {
        element.hasClass("poll") -> listOf(pollBlock(element))
        element.hasClass("lightbox-wrapper") ->
            listOfNotNull(element.selectFirst("img")?.let { pictureBlock(it) })
        element.hasClass("video-container") || element.selectFirst("iframe") != null ->
            listOf(videoBlock(element.selectFirst("iframe") ?: element))
        else -> parseChildren(element)
    }

    /**
     * `<aside>` covers both Discourse's quote-with-attribution and its link previews.
     */
    private fun asideBlocks(element: Element): List<Block> = when {
        element.hasClass("quote") -> listOf(quoteBlock(element))
        element.hasClass("onebox") || element.selectFirst(".onebox-body") != null ->
            listOf(oneboxBlock(element))
        else -> parseChildren(element)
    }

    private fun quoteBlock(element: Element): Block.Quote {
        val author = element.attr("data-username").takeIf { it.isNotBlank() }
            ?: element.selectFirst(".title")?.text()?.substringBefore(':')?.trim()?.takeIf { it.isNotBlank() }
        val avatar = element.selectFirst(".title img.avatar")?.let { Forum.absoluteUrl(it.attr("src")) }
        val topicId = element.attr("data-topic").toLongOrNull()
        val postNumber = element.attr("data-post").toIntOrNull()

        val inner = element.selectFirst("blockquote")
        val blocks = if (inner != null) parseChildren(inner) else parseChildren(element)
        return Block.Quote(author, avatar, topicId, postNumber, blocks)
    }

    private fun oneboxBlock(element: Element): Block {
        val link = element.selectFirst("a[href]")
        val url = link?.attr("href").orEmpty()
        val title = element.selectFirst("h3, .site-name + a, .onebox-body h3")?.text()?.trim()
            ?: link?.text()?.trim().orEmpty()
        val description = element.selectFirst("p, .onebox-body p")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val image = element.selectFirst("img.thumbnail, .onebox-body img")
            ?.let { Forum.absoluteUrl(it.attr("src")) }
        val site = element.selectFirst(".site-name")?.text()?.trim()
            ?: url.substringAfter("://", "").substringBefore('/').takeIf { it.isNotBlank() }
        return Block.LinkPreview(
            url = url,
            title = title.ifBlank { site ?: url },
            description = description,
            imageUrl = image,
            siteName = site
        )
    }

    private fun codeBlock(element: Element): Block.Code {
        val code = element.selectFirst("code")
        val language = code?.className()
            ?.split(' ')
            ?.firstOrNull { it.startsWith("lang-") }
            ?.removePrefix("lang-")
            ?.takeIf { it.isNotBlank() && it != "auto" && it != "nohighlight" }
        val text = (code ?: element).wholeText().trimEnd('\n')
        return Block.Code(text, language)
    }

    private fun listBlock(element: Element, ordered: Boolean): Block.Bullets {
        val start = element.attr("start").toIntOrNull() ?: 1
        val items = element.children()
            .filter { it.tagName().equals("li", ignoreCase = true) }
            .map { item ->
                parseChildren(item).ifEmpty {
                    val content = inline(item)
                    if (content.isBlank) emptyList() else listOf(Block.Paragraph(content))
                }
            }
        return Block.Bullets(ordered, start, items)
    }

    private fun tableBlock(element: Element): Block.Table {
        val header = element.select("thead th").map { inline(it) }
        val rows = element.select("tbody tr").map { row -> row.select("td").map { inline(it) } }
        if (header.isEmpty() && rows.isEmpty()) {
            // Some tables have no thead; treat the first row as the header.
            val all = element.select("tr")
            val first = all.firstOrNull()?.select("th, td")?.map { inline(it) } ?: emptyList()
            val rest = all.drop(1).map { row -> row.select("th, td").map { inline(it) } }
            return Block.Table(first, rest)
        }
        return Block.Table(header, rows)
    }

    private fun detailsBlock(element: Element): Block.Expandable {
        val summaryElement = element.selectFirst("summary")
        val summary = summaryElement?.text()?.trim().orEmpty().ifEmpty { "Show more" }
        summaryElement?.remove()
        return Block.Expandable(summary, parseChildren(element))
    }

    private fun pictureBlock(image: Element): Block.Picture? {
        val src = image.attr("src").ifBlank { image.attr("data-src") }
        val url = Forum.absoluteUrl(src) ?: return null
        return Block.Picture(
            url = url,
            alt = image.attr("alt").takeIf { it.isNotBlank() },
            width = image.attr("width").toIntOrNull(),
            height = image.attr("height").toIntOrNull()
        )
    }

    private fun videoBlock(element: Element): Block.Video {
        val src = element.attr("src").ifBlank { element.selectFirst("iframe")?.attr("src").orEmpty() }
        return Block.Video(
            url = Forum.absoluteUrl(src) ?: src,
            thumbnailUrl = element.selectFirst("img")?.let { Forum.absoluteUrl(it.attr("src")) },
            title = element.attr("title").takeIf { it.isNotBlank() }
        )
    }

    private fun pollBlock(element: Element): Block.Poll {
        val title = element.selectFirst(".poll-title, .poll-info-label")?.text()?.trim()
        val options = element.select("li[data-poll-option-id], .poll-container li")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
        return Block.Poll(title, options)
    }


    private data class InlineStyle(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false,
        val code: Boolean = false,
        val highlight: Boolean = false,
        val superscript: Boolean = false,
        val subscript: Boolean = false,
        val link: ForumLink? = null
    )

    private fun inline(element: Element): InlineText =
        InlineText(normalise(inlineSpans(element, InlineStyle())))

    private fun inlineSpans(element: Element, style: InlineStyle): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = collapse(node.getWholeText())
                    if (text.isNotEmpty()) spans += style.toSpan(text)
                }
                is Element -> spans += inlineElement(node, style)
                else -> Unit
            }
        }
        return spans
    }

    private fun inlineElement(element: Element, style: InlineStyle): List<InlineSpan> {
        val tag = element.tagName().lowercase()

        if (tag == "br") return listOf(InlineSpan("\n"))
        if (tag == "img") {
            if (isEmoji(element)) {
                val url = Forum.absoluteUrl(element.attr("src")) ?: return emptyList()
                val shortcode = element.attr("alt").ifBlank { element.attr("title") }
                return listOf(style.toSpan(shortcode.ifBlank { ":emoji:" }).copy(emojiUrl = url))
            }
            // A stray inline image is shown as its alt text; the block parser lifts real
            // pictures out into their own block before this is reached.
            return element.attr("alt").takeIf { it.isNotBlank() }?.let { listOf(style.toSpan(it)) }
                ?: emptyList()
        }

        val nested = when (tag) {
            "b", "strong" -> style.copy(bold = true)
            "i", "em", "cite" -> style.copy(italic = true)
            "s", "del", "strike" -> style.copy(strike = true)
            "code" -> style.copy(code = true)
            "mark" -> style.copy(highlight = true)
            "sup" -> style.copy(superscript = true)
            "sub" -> style.copy(subscript = true)
            "a" -> style.copy(link = ForumLinks.resolve(element.attr("href")) ?: style.link)
            "span" -> if (element.hasClass("mention")) {
                style.copy(link = mentionLink(element) ?: style.link)
            } else {
                style
            }
            else -> style
        }
        return inlineSpans(element, nested)
    }

    /** `<span class="mention">@name</span>` has no href of its own. */
    private fun mentionLink(element: Element): ForumLink? =
        element.text().trim().removePrefix("@").takeIf { it.isNotBlank() }?.let { ForumLink.User(it) }

    private fun InlineStyle.toSpan(text: String) = InlineSpan(
        text = text,
        bold = bold,
        italic = italic,
        strike = strike,
        code = code,
        highlight = highlight,
        superscript = superscript,
        subscript = subscript,
        link = link
    )

    private fun isEmoji(element: Element): Boolean =
        element.hasClass("emoji") || element.hasClass("emoji-only") ||
            element.attr("src").contains("/images/emoji/")

    /** HTML collapses runs of whitespace, including newlines, into a single space. */
    private fun collapse(text: String): String = text.replace(WHITESPACE, " ")

    /**
     * Merges neighbouring spans that share a style and trims the edges, so the renderer is
     * not handed a paragraph that starts with a stray space from the source markup.
     */
    private fun normalise(spans: List<InlineSpan>): List<InlineSpan> {
        val merged = mutableListOf<InlineSpan>()
        for (span in spans) {
            if (span.text.isEmpty() && span.emojiUrl == null) continue
            val last = merged.lastOrNull()
            if (last != null && span.emojiUrl == null && last.emojiUrl == null &&
                last.copy(text = "") == span.copy(text = "")
            ) {
                merged[merged.lastIndex] = last.copy(text = last.text + span.text)
            } else {
                merged += span
            }
        }
        if (merged.isEmpty()) return merged

        merged[0] = merged[0].let { if (it.emojiUrl == null) it.copy(text = it.text.trimStart()) else it }
        merged[merged.lastIndex] = merged[merged.lastIndex].let {
            if (it.emojiUrl == null) it.copy(text = it.text.trimEnd()) else it
        }
        return merged.filter { it.text.isNotEmpty() || it.emojiUrl != null }
    }

    private val WHITESPACE = Regex("\\s+")
}

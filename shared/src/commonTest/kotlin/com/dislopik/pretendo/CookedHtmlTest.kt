package com.dislopik.pretendo

import com.dislopik.pretendo.ui.html.Block
import com.dislopik.pretendo.ui.html.CookedHtml
import com.dislopik.pretendo.ui.html.ForumLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The HTML in these tests is the shape Discourse actually sends in a post's `cooked`
 * field, so a change to the parser that would break real posts fails here first.
 */
class CookedHtmlTest {

    @Test
    fun parsesParagraphsAndInlineMarks() {
        val blocks = CookedHtml.parse(
            "<p>Plain text with <strong>bold</strong> and <em>italic</em> and " +
                "<code>code()</code>.</p>"
        )

        assertEquals(1, blocks.size)
        val paragraph = blocks.first() as Block.Paragraph
        assertEquals("Plain text with bold and italic and code().", paragraph.content.plainText)
        assertTrue(paragraph.content.spans.any { it.bold && it.text == "bold" })
        assertTrue(paragraph.content.spans.any { it.italic && it.text == "italic" })
        assertTrue(paragraph.content.spans.any { it.code && it.text == "code()" })
    }

    @Test
    fun collapsesWhitespaceTheWayHtmlDoes() {
        val blocks = CookedHtml.parse("<p>one\n   two\t\tthree</p>")
        val paragraph = blocks.first() as Block.Paragraph
        assertEquals("one two three", paragraph.content.plainText)
    }

    @Test
    fun readsLinksAsForumDestinations() {
        val blocks = CookedHtml.parse(
            """<p>See <a href="/t/some-slug/1234/7">this topic</a> and
               <a href="https://example.com/page">this site</a>.</p>"""
        )
        val spans = (blocks.first() as Block.Paragraph).content.spans

        val topic = spans.firstOrNull { it.link is ForumLink.Topic }?.link as? ForumLink.Topic
        assertEquals(1234L, topic?.id)
        assertEquals(7, topic?.postNumber)

        val external = spans.firstOrNull { it.link is ForumLink.External }?.link as? ForumLink.External
        assertEquals("https://example.com/page", external?.url)
    }

    @Test
    fun readsMentionsAsProfileLinks() {
        val blocks = CookedHtml.parse(
            """<p>Thanks <a class="mention" href="/u/ExpertGem">@ExpertGem</a>!</p>"""
        )
        val spans = (blocks.first() as Block.Paragraph).content.spans
        val user = spans.firstOrNull { it.link is ForumLink.User }?.link as? ForumLink.User
        assertEquals("ExpertGem", user?.username)
    }

    @Test
    fun keepsQuoteAttribution() {
        val blocks = CookedHtml.parse(
            """<aside class="quote no-group" data-username="Jon" data-post="3" data-topic="99">
                 <div class="title">Jon:</div>
                 <blockquote><p>The original point</p></blockquote>
               </aside>
               <p>My answer</p>"""
        )

        val quote = blocks.filterIsInstance<Block.Quote>().single()
        assertEquals("Jon", quote.author)
        assertEquals(99L, quote.topicId)
        assertEquals(3, quote.postNumber)
        assertEquals(
            "The original point",
            (quote.blocks.single() as Block.Paragraph).content.plainText
        )
        assertTrue(blocks.any { it is Block.Paragraph && it.content.plainText == "My answer" })
    }

    @Test
    fun keepsCodeBlocksVerbatimWithTheirLanguage() {
        val blocks = CookedHtml.parse(
            "<pre><code class=\"lang-kotlin\">fun main() {\n    println(1)\n}\n</code></pre>"
        )
        val code = blocks.single() as Block.Code
        assertEquals("kotlin", code.language)
        assertEquals("fun main() {\n    println(1)\n}", code.text)
    }

    @Test
    fun readsListsAndTheirStartNumber() {
        val bullets = CookedHtml.parse("<ul><li>first</li><li>second</li></ul>").single()
        assertTrue(bullets is Block.Bullets && !bullets.ordered)
        assertEquals(2, bullets.items.size)

        val ordered = CookedHtml.parse("""<ol start="3"><li>third</li></ol>""").single()
        assertTrue(ordered is Block.Bullets && ordered.ordered)
        assertEquals(3, ordered.start)
    }

    @Test
    fun liftsImagesOutOfParagraphsButKeepsEmojiInline() {
        val blocks = CookedHtml.parse(
            """<p>Look at this <img src="/uploads/default/1.png" alt="screenshot" width="800"
               height="600"> and smile <img class="emoji" src="/images/emoji/twitter/smile.png"
               alt=":smile:"></p>"""
        )

        val picture = blocks.filterIsInstance<Block.Picture>().single()
        assertEquals("https://forum.pretendo.network/uploads/default/1.png", picture.url)
        assertEquals("screenshot", picture.alt)
        assertEquals(800, picture.width)

        val paragraph = blocks.filterIsInstance<Block.Paragraph>().single()
        val emoji = paragraph.content.spans.single { it.emojiUrl != null }
        assertEquals(":smile:", emoji.text)
        assertTrue(emoji.emojiUrl!!.endsWith("/images/emoji/twitter/smile.png"))
    }

    @Test
    fun readsHeadingsAndRules() {
        val blocks = CookedHtml.parse("<h2>A heading</h2><hr><p>after</p>")
        assertEquals(2, (blocks[0] as Block.Heading).level)
        assertEquals("A heading", (blocks[0] as Block.Heading).content.plainText)
        assertTrue(blocks[1] is Block.Rule)
    }

    @Test
    fun readsDetailsAsSomethingExpandable() {
        val block = CookedHtml.parse(
            "<details><summary>Spoiler</summary><p>the answer</p></details>"
        ).single() as Block.Expandable

        assertEquals("Spoiler", block.summary)
        assertEquals("the answer", (block.blocks.single() as Block.Paragraph).content.plainText)
    }

    @Test
    fun fallsBackToPlainTextRatherThanShowingNothing() {
        // A body made of markup the parser has no block rule for still has to be readable.
        val blocks = CookedHtml.parse("<span>just a span</span>")
        assertEquals("just a span", (blocks.single() as Block.Paragraph).content.plainText)
    }

    @Test
    fun emptyBodiesProduceNoBlocks() {
        assertTrue(CookedHtml.parse(null).isEmpty())
        assertTrue(CookedHtml.parse("").isEmpty())
        assertTrue(CookedHtml.parse("<p>   </p>").isEmpty())
    }

    @Test
    fun summaryStripsQuotesAndMarkupForListRows() {
        val summary = CookedHtml.plainSummary(
            """<aside class="quote"><blockquote><p>quoted noise</p></blockquote></aside>
               <p>The <strong>actual</strong> point</p>"""
        )
        assertEquals("The actual point", summary)
    }

    @Test
    fun summaryTrimsToTheRequestedLength() {
        val summary = CookedHtml.plainSummary("<p>${"a".repeat(300)}</p>", limit = 50)
        assertEquals(51, summary.length) // 50 characters plus the ellipsis
        assertTrue(summary.endsWith("…"))
    }
}

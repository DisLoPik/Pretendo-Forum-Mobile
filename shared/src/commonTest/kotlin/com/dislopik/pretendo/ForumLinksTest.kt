package com.dislopik.pretendo

import com.dislopik.pretendo.ui.html.ForumLink
import com.dislopik.pretendo.ui.html.ForumLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Links inside posts decide whether a tap stays in the app or leaves for a browser, so
 * every shape Discourse writes is checked here.
 */
class ForumLinksTest {

    @Test
    fun recognisesTopicsInAllTheirForms() {
        assertEquals(
            ForumLink.Topic(1234, null),
            ForumLinks.resolve("/t/1234")
        )
        assertEquals(
            ForumLink.Topic(1234, null),
            ForumLinks.resolve("/t/some-slug/1234")
        )
        assertEquals(
            ForumLink.Topic(1234, 7),
            ForumLinks.resolve("/t/some-slug/1234/7")
        )
        assertEquals(
            ForumLink.Topic(1234, 7),
            ForumLinks.resolve("https://forum.pretendo.network/t/some-slug/1234/7")
        )
    }

    @Test
    fun recognisesProfilesCategoriesAndTags() {
        assertEquals(ForumLink.User("ExpertGem"), ForumLinks.resolve("/u/ExpertGem"))
        assertEquals(ForumLink.Category("support", 6), ForumLinks.resolve("/c/support/6"))
        assertEquals(ForumLink.Tag("outage"), ForumLinks.resolve("/tag/outage"))
    }

    @Test
    fun treatsOtherSitesAsExternal() {
        val link = ForumLinks.resolve("https://pretendo.network/account")
        assertTrue(link is ForumLink.External)
        assertEquals("https://pretendo.network/account", link.url)
    }

    @Test
    fun treatsUnknownForumPathsAsExternalToTheForum() {
        // The app has no screen for these, but they still belong on the forum's own site.
        val link = ForumLinks.resolve("/badges/12/nice-topic")
        assertEquals(
            ForumLink.External("https://forum.pretendo.network/badges/12/nice-topic"),
            link
        )
    }

    @Test
    fun ignoresAnchorsAndEmptyHrefs() {
        assertNull(ForumLinks.resolve("#heading-1"))
        assertNull(ForumLinks.resolve(""))
        assertNull(ForumLinks.resolve(null))
    }

    @Test
    fun stripsQueryAndFragmentBeforeMatching() {
        assertEquals(
            ForumLink.Topic(99, null),
            ForumLinks.resolve("/t/slug/99?page=2#post_3")
        )
    }
}

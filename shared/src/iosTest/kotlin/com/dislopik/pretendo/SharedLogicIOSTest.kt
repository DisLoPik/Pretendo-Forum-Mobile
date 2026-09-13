package com.dislopik.pretendo

import com.dislopik.pretendo.data.Forum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** URL building, which decides whether shared links and avatars resolve at all. */
class SharedLogicIOSTest {

    @Test
    fun buildsTopicLinksTheWayTheWebsiteDoes() {
        assertEquals(
            "https://forum.pretendo.network/t/some-slug/123",
            Forum.topicUrl(123, "some-slug")
        )
        assertEquals(
            "https://forum.pretendo.network/t/some-slug/123/4",
            Forum.topicUrl(123, "some-slug", postNumber = 4)
        )
        // The first post is the topic itself, so it needs no post number.
        assertEquals(
            "https://forum.pretendo.network/t/some-slug/123",
            Forum.topicUrl(123, "some-slug", postNumber = 1)
        )
        assertEquals("https://forum.pretendo.network/t/123", Forum.topicUrl(123, null))
    }

    @Test
    fun fillsInTheSizeDiscourseLeavesOutOfAvatarTemplates() {
        assertEquals(
            "https://forum.pretendo.network/user_avatar/forum.pretendo.network/gem/96/1_2.png",
            Forum.avatarUrl("/user_avatar/forum.pretendo.network/gem/{size}/1_2.png", size = 96)
        )
        assertNull(Forum.avatarUrl(null))
    }

    @Test
    fun leavesAbsoluteUrlsAlone() {
        assertEquals(
            "https://cdn.example.com/a.png",
            Forum.absoluteUrl("https://cdn.example.com/a.png")
        )
        assertEquals("https://example.com/a.png", Forum.absoluteUrl("//example.com/a.png"))
        assertNull(Forum.absoluteUrl(""))
    }
}

package com.dislopik.pretendo

import com.dislopik.pretendo.data.forumJson
import com.dislopik.pretendo.model.SearchResponse
import com.dislopik.pretendo.model.SiteDto
import com.dislopik.pretendo.model.TopicDetailDto
import com.dislopik.pretendo.model.TopicListResponse
import com.dislopik.pretendo.model.UserActionsResponse
import com.dislopik.pretendo.model.UserResponse
import com.dislopik.pretendo.model.UserSummaryResponse
import com.dislopik.pretendo.model.isPrivateMessage
import com.dislopik.pretendo.model.likeCount
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Decodes real responses captured from forum.pretendo.network.
 *
 * The wire types are guesses until something checks them against what the forum actually
 * sends, and a guess that is wrong shows up as "the forum sent something this app could
 * not read" with no clue which field caused it. The fixtures in `resources/fixtures` are
 * unedited apart from being trimmed, and are decoded with the app's own configuration, so
 * a shape the app cannot read fails here first.
 *
 * Re-capture them with, for example:
 *   curl -H 'User-Agent: Mozilla/5.0' https://forum.pretendo.network/latest.json
 */
class ApiPayloadTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "missing fixture: $name"
        }.use { it.readBytes().decodeToString() }

    private inline fun <reified T> decode(name: String): T =
        forumJson.decodeFromString<T>(fixture(name))

    @Test
    fun theTopicListDecodes() {
        val response = decode<TopicListResponse>("latest.json")
        val topics = response.topicList?.topics.orEmpty()

        assertTrue(topics.size >= 5, "expected a full page of topics")
        assertTrue(topics.all { it.id > 0 })
        assertTrue(topics.any { it.title.isNotBlank() })
        assertTrue(response.users.isNotEmpty(), "posters ride alongside the topics")
        assertTrue(topics.any { it.posters.isNotEmpty() })
    }

    /**
     * This forum sends tags as objects rather than the plain names stock Discourse uses,
     * which is what once took the whole topic list down.
     */
    @Test
    fun tagsDecodeToNamesWhicheverShapeTheyArriveIn() {
        val topics = decode<TopicListResponse>("latest.json").topicList?.topics.orEmpty()
        val tagged = topics.filter { it.tags.isNotEmpty() }

        assertTrue(tagged.isNotEmpty(), "the fixture should include tagged topics")
        tagged.flatMap { it.tags }.forEach { tag ->
            assertTrue(tag.isNotBlank(), "a tag decoded to nothing")
            assertTrue(!tag.startsWith("{"), "a tag decoded as raw JSON: $tag")
        }
    }

    @Test
    fun plainStringTagsStillDecode() {
        // Stock Discourse sends names, and the app has to keep reading those too.
        val stock = """{"topic_list":{"topics":[{"id":1,"title":"x","tags":["outage","mh4u"]}]}}"""
        val topics = forumJson.decodeFromString<TopicListResponse>(stock).topicList?.topics.orEmpty()
        assertEquals(listOf("outage", "mh4u"), topics.single().tags)
    }

    @Test
    fun aTopicAndItsPostsDecode() {
        val topic = decode<TopicDetailDto>("topic.json")

        assertTrue(topic.id > 0)
        assertTrue(topic.title.isNotBlank())
        assertTrue(topic.postStream.posts.isNotEmpty(), "a topic without posts is not a topic")
        assertTrue(topic.postStream.stream.isNotEmpty(), "the post stream lists every id")
        assertTrue(!topic.isPrivateMessage)

        val first = topic.postStream.posts.first()
        assertTrue(first.cooked.isNotBlank(), "post bodies arrive as cooked HTML")
        assertTrue(first.username.isNotBlank())
        assertEquals(1, first.postNumber)
        // The like count is read out of the action summary rather than a plain field.
        assertTrue(first.likeCount >= 0)
    }

    @Test
    fun theCategoryListDecodes() {
        val site = decode<SiteDto>("site.json")

        assertTrue(site.categories.size >= 5)
        val support = site.categories.firstOrNull { it.slug == "support" }
        assertNotNull(support, "the Support category should be present")
        assertTrue(support.name.isNotBlank())
        assertTrue(support.color.isNotBlank())
        // Subcategories are what the Categories tab nests under their parent.
        assertTrue(
            site.categories.any { it.parentCategoryId != null },
            "expected at least one subcategory"
        )
    }

    @Test
    fun aProfileDecodes() {
        val user = decode<UserResponse>("user.json").user
        assertNotNull(user)
        assertTrue(user.username.isNotBlank())
        assertTrue(user.id > 0)
        assertTrue(user.createdAt?.isNotBlank() == true)
    }

    @Test
    fun aProfileSummaryDecodes() {
        val summary = decode<UserSummaryResponse>("summary.json").userSummary
        assertNotNull(summary)
        assertTrue(summary.postCount >= 0)
        assertTrue(summary.likesReceived >= 0)
        assertTrue(summary.daysVisited >= 0)
    }

    @Test
    fun userActivityDecodes() {
        val actions = decode<UserActionsResponse>("actions.json").userActions
        assertTrue(actions.isNotEmpty())
        assertTrue(actions.all { it.topicId > 0 })
        assertTrue(actions.any { it.title.isNotBlank() })
    }

    @Test
    fun searchResultsDecode() {
        val results = decode<SearchResponse>("search.json")
        assertTrue(
            results.topics.isNotEmpty() || results.posts.isNotEmpty(),
            "the search fixture should carry results"
        )
        results.posts.forEach { assertTrue(it.topicId > 0) }
    }
}

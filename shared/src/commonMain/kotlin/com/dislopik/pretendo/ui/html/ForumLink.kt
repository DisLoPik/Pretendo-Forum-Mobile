package com.dislopik.pretendo.ui.html

import com.dislopik.pretendo.data.Forum

/**
 * Where a link inside a post should take the reader.
 *
 * Discourse writes plain site-relative hrefs, so a link to another topic is just
 * `/t/some-slug/1234`. Recognising those keeps the reader inside the app instead of
 * bouncing them out to a browser and back.
 */
sealed interface ForumLink {
    data class Topic(val id: Long, val postNumber: Int?) : ForumLink
    data class User(val username: String) : ForumLink
    data class Category(val slug: String, val id: Int?) : ForumLink
    data class Tag(val name: String) : ForumLink
    data class Search(val query: String) : ForumLink
    data class External(val url: String) : ForumLink
}

object ForumLinks {

    /** Classifies an href, treating anything that is not clearly on the forum as external. */
    fun resolve(href: String?): ForumLink? {
        if (href.isNullOrBlank()) return null
        val trimmed = href.trim()

        val path = when {
            trimmed.startsWith(Forum.BASE_URL) -> trimmed.removePrefix(Forum.BASE_URL)
            trimmed.startsWith("/") -> trimmed
            // A bare anchor within the same post has nowhere else to go.
            trimmed.startsWith("#") -> return null
            else -> return ForumLink.External(trimmed)
        }.ifEmpty { "/" }

        val clean = path.substringBefore('#').substringBefore('?')
        val segments = clean.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return ForumLink.External(Forum.BASE_URL)

        return when (segments[0]) {
            "t" -> parseTopic(segments)
            "u" -> segments.getOrNull(1)?.let { ForumLink.User(it) }
            "c" -> parseCategory(segments)
            "tag", "tags" -> segments.getOrNull(1)?.let { ForumLink.Tag(it) }
            "search" -> ForumLink.Search(path.substringAfter("q=", "").substringBefore('&'))
            else -> ForumLink.External(Forum.BASE_URL + path)
        } ?: ForumLink.External(Forum.BASE_URL + path)
    }

    /**
     * `/t/1234`, `/t/slug/1234` and `/t/slug/1234/7` are all valid ways to point at a
     * topic, the last one at a specific post.
     */
    private fun parseTopic(segments: List<String>): ForumLink? {
        val numbers = segments.drop(1).mapNotNull { it.toLongOrNull() }
        val id = numbers.firstOrNull() ?: return null
        val postNumber = numbers.getOrNull(1)?.toInt()
        return ForumLink.Topic(id, postNumber)
    }

    /** `/c/slug/12` and the older `/c/slug` both appear in older posts. */
    private fun parseCategory(segments: List<String>): ForumLink? {
        val tail = segments.drop(1)
        if (tail.isEmpty()) return null
        val id = tail.lastOrNull()?.toIntOrNull()
        val slug = if (id != null) tail.dropLast(1).lastOrNull() else tail.last()
        return ForumLink.Category(slug ?: return null, id)
    }
}

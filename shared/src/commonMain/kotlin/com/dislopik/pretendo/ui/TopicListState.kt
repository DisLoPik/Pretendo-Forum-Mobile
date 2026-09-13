package com.dislopik.pretendo.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dislopik.pretendo.data.ForumAuthException
import com.dislopik.pretendo.model.TopicDto
import com.dislopik.pretendo.model.TopicListResponse
import com.dislopik.pretendo.model.UserBriefDto
import com.dislopik.pretendo.data.runCatchingUnlessCancelled

/**
 * Paging for any of the forum's topic lists.
 *
 * Discourse pages every list the same way, so latest, a category, a tag, bookmarks and
 * the message inbox all share this rather than each screen growing its own copy.
 */
@Stable
class TopicListState(private val load: suspend (page: Int) -> TopicListResponse) {

    var topics: List<TopicDto> by mutableStateOf(emptyList())
        private set

    /** Posters are sent alongside the topics rather than inside them. */
    var usersById: Map<Long, UserBriefDto> by mutableStateOf(emptyMap())
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    var loadingMore: Boolean by mutableStateOf(false)
        private set

    var error: String? by mutableStateOf(null)
        private set

    var authLost: Boolean by mutableStateOf(false)
        private set

    var hasMore: Boolean by mutableStateOf(true)
        private set

    private var page = 0

    val isEmpty: Boolean get() = topics.isEmpty() && !loading && error == null

    suspend fun refresh() {
        loading = true
        error = null
        authLost = false
        page = 0
        try {
            runCatchingUnlessCancelled { load(0) }
                .onSuccess { response ->
                    val list = response.topicList
                    topics = list?.topics.orEmpty()
                    usersById = response.users.associateBy { it.id }
                    hasMore = list?.moreTopicsUrl != null && list.topics.isNotEmpty()
                }
                .onFailure { failure ->
                    if (failure is ForumAuthException) authLost = true
                    error = failure.message ?: "Could not load this list."
                    topics = emptyList()
                }
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loadingMore || loading || !hasMore) return
        loadingMore = true
        val next = page + 1
        try {
            runCatchingUnlessCancelled { load(next) }
            .onSuccess { response ->
                val list = response.topicList
                val newTopics = list?.topics.orEmpty()
                // Discourse keeps answering with a "more" link past the end, so an empty
                // page is the real signal that a list is finished.
                if (newTopics.isEmpty()) {
                    hasMore = false
                } else {
                    val seen = topics.mapTo(mutableSetOf()) { it.id }
                    topics = topics + newTopics.filter { seen.add(it.id) }
                    usersById = usersById + response.users.associateBy { it.id }
                    hasMore = list?.moreTopicsUrl != null
                    page = next
                }
            }
                .onFailure { failure ->
                    if (failure is ForumAuthException) authLost = true
                    hasMore = false
                }
        } finally {
            loadingMore = false
        }
    }
}

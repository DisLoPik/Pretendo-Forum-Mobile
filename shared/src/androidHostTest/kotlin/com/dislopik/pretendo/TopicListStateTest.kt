package com.dislopik.pretendo

import com.dislopik.pretendo.model.TopicDto
import com.dislopik.pretendo.model.TopicListDto
import com.dislopik.pretendo.model.TopicListResponse
import com.dislopik.pretendo.ui.TopicListState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The loading state behind every topic list.
 *
 * The cancellation case is the one that matters: a reader who leaves a list while it is
 * still loading should get silence, not Compose's internal cancellation message rendered
 * as though the forum had failed.
 */
class TopicListStateTest {

    private fun response(vararg titles: String) = TopicListResponse(
        topicList = TopicListDto(
            topics = titles.mapIndexed { index, title ->
                TopicDto(id = index + 1L, title = title)
            },
            moreTopicsUrl = null
        )
    )

    @Test
    fun leavingMidLoadReportsNothing() = runTest {
        // A load that never finishes on its own, so the only way out is cancellation.
        val neverFinishes = CompletableDeferred<TopicListResponse>()
        val state = TopicListState { neverFinishes.await() }

        val job = launch { state.refresh() }
        yield()
        assertTrue(state.loading, "the list should be loading before it is left")

        job.cancelAndJoin()

        assertNull(state.error, "leaving a list must not look like a failure")
        assertFalse(state.loading, "the spinner has to stop even when the load is cancelled")
    }

    @Test
    fun aRealFailureIsReported() = runTest {
        val state = TopicListState { throw IllegalStateException("the forum fell over") }

        state.refresh()

        assertEquals("the forum fell over", state.error)
        assertFalse(state.loading)
    }

    @Test
    fun aSuccessfulLoadFillsTheList() = runTest {
        val state = TopicListState { response("First topic", "Second topic") }

        state.refresh()

        assertEquals(2, state.topics.size)
        assertEquals("First topic", state.topics.first().title)
        assertNull(state.error)
        assertFalse(state.loading)
        assertFalse(state.hasMore, "there was no next page, so paging should stop")
    }

    @Test
    fun pagingStopsAtTheEndOfTheList() = runTest {
        var pagesAsked = 0
        val state = TopicListState { page ->
            pagesAsked++
            if (page == 0) {
                TopicListResponse(
                    topicList = TopicListDto(
                        topics = listOf(TopicDto(id = 1, title = "Only topic")),
                        moreTopicsUrl = "/latest?page=1"
                    )
                )
            } else {
                // Discourse keeps offering a "more" link past the end, so an empty page is
                // the real signal that the list is finished.
                TopicListResponse(topicList = TopicListDto(topics = emptyList()))
            }
        }

        state.refresh()
        assertTrue(state.hasMore)

        state.loadMore()
        assertFalse(state.hasMore)
        assertEquals(1, state.topics.size)

        state.loadMore()
        assertEquals(2, pagesAsked, "a finished list must not keep asking for pages")
    }
}

package com.dislopik.pretendo

import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Leaving a screen mid-load cancels its coroutine, and cancellation is a `Throwable`. A
 * plain `runCatching` therefore catches the screen's own cancellation and reports it as a
 * failure, which is how Compose's "rememberCoroutineScope left the composition" once ended
 * up rendered where a topic list should have been.
 */
class CancellationTest {

    @Test
    fun cancellationKeepsUnwinding() {
        assertFailsWith<CancellationException> {
            runCatchingUnlessCancelled { throw CancellationException("left the composition") }
        }
    }

    @Test
    fun realFailuresAreStillCaught() {
        val result = runCatchingUnlessCancelled { error("the forum fell over") }
        assertTrue(result.isFailure)
        assertEquals("the forum fell over", result.exceptionOrNull()?.message)
    }

    @Test
    fun successPassesTheValueThrough() {
        assertEquals(7, runCatchingUnlessCancelled { 7 }.getOrNull())
    }
}

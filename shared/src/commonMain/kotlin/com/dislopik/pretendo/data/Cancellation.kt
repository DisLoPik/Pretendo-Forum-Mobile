package com.dislopik.pretendo.data

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching], without swallowing cancellation.
 *
 * `runCatching` catches `Throwable`, and coroutine cancellation *is* a `Throwable`. Every
 * load in this app runs from a composable that the reader can leave at any moment, so a
 * plain `runCatching` ends up catching the screen's own cancellation and reporting it as
 * though the forum had failed, which is how "rememberCoroutineScope left the composition"
 * can appear where a topic list should be.
 *
 * Cancellation is rethrown so it keeps unwinding and the coroutine dies quietly. Only real
 * failures come back as a [Result].
 */
inline fun <T> runCatchingUnlessCancelled(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

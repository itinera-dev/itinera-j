package org.itineradev.itinera.report.async

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Runs `suspend` code that never suspends to its end on the caller's thread, as the engine runs a
 * synchronous workflow, so the tests need no coroutine library.
 */
fun <T> complete(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result -> outcome = result })
    return checkNotNull(outcome) { "The code suspended." }.getOrThrow()
}

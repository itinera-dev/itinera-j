package org.itineradev.itinera.engine

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Runs [block] to its end on the caller's thread and returns what it returns, or throws what it
 * throws. The synchronous mode never suspends, so a suspension is a bug in itinera, which throws an
 * [AssertionError].
 *
 * Part of itinera's engine, not of its API.
 */
public fun <T> driven(block: suspend () -> T): T {
    var ended: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result -> ended = result })
    return (ended ?: throw AssertionError("A synchronous journey suspended.")).getOrThrow()
}

package org.itineradev.itinera.engine

import java.util.concurrent.atomic.AtomicBoolean

/** Whether custom code of the synchronous journey running on this thread was interrupted. */
private val interruptions = ThreadLocal<AtomicBoolean>()

/**
 * Rethrows an [Error], which itinera never catches, and lets anything else custom code threw be
 * handled where it was caught. An [InterruptedException] caught during a synchronous journey is
 * remembered, so that [restoringInterrupts] sets the thread's interrupt flag once the journey ends.
 *
 * Part of itinera's engine, not of its API.
 */
public fun rethrowError(thrown: Throwable) {
    if (thrown is Error) throw thrown
    if (thrown is InterruptedException) interruptions.get()?.set(true)
}

/**
 * Runs [journey] on the caller's thread and returns what it returns, or throws what it throws, then
 * sets the thread's interrupt flag again when custom code of the journey was interrupted: throwing
 * [InterruptedException] cleared it, and the journey went on by the specification's rules, but
 * whoever interrupted the thread still sees it once the journey ends.
 */
internal fun <T> restoringInterrupts(journey: () -> T): T {
    val outer = interruptions.get()
    val inner = AtomicBoolean()
    interruptions.set(inner)
    try {
        return journey()
    } finally {
        if (outer == null) interruptions.remove() else interruptions.set(outer)
        if (inner.get()) Thread.currentThread().interrupt()
    }
}

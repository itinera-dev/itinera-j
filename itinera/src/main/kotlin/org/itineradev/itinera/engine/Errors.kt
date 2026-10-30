package org.itineradev.itinera.engine

/**
 * Rethrows an [Error], which itinera never catches, and lets anything else custom code threw be
 * handled where it was caught.
 *
 * Part of itinera's engine, not of its API.
 */
public fun rethrowError(thrown: Throwable) {
    if (thrown is Error) throw thrown
}

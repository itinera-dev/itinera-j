package org.itineradev.itinera.engine

/**
 * Delivers to every one of [reporters], whichever throws, then throws what the first one threw. An
 * [Error] is thrown at once. Being inline, it lets an asynchronous dispatcher await each delivery.
 *
 * Part of itinera's engine, not of its API.
 */
public inline fun <R> deliverToEveryone(reporters: List<R>, deliver: (R) -> Unit) {
    val failure =
        reporters
            .map { reporter -> caught(reporter, deliver) }
            .firstNotNullOfOrNull(Result<Unit>::exceptionOrNull)
    if (failure != null) throw failure
}

@PublishedApi
internal inline fun <R> caught(reporter: R, deliver: (R) -> Unit): Result<Unit> = runCatching {
    deliver(reporter)
}
    .onFailure(::rethrowError)

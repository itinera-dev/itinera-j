package org.itineradev.itinera.engine

/**
 * The message of what custom code threw: its own, or a fixed text when it has none, or when reading
 * it throws, since nothing may escape a journey. An [Error] is never caught.
 *
 * Part of itinera's engine, not of its API.
 */
public fun messageOf(thrown: Throwable): String = runCatching {
    thrown.message ?: NO_MESSAGE
}
    .onFailure(::rethrowError)
    .getOrDefault(UNREADABLE)

private const val NO_MESSAGE = "an error with no message"
private const val UNREADABLE = "an error whose message could not be read"

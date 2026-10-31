package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.Event

/**
 * What a recording reporter keeps, in either mode: every event, in order, and the latest value of
 * each entry of global metadata, in the order the keys were first set. Each read is a copy, so a
 * test keeps what it read while the journey goes on.
 */
internal class Recording {
    private val events = mutableListOf<Event>()
    private val globalMetadata = linkedMapOf<String, Any?>()

    fun report(event: Event) = synchronized(this) { events += event }

    fun setGlobalMetadata(key: String, value: Any?) =
        synchronized(this) { globalMetadata[key] = value }

    fun events(): List<Event> = synchronized(this) { events.toList() }

    fun globalMetadata(): Map<String, Any?> = synchronized(this) { LinkedHashMap(globalMetadata) }

    fun path(): List<String> = events().mapNotNull(::pathLine)
}

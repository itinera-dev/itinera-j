package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.event.Event

/**
 * What a Then sentence expected and itinera did not do: a failure of the implementation, never an
 * error in the case.
 */
internal class Unmet(message: String) : AssertionError(message)

/**
 * Holds when [condition] does, and otherwise throws [Unmet], saying what was expected and found.
 */
internal fun holds(condition: Boolean, expected: String, found: String) {
    if (!condition) throw Unmet("expected $expected; found $found")
}

/**
 * Holds when [condition] does, and otherwise says what was expected and which events [events] held.
 */
internal fun holdsOfStream(condition: Boolean, expected: String, events: List<Event>) {
    holds(condition, expected, "the events ${events.map(Event::kind)}")
}

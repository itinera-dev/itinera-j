package org.itineradev.itinera.conformance.trace

import java.lang.reflect.RecordComponent
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.event.Event

private val notCompared = setOf("journeyId", "timestamp")

/** Every field of each of [events], its type included, apart from the journey ID and timestamp. */
internal fun apartFromIdAndTime(events: List<Event>): List<List<Any?>> =
    events.map(::apartFromIdAndTime)

/** Every field of [event], its type included, apart from the journey ID and timestamp. */
internal fun apartFromIdAndTime(event: Event): List<Any?> =
    listOf(event.javaClass) + componentsOf(event).filter(::isCompared).map { valueOf(event, it) }

private fun componentsOf(event: Event): List<RecordComponent> =
    event.javaClass.recordComponents?.toList()
        ?: throw ModelException("${event.kind} is not a record, so its fields cannot be compared")

private fun isCompared(component: RecordComponent): Boolean = component.name !in notCompared

private fun valueOf(event: Event, component: RecordComponent): Any? =
    component.accessor.invoke(event)

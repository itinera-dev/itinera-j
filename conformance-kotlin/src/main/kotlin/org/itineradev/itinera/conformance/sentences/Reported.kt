package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.run.Entry
import org.itineradev.itinera.conformance.run.ScenarioReporter
import org.itineradev.itinera.event.Event

/**
 * The scripted reporters made under [name].
 *
 * @throws ModelException when the scenario made none, so no sentence can read what it received.
 */
internal fun madeAs(world: World, name: String): List<ScenarioReporter> =
    world.recorders.made(name).ifEmpty { throw ModelException("no reporter \"$name\" was made") }

/** Every event the scripted reporters made under [name] received. */
internal fun eventsOf(world: World, name: String): List<Event> =
    madeAs(world, name).flatMap(ScenarioReporter::events)

/** Every global metadata entry the scripted reporters made under [name] received. */
internal fun entriesOf(world: World, name: String): List<Entry> =
    madeAs(world, name).flatMap(ScenarioReporter::entries)

package org.itineradev.itinera.conformance

import org.itineradev.itinera.conformance.model.Model

/**
 * What one scenario's sentences share: the column it runs in and the model its Given sentences
 * fill. Cucumber makes one for each scenario and gives it to every class of sentences.
 */
internal class World {
    /** The column the scenario runs in, which the column's own glue sets before any sentence. */
    lateinit var column: Column

    /** What the scenario declares. */
    val model = Model()
}

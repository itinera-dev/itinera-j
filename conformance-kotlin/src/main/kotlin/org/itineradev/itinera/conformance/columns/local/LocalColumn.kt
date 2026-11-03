package org.itineradev.itinera.conformance.columns.local

import io.cucumber.java.Before
import org.itineradev.itinera.conformance.Column
import org.itineradev.itinera.conformance.World

/** Glue of the synchronous column only: tells each scenario it runs on `LocalExecutor`. */
internal class LocalColumn(private val world: World) {
    @Before(order = 0)
    fun runsSynchronously() {
        world.column = Column.SYNCHRONOUS
    }
}

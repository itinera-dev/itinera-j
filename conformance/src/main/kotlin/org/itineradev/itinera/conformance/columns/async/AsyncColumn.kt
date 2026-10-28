package org.itineradev.itinera.conformance.columns.async

import io.cucumber.java.Before
import org.itineradev.itinera.conformance.Column
import org.itineradev.itinera.conformance.World

/** Glue of the asynchronous column only: tells each scenario it runs on `AsyncLocalExecutor`. */
internal class AsyncColumn(private val world: World) {
    @Before(order = 0)
    fun runsAsynchronously() {
        world.column = Column.ASYNCHRONOUS
    }
}

package org.itineradev.itinera.report

import java.io.PrintStream
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event

/**
 * Prints each event on a line of its own on standard output, as its record prints, for a look at a
 * journey while writing a workflow. A line standard output cannot take is dropped.
 *
 * @sample org.itineradev.itinera.report.samples.debugging
 */
@ExperimentalItineraApi
public class DebugReporter internal constructor(private val out: PrintStream) : Reporter {
    /**
     * A reporter printing on standard output.
     *
     * @sample org.itineradev.itinera.report.samples.debugging
     */
    public constructor() : this(System.out)

    /**
     * Prints the event on a line of its own.
     *
     * @sample org.itineradev.itinera.report.samples.debugging
     */
    override fun report(event: Event) {
        out.println(event)
    }
}

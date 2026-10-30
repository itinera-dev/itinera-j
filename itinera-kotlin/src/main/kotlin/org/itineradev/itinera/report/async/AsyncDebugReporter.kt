package org.itineradev.itinera.report.async

import java.io.PrintStream
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event

/**
 * Prints each event of an asynchronous workflow on a line of its own on standard output, as its
 * record prints, for a look at a journey while writing a workflow. A line standard output cannot
 * take is dropped.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncDebugging
 */
@ExperimentalItineraApi
public class AsyncDebugReporter internal constructor(private val out: PrintStream) : AsyncReporter {
    /**
     * A reporter printing on standard output.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDebugging
     */
    public constructor() : this(System.out)

    /**
     * Prints the event on a line of its own.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDebugging
     */
    @JvmSynthetic
    override suspend fun report(event: Event) {
        out.println(event)
    }
}

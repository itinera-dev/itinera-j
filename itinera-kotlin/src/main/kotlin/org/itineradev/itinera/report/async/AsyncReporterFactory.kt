package org.itineradev.itinera.report.async

import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.ReporterFactoryOf
import org.itineradev.itinera.workflow.Async

/**
 * Makes a reporter for one instance of an asynchronous workflow, with the instance's journey ID,
 * while the instance is created. It is an ordinary function, as creating an instance is: what the
 * reporter needs from I/O is fetched before.
 *
 * @sample org.itineradev.itinera.instance.async.samples.asyncPerJourney
 */
public fun interface AsyncReporterFactory : ReporterFactoryOf<Async> {
    /**
     * Makes the reporter of the journey with this ID.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncPerJourney
     */
    override fun create(journeyId: JourneyId): AsyncReporter
}

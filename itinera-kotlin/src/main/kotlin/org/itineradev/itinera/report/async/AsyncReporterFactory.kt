package org.itineradev.itinera.report.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId

/**
 * Makes a reporter for one instance of an asynchronous workflow, with the instance's journey ID,
 * while the instance is created. It is an ordinary function, as creating an instance is: what the
 * reporter needs from I/O is fetched before.
 *
 * @sample org.itineradev.itinera.instance.async.samples.asyncPerJourney
 */
@ExperimentalItineraApi
public fun interface AsyncReporterFactory {
    /**
     * Makes the reporter of the journey with this ID.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncPerJourney
     */
    public fun create(journeyId: JourneyId): AsyncReporter
}

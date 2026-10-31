package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId

/**
 * Makes a reporter for one workflow instance, with the instance's journey ID, while the instance is
 * created. A `null` from Java makes creating the instance fail.
 *
 * @sample org.itineradev.itinera.report.samples.perJourney
 */
@ExperimentalItineraApi
public fun interface ReporterFactory {
    /**
     * Makes the reporter of the journey with this ID.
     *
     * @sample org.itineradev.itinera.report.samples.perJourney
     */
    public fun create(journeyId: JourneyId): Reporter
}

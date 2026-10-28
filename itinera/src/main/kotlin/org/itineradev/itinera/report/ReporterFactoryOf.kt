package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.Mode

/**
 * Makes a reporter of a workflow of mode [M] for one instance, with the instance's journey ID,
 * while the instance is created: [ReporterFactory] for a synchronous workflow, and
 * `AsyncReporterFactory` from `itinera-kotlin` for an asynchronous one. A workflow lists its
 * reporters as factories, since reporters belong to each instance, and each instance makes its own
 * with them, before the reporters it is given itself. A `null` from Java makes creating the
 * instance fail.
 *
 * @param M the mode of the workflows whose reporters it makes.
 * @sample org.itineradev.itinera.report.samples.listed
 */
@ExperimentalItineraApi
public fun interface ReporterFactoryOf<M : Mode> {
    /**
     * Makes the reporter of the journey with this ID.
     *
     * @sample org.itineradev.itinera.report.samples.listed
     */
    public fun create(journeyId: JourneyId): ReporterOf<M>
}

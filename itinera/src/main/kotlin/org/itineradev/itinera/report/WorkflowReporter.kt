package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * A reporter a workflow of mode [M] lists. Reporters belong to each instance, so the workflow holds
 * only how to make one: each instance makes its own, with its journey ID, before the reporters the
 * instance itself is given. [of] makes one for a synchronous workflow, and `asyncWorkflowReporter`,
 * in `itinera-kotlin`, one for an asynchronous workflow.
 *
 * @param M the mode of the workflow that lists it.
 * @sample org.itineradev.itinera.report.samples.listed
 */
@ExperimentalItineraApi
public class WorkflowReporter<M : Mode>
internal constructor(internal val make: (JourneyId) -> ReporterOf<M>?) {
    /** Makes reporters of synchronous workflows. */
    public companion object {
        /**
         * The reporter [factory] makes for each instance of a synchronous workflow.
         *
         * @sample org.itineradev.itinera.report.samples.listed
         */
        @JvmStatic
        public fun of(factory: ReporterFactory): WorkflowReporter<Sync> =
            WorkflowReporter(factory::create)
    }
}

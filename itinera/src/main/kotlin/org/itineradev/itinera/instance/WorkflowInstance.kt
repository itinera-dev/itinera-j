package org.itineradev.itinera.instance

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * One journey's worth of a workflow, ready to hand to the executor of its mode, which runs it at
 * most once: the workflow's descriptor and the application's workflow object side by side, with the
 * journey ID, the reporters, and what the journey starts with.
 *
 * [InstanceBuilder] creates one for a synchronous workflow, and `AsyncInstanceBuilder` from
 * `itinera-kotlin` for an asynchronous one. Any other implementation runs too, checked by the
 * executor before the journey.
 *
 * @param M the workflow's mode, which only the executor of that mode accepts.
 * @sample org.itineradev.itinera.instance.samples.instances
 */
public interface WorkflowInstance<M : Mode> {
    /** The workflow's descriptor. */
    public val descriptor: WorkflowDescriptor<*, M>

    /** The application's workflow object, which the descriptor's parts are called with. */
    public val workflow: Any?

    /** The journey's ID. */
    public val journeyId: JourneyId

    /** The journey's reporters, in the order they receive each event. */
    public val reporters: List<ReporterOf<M>>

    /** The initial data, by key, in the order the data bag starts with. */
    public val initialData: Map<String, Any?>

    /** The global metadata set on the reporters before the journey starts, by key, in order. */
    @ExperimentalItineraApi public val initialGlobalMetadata: Map<String, Any?>

    /**
     * Claims the instance for a journey: `true` the first time it is called, and `false` ever
     * after, so the executor refuses an instance handed to an executor before.
     */
    public fun claim(): Boolean
}

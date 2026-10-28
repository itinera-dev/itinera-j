package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Violations

/** What building the scenario's workflow gave: journeys to run, or the violations refusing it. */
internal sealed interface Admission {
    /** The builder accepted the workflow, whose journeys run as [journeys] says. */
    data class Admitted(val journeys: Journeys) : Admission

    /** The builder refused the workflow. */
    data class Refused(val violations: Violations) : Admission
}

/** How a column runs journeys of the admitted workflow, and its listing. */
internal interface Journeys {
    /** The admitted workflow's listing. */
    val listing: List<ListedStep>

    /** Runs [times] journeys, one after another, on one executor, each with a new instance. */
    fun run(times: Int): List<Journey>
}

/**
 * How one journey ended: with its result, refused by `run`, or before, when no instance was
 * created.
 */
internal sealed interface Ended {
    /** Creating the workflow instance failed, so `run` was never called. */
    data class NotCreated(val failure: IllegalStateException) : Ended

    /** `run` returned the result. */
    data class Returned(val result: JourneyResult) : Ended

    /** `run` refused the journey. */
    data class Refused(val refusal: Refusal) : Ended
}

/** One journey the scenario ran, with the ID of its instance, `null` when none was created. */
internal data class Journey(val journeyId: String?, val ended: Ended)

/** The journey ID of [instance]. */
internal fun idOf(instance: WorkflowInstance<*>): String = instance.journeyId.toString()

/** The journey of the instance [create] makes, run by [run], or the failure creating it. */
internal inline fun <I> journeyOf(
    create: () -> I,
    id: (I) -> String,
    run: (I) -> JourneyResult,
): Journey {
    val instance =
        try {
            create()
        } catch (failure: IllegalStateException) {
            return Journey(null, Ended.NotCreated(failure))
        }
    return Journey(id(instance), endedOf { run(instance) })
}

/** How [run] ended. */
internal inline fun endedOf(run: () -> JourneyResult): Ended =
    try {
        Ended.Returned(run())
    } catch (refusal: Refusal) {
        Ended.Refused(refusal)
    }

/** What building gave: the journeys [build] makes, or the violations it threw. */
internal inline fun admittedBy(build: () -> Journeys): Admission =
    try {
        Admission.Admitted(build())
    } catch (violations: Violations) {
        Admission.Refused(violations)
    }

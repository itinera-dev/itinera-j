package org.itineradev.itinera.conformance

import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.run.Admission
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Journey
import org.itineradev.itinera.conformance.run.Recorders
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.Witness
import org.itineradev.itinera.conformance.run.admittedBy
import org.itineradev.itinera.conformance.run.asyncJourneys
import org.itineradev.itinera.conformance.run.localJourneys
import org.itineradev.itinera.conformance.run.streamOf
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.workflow.ListedStep

/**
 * What one scenario's sentences share: the column it runs in, the model its Given sentences fill,
 * and what its When sentences did. Cucumber makes one for each scenario and gives it to every class
 * of sentences.
 */
internal class World {
    /** The column the scenario runs in, which the column's own glue sets before any sentence. */
    lateinit var column: Column

    /** What the scenario declares. */
    val model = Model()

    /** Every recording reporter of the scenario, by name. */
    val recorders = Recorders(model.reporterFailures)

    /** Every step the scenario's workflow built. */
    val witness = Witness()

    private var admitted: Admission? = null

    /** Every journey the scenario ran, in order. */
    var journeys: List<Journey> = emptyList()
        private set

    /** Builds the workflow the scenario declares, in the scenario's column, once. */
    fun admit(): Admission = admitted ?: admittedInColumn().also(::keep)

    private fun admittedInColumn(): Admission =
        when (column) {
            Column.SYNCHRONOUS -> admittedBy { localJourneys(model, recorders, witness) }
            Column.ASYNCHRONOUS -> admittedBy { asyncJourneys(model, recorders, witness) }
        }

    private fun keep(admission: Admission) {
        admitted = admission
    }

    /** Runs [times] journeys of the workflow on one executor, once it is admitted. */
    fun run(times: Int) {
        val admission = admit()
        if (admission is Admission.Admitted) journeys = admission.journeys.run(times)
    }

    /**
     * The listing of the workflow, once admitted.
     *
     * @throws Unmet when the builder refused the workflow.
     */
    val listing: List<ListedStep>
        get() =
            when (val admission = admit()) {
                is Admission.Admitted -> admission.journeys.listing
                is Admission.Refused ->
                    throw Unmet(
                        "expected a listing; the workflow was refused: ${admission.violations.message}"
                    )
            }

    /** What building the workflow gave, once a When sentence built it. */
    val admission: Admission
        get() = admitted ?: throw ModelException("the workflow was neither admitted nor run")

    /**
     * The scenario's journey, the last when it ran several.
     *
     * @throws Unmet when the builder refused the workflow, so no journey ran.
     */
    val journey: Journey
        get() = journeys.lastOrNull() ?: noJourney()

    private fun noJourney(): Nothing =
        when (val admission = admission) {
            is Admission.Refused ->
                throw Unmet(
                    "expected a journey; the workflow was refused: ${admission.violations.message}"
                )
            is Admission.Admitted ->
                throw ModelException("the workflow was only admitted, so no journey ran")
        }

    /**
     * The result of the scenario's journey.
     *
     * @throws Unmet when `run` refused the journey instead.
     */
    val result: JourneyResult
        get() =
            when (val ended = journey.ended) {
                is Ended.Returned -> ended.result
                is Ended.Refused ->
                    throw Unmet(
                        "expected a journey result; run was refused: ${ended.refusal.message}"
                    )
                is Ended.NotCreated ->
                    throw Unmet(
                        "expected a journey result; no instance was created: ${ended.failure.message}"
                    )
            }

    /**
     * The event stream of [journey], the last that ran by default; with none, everything the held
     * reporter received, or nothing under the default dispatcher.
     */
    fun stream(journey: Journey? = journeys.lastOrNull()): List<Event> =
        streamOf(model, recorders, journeys, journey)
}

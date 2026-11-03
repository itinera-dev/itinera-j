package org.itineradev.itinera.conformance

import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.run.Admission
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Journey
import org.itineradev.itinera.conformance.run.RanAlone
import org.itineradev.itinera.conformance.run.Recorders
import org.itineradev.itinera.conformance.run.Tested
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.Witness
import org.itineradev.itinera.conformance.run.admittedBy
import org.itineradev.itinera.conformance.run.asyncJourneys
import org.itineradev.itinera.conformance.run.localJourneys
import org.itineradev.itinera.conformance.run.streamOf
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.executor.testing.ScriptedRun
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Supplied

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

    /** The test support the scenario's journeys use. */
    val tested = Tested(model.testing)

    private var admitted: Admission? = null

    private var alone: RanAlone? = null

    private var supplied: Supplied? = null

    /** What the step or hook the scenario ran alone was lent and did. */
    var ranAlone: RanAlone
        get() = alone ?: throw ModelException("no step or hook ran alone")
        set(ran) {
            alone = ran
        }

    /** What the input adapter the scenario called alone supplied. */
    var returned: Supplied
        get() = supplied ?: throw ModelException("no input adapter was called alone")
        set(given) {
            supplied = given
        }

    /** Every journey the scenario ran, in order. */
    var journeys: List<Journey> = emptyList()
        private set

    /** Builds the workflow the scenario declares, in the scenario's column, once. */
    fun admit(): Admission = admitted ?: admittedInColumn().also(::keep)

    private fun admittedInColumn(): Admission =
        when (column) {
            Column.SYNCHRONOUS -> admittedBy { localJourneys(model, recorders, witness, tested) }
            Column.ASYNCHRONOUS -> admittedBy { asyncJourneys(model, recorders, witness, tested) }
        }

    private fun keep(admission: Admission) {
        admitted = admission
    }

    private var run: ScriptedRun? = null

    /** The record of the scripted run. */
    val scriptedRun: ScriptedRun
        get() = run ?: throw Unmet("expected a scripted run; ${journey.ended}")

    /** Runs one journey of the workflow with the scenario's script, once it is admitted. */
    fun runScripted() {
        val admission = admit()
        if (admission is Admission.Admitted) {
            val scripted = admission.journeys.runScripted()
            journeys = listOf(scripted.journey)
            run = scripted.run
        }
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
                is Ended.Stopped ->
                    throw Unmet(
                        "expected a journey result; the test was stopped: ${ended.mistake.message}"
                    )
            }

    /**
     * The event stream of [journey], the last that ran by default; with none, everything the held
     * reporter received, or nothing under the default dispatcher.
     */
    fun stream(journey: Journey? = journeys.lastOrNull()): List<Event> =
        streamOf(model, recorders, journeys, journey)
}

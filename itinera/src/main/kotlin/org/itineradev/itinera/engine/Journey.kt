package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.ContributionSource
import org.itineradev.itinera.event.DataOverwritten
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.step.StepName

/**
 * One journey while it runs: it holds its data bag and commits to it, makes its events and delivers
 * them through its dispatcher, knows the step being run, and records the journey's abort, the first
 * one standing.
 */
internal class Journey(
    private val emitter: Emitter,
    private val delivery: Delivery,
    private val failures: Failures,
    private val parts: Parts<*>,
    val data: DataBag,
) {
    var step: StepName? = null
    @Volatile private var recorded: Abort? = null

    /**
     * Makes and delivers one event. When a reporter or the dispatcher fails on it, or a dispatcher
     * swallows a reporter's failure, the journey is aborted with `reporter failed`.
     *
     * @throws JourneyAbortSignal once the journey is aborted.
     */
    @JvmSynthetic
    suspend fun emit(make: Stamp.() -> Event) {
        val event = emitter.next(make)
        val failedBefore = failures.first
        parts
            .calling { delivery.dispatch(event) }
            .onFailure { thrown -> throw reporterFailed(failures.first ?: thrown) }
        val failed = failures.first
        if (failed != null && failedBefore == null) throw reporterFailed(failed)
    }

    /**
     * Records [abort], unless one is recorded already, since the first stands, and gives the signal
     * that carries the abort recorded.
     */
    fun abort(abort: Abort): JourneyAbortSignal =
        JourneyAbortSignal(recorded ?: abort.also { first -> recorded = first })

    /**
     * Does nothing before the journey is aborted.
     *
     * @throws JourneyAbortSignal carrying the abort recorded, once the journey is aborted.
     */
    fun ensureNotAborted() {
        recorded?.let { abort -> throw JourneyAbortSignal(abort) }
    }

    /**
     * Commits [contributions], which [source] made, to the data bag, in order, emitting
     * `contribution_committed` for each key and `data_overwritten` after it when the key held a
     * value.
     *
     * @throws JourneyAbortSignal once the journey is aborted, which stops the commits.
     */
    @JvmSynthetic
    suspend fun commit(contributions: Map<String, Captured>, source: ContributionSource) {
        for ((key, value) in contributions) {
            val overwrites = key in data
            data.put(key, value)
            emit { ContributionCommitted(sequence, timestamp, journeyId, workflow, key, source) }
            if (overwrites) {
                emit { DataOverwritten(sequence, timestamp, journeyId, workflow, key, source) }
            }
        }
    }

    /**
     * Delivers `journey_aborted` for [abort], ignoring whatever fails while it is delivered, then
     * abandons the journey if its coroutine was cancelled meanwhile.
     */
    @JvmSynthetic
    suspend fun deliverAborted(abort: Abort) {
        val event = emitter.next {
            JourneyAborted(sequence, timestamp, journeyId, workflow, abort.details)
        }
        runCatching { delivery.dispatch(event) }.onFailure(::rethrowError)
        parts.ensureActive()
    }

    private fun reporterFailed(thrown: Throwable): JourneyAbortSignal =
        abort(Abort(ReporterFailed(step, messageOf(thrown)), thrown))
}

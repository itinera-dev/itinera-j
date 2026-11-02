package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ReporterFailure
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.testing.RecordingReporter

/** The name of the recording reporter the runner's own dispatchers hold. */
internal const val RUNNERS_RECORDER = "the runner's recorder"

/** An entry of global metadata a reporter received, and the kind of the last event before it. */
internal data class Entry(val key: String, val value: Any?, val after: EventKind?)

/**
 * A recording reporter of the scenario: it keeps every event and entry of global metadata it
 * receives, the one it fails on included, then fails as the scenario scripts it, and remembers
 * whether it ever failed.
 */
internal class ScenarioReporter(private val failure: ReporterFailure?) : Reporter {
    private val recording = RecordingReporter()
    private val received = mutableListOf<Entry>()

    /** Whether it failed on any event or entry of global metadata. */
    var failed = false
        private set

    /** Every event it received, in order. */
    val events: List<Event>
        get() = recording.events

    /** Every entry of global metadata it received, in order. */
    val entries: List<Entry>
        get() = received.toList()

    override fun report(event: Event) {
        val isFirst = recording.events.isEmpty()
        recording.report(event)
        if (failure != null && failure.failsOn(event, isFirst)) {
            failed = true
            throw ScriptedReporterFailure(failure.messageOn(event))
        }
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        received += Entry(key, value, recording.events.lastOrNull()?.kind)
        if (failure is ReporterFailure.OnGlobalMetadata && failure.key == key) {
            failed = true
            throw ScriptedReporterFailure("the scripted reporter fails on the global metadata $key")
        }
    }
}

/** A [ScenarioReporter] for asynchronous workflows, recording into [recorder]. */
internal class AsyncScenarioReporter(private val recorder: ScenarioReporter) : AsyncReporter {
    override suspend fun report(event: Event) {
        recorder.report(event)
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        recorder.setGlobalMetadata(key, value)
    }
}

private fun ReporterFailure.failsOn(event: Event, isFirst: Boolean): Boolean =
    when (this) {
        ReporterFailure.OnFirstEvent -> isFirst
        is ReporterFailure.OnEvent -> event.kind == kind
        is ReporterFailure.OnGlobalMetadata -> false
    }

private fun ReporterFailure.messageOn(event: Event): String =
    (this as? ReporterFailure.OnEvent)?.message ?: "the scripted reporter fails on ${event.kind}"

/** What a scripted reporter throws. */
internal class ScriptedReporterFailure(message: String) : RuntimeException(message)

/**
 * Every recording reporter of a scenario, by name, in the order each was made: one per journey for
 * a reporter the workflow lists, and one in all for the reporter a dispatcher holds.
 */
internal class Recorders(private val failures: Map<String, ReporterFailure>) {
    private val made = linkedMapOf<String, MutableList<ScenarioReporter>>()

    /** A new reporter under [name], failing as the scenario scripts it. */
    fun make(name: String): ScenarioReporter {
        val reporter = ScenarioReporter(failures[name])
        made.getOrPut(name, ::mutableListOf) += reporter
        return reporter
    }

    /** The reporters made under [name], in the order they were made. */
    fun made(name: String): List<ScenarioReporter> = made[name].orEmpty().toList()

    /** Every reporter made, by name in the order each name was first made. */
    val all: List<ScenarioReporter>
        get() = made.values.flatten()
}

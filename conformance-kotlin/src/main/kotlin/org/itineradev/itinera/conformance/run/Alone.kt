package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.Column
import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.journey.testing.Contributions
import org.itineradev.itinera.journey.testing.TestDataBag
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.step.testing.AsyncStepEventReporter
import org.itineradev.itinera.step.testing.StepEventReporter
import org.itineradev.itinera.workflow.Supplied

/** The recorder of test support that lent a reporter to what ran alone. */
internal enum class Recorder(private val written: String) {
    STEP("step"),
    HOOK("hook");

    override fun toString(): String = written
}

/**
 * What a step or a hook run alone, outside any journey, was lent and did: what the sentences name
 * as [called], the [recorder] that lent its reporter, the [contributions] recorder, what the
 * recorder kept, and the [calls] made on the reporter.
 */
internal class RanAlone(
    val called: String,
    val recorder: Recorder,
    val contributions: Contributions,
    val emitted: List<Emitted>,
    val globalMetadata: Map<String, Any?>,
    val calls: List<Call>,
)

/**
 * Builds the scripted step [step] outside any journey, in [column]'s mode, and runs it, doing what
 * it does on attempt 1, with a contributor lent by the contributions recorder and a step reporter
 * lent by the step event reporter, failing when [model] says so.
 *
 * @throws ModelException when the step requests an input or a service, which a step run alone is
 *   not given.
 */
internal fun stepRunAlone(model: Model, step: String, column: Column): RanAlone {
    val script = model.step(step)
    if (script.inputs.isNotEmpty() || script.services.isNotEmpty()) {
        throw ModelException("step \"$step\" requests what a step run alone is not given")
    }
    val failing = model.testing.stepRecorderFails
    val called = "step \"$step\""
    return when (column) {
        Column.SYNCHRONOUS -> stepAlone(script, failing, called)
        Column.ASYNCHRONOUS -> runBlocking { asyncStepAlone(script, failing, called) }
    }
}

private fun stepAlone(script: StepScript, failing: Boolean, called: String): RanAlone {
    val contributions = Contributions()
    val events = if (failing) StepEventReporter.failing() else StepEventReporter()
    val calls = CarryingOn()
    val reporter = CarriedOnStepReporter(events.reporter, calls)
    val received =
        Received<StepReporter>(emptyMap(), emptyMap(), contributions.contributor, reporter, 0, NONE)
    for (action in Scripted(script, Requests.stepReporter()).prepared(received)) {
        perform(action, received, ignoresEmitFailures = false)
    }
    return RanAlone(
        called,
        Recorder.STEP,
        contributions,
        events.emitted,
        events.globalMetadata,
        calls.calls,
    )
}

private suspend fun asyncStepAlone(
    script: StepScript,
    failing: Boolean,
    called: String,
): RanAlone {
    val contributions = Contributions()
    val events = if (failing) AsyncStepEventReporter.failing() else AsyncStepEventReporter()
    val calls = CarryingOn()
    val reporter = CarriedOnAsyncStepReporter(events.reporter, calls)
    val received =
        Received<AsyncStepReporter>(
            emptyMap(),
            emptyMap(),
            contributions.contributor,
            reporter,
            0,
            NONE,
        )
    for (action in Scripted(script, Requests.asyncStepReporter()).prepared(received)) {
        performAsync(action, received, ignoresEmitFailures = false)
    }
    return RanAlone(
        called,
        Recorder.STEP,
        contributions,
        events.emitted,
        events.globalMetadata,
        calls.calls,
    )
}

/**
 * Calls the input adapter [adapter] outside any journey for [input] of [step], with the read access
 * of a data bag holding [values], built with test support, and gives what it supplied.
 */
internal fun adapterCalledAlone(
    model: Model,
    adapter: String,
    step: String,
    input: String,
    values: List<Pair<String, Any?>>,
): Supplied {
    val script =
        model.workflow.adapters.find { declared -> isNamed(declared, adapter) }
            ?: throw ModelException("the workflow has no input adapter \"$adapter\"")
    val bag = values.fold(TestDataBag(), ::holding)
    return Adapting(script).suppliedAlone(step, input, bag.access)
}

private fun isNamed(adapter: AdapterModel, name: String): Boolean = adapter.name == name

private fun holding(bag: TestDataBag, entry: Pair<String, Any?>): TestDataBag =
    bag.with(entry.first, entry.second)

/** What a rescue step requests of the step it rescues, which a step run alone is not. */
private val NONE = emptyMap<HookRequest, Any?>()

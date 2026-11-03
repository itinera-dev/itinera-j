package org.itineradev.itinera.conformance.model

/** What a fake put in place of a service provider does on each call. */
internal sealed interface FakeModel {
    /** It makes an instance of [implementation] of `labeller`, with [data] when one is written. */
    data class Makes(val implementation: Implementation, val data: Any?) : FakeModel

    /** It fails with [message]. */
    data class Fails(val message: String) : FakeModel

    /** It makes a `meter`, another service than a provider of a `labeller` makes. */
    data object MakesMeters : FakeModel
}

/**
 * What a scenario of test support declares: whether the instance has the recording reporter of test
 * support, the fakes put in place of service providers, by the provider's name, and which recorders
 * lent outside a journey fail.
 */
internal class TestSupport {
    /** Whether the instance has the recording reporter of test support. */
    var recordingReporter = false
        private set

    /** The fakes, by the name of the provider each replaces, in the order written. */
    val fakes = linkedMapOf<String, FakeModel>()

    /** Whether the step event reporter is failing. */
    var stepRecorderFails = false
        private set

    /** Whether the hook event reporter is failing. */
    var hookRecorderFails = false
        private set

    /** The rows of the script, in the order written, once it is written. */
    var script: List<ScriptRow>? = null
        private set

    /** Whether the script ensures every step is scripted. */
    var ensuresAllScripted = false
        private set

    /** Whether the instance is created through test support. */
    val createsTheInstance: Boolean
        get() = recordingReporter || fakes.isNotEmpty()

    /** Gives the instance the recording reporter of test support. */
    fun record() {
        recordingReporter = true
    }

    /** Puts [fake] in place of the provider named [provider], once. */
    fun fake(provider: String, fake: FakeModel) {
        if (provider in fakes) throw ModelException("the provider \"$provider\" is faked twice")
        fakes[provider] = fake
    }

    /** Writes the script, once. */
    fun script(rows: List<ScriptRow>) {
        if (script != null) throw ModelException("the script is written twice")
        script = rows
    }

    /** Makes the script ensure every step is scripted. */
    fun ensureAllScripted() {
        ensuresAllScripted = true
    }

    /** Makes the step event reporter fail. */
    fun failStepRecorder() {
        stepRecorderFails = true
    }

    /** Makes the hook event reporter fail. */
    fun failHookRecorder() {
        hookRecorderFails = true
    }
}

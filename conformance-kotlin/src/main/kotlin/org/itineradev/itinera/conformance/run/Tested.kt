package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.FakeModel
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.TestSupport
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.testing.AsyncFake
import org.itineradev.itinera.instance.testing.AsyncTestInstanceBuilder
import org.itineradev.itinera.instance.testing.Fake
import org.itineradev.itinera.instance.testing.TestInstanceBuilder
import org.itineradev.itinera.report.testing.AsyncRecordingReporter
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** What the recording reporter of test support kept: its events, global metadata and path. */
internal class Kept(
    val events: List<Event>,
    val globalMetadata: Map<String, Any?>,
    val path: List<String>,
)

/**
 * A fake put in place of a service provider, doing as [fake] says and counting its calls; the
 * `labeller` it makes on call n is labelled `fake n`.
 */
internal class CountedFake(private val fake: FakeModel) {
    /** How many times it was called, whether it made an instance or failed. */
    var calls = 0
        private set

    /** The service it makes. */
    val service: Class<*> =
        if (fake == FakeModel.MakesMeters) Meter::class.java else Labeller::class.java

    /** What it provides on its next call, of [type], the service it makes. */
    fun <T : Any> provided(type: Class<T>): Provided<T> {
        calls += 1
        return when (fake) {
            is FakeModel.Makes ->
                Provided.Instance(
                    type.cast(labellerOf(fake.implementation, "fake $calls", null)),
                    fake.data,
                )
            is FakeModel.Fails -> throw IllegalStateException(fake.message)
            FakeModel.MakesMeters -> Provided.Instance(type.cast(Meter { 0 }))
        }
    }
}

/**
 * The test support a scenario's journeys use, as [testing] declares: for the last instance created,
 * what its recording reporter kept and its fakes.
 */
internal class Tested(private val testing: TestSupport) {
    private var keeping: (() -> Kept)? = null

    /** The fakes of the last instance, by the name of the provider each replaces. */
    var fakes: Map<String, CountedFake> = emptyMap()
        private set

    /** What the recording reporter of the last instance kept. */
    val kept: Kept
        get() =
            keeping?.invoke()
                ?: throw Unmet("expected the recording reporter of test support; none was made")

    /**
     * The instance of the workflow [descriptor] describes, created through test support for
     * [scenario] as [model] declares, after the reporters the workflow lists, with the recording
     * reporter of test support and the fakes.
     */
    fun instance(
        descriptor: WorkflowDescriptor<Scenario, Sync>,
        scenario: Scenario,
        model: Model,
    ): WorkflowInstance<Sync> {
        val builder = TestInstanceBuilder.of(descriptor, scenario).initialData(model.initialData)
        for ((key, value) in model.initialGlobalMetadata) {
            builder.reportersGlobalMetadata(key, value)
        }
        if (testing.recordingReporter) {
            val recording = RecordingReporter()
            builder.reporter(recording)
            keeping = { Kept(recording.events, recording.globalMetadata, recording.path) }
        }
        fakes = testing.fakes.mapValues { entry -> CountedFake(entry.value) }
        for ((provider, fake) in fakes) builder.faking(provider, fake.service, fake)
        return builder.create()
    }

    /** The asynchronous twin of [instance]. */
    fun asyncInstance(
        descriptor: WorkflowDescriptor<Scenario, Async>,
        scenario: Scenario,
        model: Model,
    ): WorkflowInstance<Async> {
        val builder =
            AsyncTestInstanceBuilder.of(descriptor, scenario).initialData(model.initialData)
        for ((key, value) in model.initialGlobalMetadata) {
            builder.reportersGlobalMetadata(key, value)
        }
        if (testing.recordingReporter) {
            val recording = AsyncRecordingReporter()
            builder.reporter(recording)
            keeping = { Kept(recording.events, recording.globalMetadata, recording.path) }
        }
        fakes = testing.fakes.mapValues { entry -> CountedFake(entry.value) }
        for ((provider, fake) in fakes) builder.faking(provider, fake.service, fake)
        return builder.create()
    }
}

private fun <T : Any> TestInstanceBuilder.faking(
    provider: String,
    service: Class<T>,
    fake: CountedFake,
) {
    fakeProvider(provider, service, Fake { fake.provided(service) })
}

private fun <T : Any> AsyncTestInstanceBuilder.faking(
    provider: String,
    service: Class<T>,
    fake: CountedFake,
) {
    fakeProvider(provider, service, AsyncFake { fake.provided(service) })
}

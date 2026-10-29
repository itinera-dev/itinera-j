package org.itineradev.itinera.instance.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Draft
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterFactory
import org.itineradev.itinera.service.SyncProvider
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * Creates the instance of a synchronous workflow for a test, as `InstanceBuilder` does, and can
 * also put a test's own [Fake] in place of a service provider's function, for this instance alone.
 * [AsyncTestInstanceBuilder] is its twin for asynchronous workflows.
 *
 * A fake replaces only the provider's function: the provider keeps its scope, the steps it serves
 * and its requests, which the executor resolves first, so missing data aborts the journey as
 * without a fake, before the fake is called wherever and as often as the provider would be.
 * `service_provided` names the implementation the fake made, and carries its data. The workflow's
 * declaration, its admission and its listing are unchanged.
 *
 * @sample org.itineradev.itinera.instance.testing.samples.fakes
 */
@ExperimentalItineraApi
public class TestInstanceBuilder
private constructor(private val draft: Draft<*, Sync>, descriptor: WorkflowDescriptor<*, Sync>) {
    private val fakes = Fakes(descriptor)

    /**
     * Puts [value] in the initial data under [key], replacing what was put there before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun data(key: String, value: Any?): TestInstanceBuilder = apply {
        draft.data(key, value)
    }

    /**
     * Puts every entry of [data] in the initial data, in the map's order, replacing what was put
     * under its key before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun initialData(data: Map<String, Any?>): TestInstanceBuilder = apply {
        data.forEach(draft::data)
    }

    /**
     * Puts each component of [data], a record or data class, in the initial data under its key, as
     * `InstanceBuilder.initialData` does.
     *
     * @throws IllegalArgumentException as `InstanceBuilder.initialData` says.
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun initialData(data: Any): TestInstanceBuilder = apply { draft.initialData(data) }

    /**
     * Adds a reporter after those already added.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun reporter(reporter: Reporter): TestInstanceBuilder = apply {
        draft.reporter { reporter }
    }

    /**
     * Adds the reporter [factory] makes for the journey's ID, once the ID is produced, after those
     * already added.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun reporterFor(factory: ReporterFactory): TestInstanceBuilder = apply {
        draft.reporter(factory::create)
    }

    /**
     * Sets [value] as the global metadata under [key], which the reporters receive after
     * `journey_id` and before the journey starts, replacing what was set under it before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun reportersGlobalMetadata(key: String, value: Any?): TestInstanceBuilder = apply {
        draft.metadata(key, value)
    }

    /**
     * Puts [fake] in place of the function of the workflow's service provider named [name], which
     * makes [service], replacing the fake put there before.
     *
     * @throws TestMistake when the workflow has no service provider named [name], or that provider
     *   makes another service than [service].
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun <T : Any> fakeProvider(
        name: String,
        service: Class<T>,
        fake: Fake<T>,
    ): TestInstanceBuilder = apply { fakes.put(name, service, SyncFakeCall(fake)) }

    /**
     * The instance, created as `InstanceBuilder.create` creates it, whose journey calls the fakes
     * in place of the providers they replace.
     *
     * @throws IllegalStateException as `InstanceBuilder.create` says.
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun create(): WorkflowInstance<Sync> = fakes.of(draft.create())

    /** Starts instances. */
    public companion object {
        /**
         * Starts an instance of the synchronous workflow [workflow], an annotated class, reading
         * its annotations as `instance()` on it does.
         *
         * @throws org.itineradev.itinera.workflow.Violations with every mistake in the class's
         *   declaration.
         * @sample org.itineradev.itinera.instance.testing.samples.fakes
         */
        @JvmStatic
        public fun of(workflow: ItineraWorkflow): TestInstanceBuilder =
            of(WorkflowDescriptor.of(workflow.javaClass), workflow)

        /**
         * Starts an instance of a synchronous workflow that [descriptor] describes, whose parts are
         * called with [workflow].
         *
         * @sample org.itineradev.itinera.instance.testing.samples.fakes
         */
        @JvmStatic
        public fun <W> of(
            descriptor: WorkflowDescriptor<W, Sync>,
            workflow: W,
        ): TestInstanceBuilder = TestInstanceBuilder(Draft(descriptor, workflow), descriptor)
    }
}

/** How a synchronous [fake] is called in place of a provider's call. */
private class SyncFakeCall<T : Any>(private val fake: Fake<T>) : FakeCall<Sync> {
    override fun <W, K> call(): SyncProvider<W, K, T> = SyncProvider { _, _ -> fake.provide() }
}

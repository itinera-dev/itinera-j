package org.itineradev.itinera.instance.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Draft
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.async.AsyncReporterFactory
import org.itineradev.itinera.service.async.AsyncProvider
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf

/**
 * Creates the instance of an asynchronous workflow for a test, as `AsyncInstanceBuilder` does, and
 * can also put a test's own [AsyncFake] in place of a service provider's function, for this
 * instance alone, as [TestInstanceBuilder] does for a synchronous workflow. It needs
 * `itinera-kotlin`.
 *
 * A fake replaces only the provider's function: the provider keeps its scope, the steps it serves
 * and its requests, which the executor resolves first, so missing data aborts the journey as
 * without a fake, before the fake is called wherever and as often as the provider would be.
 * `service_provided` names the implementation the fake made, and carries its data. The workflow's
 * declaration, its admission and its listing are unchanged.
 *
 * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
 */
@ExperimentalItineraApi
public class AsyncTestInstanceBuilder
private constructor(private val draft: Draft<*, Async>, descriptor: WorkflowDescriptor<*, Async>) {
    private val fakes = Fakes(descriptor)

    /**
     * Puts [value] in the initial data under [key], replacing what was put there before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun data(key: String, value: Any?): AsyncTestInstanceBuilder = apply {
        draft.data(key, value)
    }

    /**
     * Puts every entry of [data] in the initial data, in the map's order, replacing what was put
     * under its key before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun initialData(data: Map<String, Any?>): AsyncTestInstanceBuilder = apply {
        data.forEach(draft::data)
    }

    /**
     * Puts each component of [data], a record or data class, in the initial data under its key, as
     * `AsyncInstanceBuilder.initialData` does.
     *
     * @throws IllegalArgumentException as `AsyncInstanceBuilder.initialData` says.
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun initialData(data: Any): AsyncTestInstanceBuilder = apply { draft.initialData(data) }

    /**
     * Adds a reporter after those already added.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun reporter(reporter: AsyncReporter): AsyncTestInstanceBuilder = apply {
        draft.reporter { reporter }
    }

    /**
     * Adds the reporter [factory] makes for the journey's ID, once the ID is produced, after those
     * already added.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun reporterFor(factory: AsyncReporterFactory): AsyncTestInstanceBuilder = apply {
        draft.reporter(factory::create)
    }

    /**
     * Sets [value] as the global metadata under [key], which the reporters receive after
     * `journey_id` and before the journey starts, replacing what was set under it before.
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun reportersGlobalMetadata(key: String, value: Any?): AsyncTestInstanceBuilder = apply {
        draft.metadata(key, value)
    }

    /**
     * Puts [fake] in place of the function of the workflow's service provider named [name], which
     * makes [service], replacing the fake put there before.
     *
     * @throws TestMistake when the workflow has no service provider named [name], or that provider
     *   makes another service than [service].
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun <T : Any> fakeProvider(
        name: String,
        service: Class<T>,
        fake: AsyncFake<T>,
    ): AsyncTestInstanceBuilder = apply { fakes.put(name, service, AsyncFakeCall(fake)) }

    /**
     * The instance, created as `AsyncInstanceBuilder.create` creates it, whose journey calls the
     * fakes in place of the providers they replace.
     *
     * @throws IllegalStateException as `AsyncInstanceBuilder.create` says.
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    public fun create(): WorkflowInstance<Async> = fakes.of(draft.create())

    /** Starts instances. */
    public companion object {
        /**
         * Starts an instance of the asynchronous workflow [workflow], an annotated class, reading
         * its annotations as `instance()` on it does.
         *
         * @throws org.itineradev.itinera.workflow.Violations with every mistake in the class's
         *   declaration.
         * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
         */
        public fun of(workflow: ItineraAsyncWorkflow): AsyncTestInstanceBuilder =
            of(asyncDescriptorOf(workflow.javaClass.kotlin), workflow)

        /**
         * Starts an instance of an asynchronous workflow that [descriptor] describes, whose parts
         * are called with [workflow].
         *
         * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
         */
        public fun <W> of(
            descriptor: WorkflowDescriptor<W, Async>,
            workflow: W,
        ): AsyncTestInstanceBuilder =
            AsyncTestInstanceBuilder(Draft(descriptor, workflow), descriptor)
    }
}

/** How an asynchronous [fake] is called in place of a provider's call. */
private class AsyncFakeCall<T : Any>(private val fake: AsyncFake<T>) : FakeCall<Async> {
    override fun <W, K> call(): AsyncProvider<W, K, T> = AsyncProvider { _, _ -> fake.provide() }
}

package org.itineradev.itinera.instance.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Draft
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.async.AsyncReporterFactory
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * Creates the instance of an asynchronous workflow: what its journey starts with, and who reports
 * it. `instance()` on a workflow object starts one, and [of] starts one for a descriptor the
 * builder made.
 *
 * Nothing is checked until [create], which captures the initial data, then produces the journey ID,
 * then makes the reporters, in that order; the executor refuses initial data or global metadata
 * that are not values, listing every key. Creating an instance is an ordinary function in both
 * modes, so the ID generator and the reporter factories are too.
 *
 * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
 */
public class AsyncInstanceBuilder internal constructor(private val draft: Draft<*, Async>) {
    /**
     * Puts [value] in the initial data under [key], replacing what was put there before.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
     */
    public fun data(key: String, value: Any?): AsyncInstanceBuilder = apply {
        draft.data(key, value)
    }

    /**
     * Puts every entry of [data] in the initial data, in the map's order, replacing what was put
     * under its key before.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
     */
    public fun initialData(data: Map<String, Any?>): AsyncInstanceBuilder = apply {
        data.forEach(draft::data)
    }

    /**
     * Puts each component of [data], a data class or record, in the initial data under its key, in
     * the components' order, replacing what was put under its key before. The key is the
     * component's name, unless `@Datum` names another, and an optional component, as `@Datum` says,
     * gives no key when it holds `null`.
     *
     * @throws IllegalArgumentException when [data] is not a data class or a record, when two of its
     *   components give one key, or when no value can be of a component's type.
     * @sample org.itineradev.itinera.instance.async.samples.typedAsyncInstances
     */
    public fun initialData(data: Any): AsyncInstanceBuilder = apply { draft.initialData(data) }

    /**
     * Adds a reporter after those already added.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
     */
    public fun reporter(reporter: AsyncReporter): AsyncInstanceBuilder = apply {
        draft.reporter { reporter }
    }

    /**
     * Adds the reporter [factory] makes for the journey's ID, once the ID is produced, after those
     * already added.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncPerJourney
     */
    public fun reporterFor(factory: AsyncReporterFactory): AsyncInstanceBuilder = apply {
        draft.reporter(factory::create)
    }

    /**
     * Sets [value] as the global metadata under [key], which the reporters receive after
     * `journey_id` and before the journey starts, replacing what was set under it before.
     *
     * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
     */
    @ExperimentalItineraApi
    public fun reportersGlobalMetadata(key: String, value: Any?): AsyncInstanceBuilder = apply {
        draft.metadata(key, value)
    }

    /**
     * The instance: captures the initial data, then produces the journey ID with the workflow's ID
     * generator, or a UUID version 4 when it has none, then makes the reporters.
     *
     * @throws IllegalStateException when the ID generator throws or returns an empty or `null` ID,
     *   or a reporter factory throws or returns `null`, with no journey and no event.
     * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
     */
    public fun create(): WorkflowInstance<Async> = draft.create()

    /** Starts instances. */
    public companion object {
        /**
         * Starts an instance of an asynchronous workflow that [descriptor] describes, whose parts
         * are called with [workflow].
         *
         * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
         */
        public fun <W> of(
            descriptor: WorkflowDescriptor<W, Async>,
            workflow: W,
        ): AsyncInstanceBuilder = AsyncInstanceBuilder(Draft(descriptor, workflow))
    }
}

/**
 * Starts an instance of the asynchronous workflow this descriptor describes, whose parts are called
 * with [workflow], as [AsyncInstanceBuilder.of] does.
 *
 * @sample org.itineradev.itinera.instance.async.samples.asyncInstances
 */
public fun <W> WorkflowDescriptor<W, Async>.instance(workflow: W): AsyncInstanceBuilder =
    AsyncInstanceBuilder.of(this, workflow)

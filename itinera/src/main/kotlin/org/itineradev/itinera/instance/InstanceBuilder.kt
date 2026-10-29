package org.itineradev.itinera.instance

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Draft
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterFactory
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * Creates the instance of a synchronous workflow: what its journey starts with, and who reports it.
 * `instance()` on a workflow object starts one, and [of] starts one for a descriptor the builder
 * made.
 *
 * Nothing is checked until [create], which captures the initial data, then produces the journey ID,
 * then makes the reporters, in that order; the executor refuses initial data or global metadata
 * that are not values, listing every key.
 *
 * @sample org.itineradev.itinera.instance.samples.instances
 */
public class InstanceBuilder internal constructor(private val draft: Draft<*, Sync>) {
    /**
     * Puts [value] in the initial data under [key], replacing what was put there before.
     *
     * @sample org.itineradev.itinera.instance.samples.instances
     */
    public fun data(key: String, value: Any?): InstanceBuilder = apply { draft.data(key, value) }

    /**
     * Puts every entry of [data] in the initial data, in the map's order, replacing what was put
     * under its key before.
     *
     * @sample org.itineradev.itinera.instance.samples.instances
     */
    public fun initialData(data: Map<String, Any?>): InstanceBuilder = apply {
        data.forEach(draft::data)
    }

    /**
     * Puts each component of [data], a record or data class, in the initial data under its key, in
     * the components' order, replacing what was put under its key before. The key is the
     * component's name, unless `@Datum` names another, and an optional component, as `@Datum` says,
     * gives no key when it holds `null` or an empty `Optional`.
     *
     * @throws IllegalArgumentException when [data] is not a record or a data class, or a Kotlin
     *   data class without `itinera-kotlin`; when two of its components give one key; when it marks
     *   an `Optional` component required, or has one without `itinera-java`; or when no value can
     *   be of a component's type.
     * @sample org.itineradev.itinera.instance.samples.typedInstances
     */
    public fun initialData(data: Any): InstanceBuilder = apply { draft.initialData(data) }

    /**
     * Adds a reporter after those already added.
     *
     * @sample org.itineradev.itinera.instance.samples.instances
     */
    public fun reporter(reporter: Reporter): InstanceBuilder = apply {
        draft.reporter { reporter }
    }

    /**
     * Adds the reporter [factory] makes for the journey's ID, once the ID is produced, after those
     * already added.
     *
     * @sample org.itineradev.itinera.report.samples.perJourney
     */
    public fun reporterFor(factory: ReporterFactory): InstanceBuilder = apply {
        draft.reporter(factory)
    }

    /**
     * Sets [value] as the global metadata under [key], which the reporters receive after
     * `journey_id` and before the journey starts, replacing what was set under it before.
     *
     * @sample org.itineradev.itinera.instance.samples.instances
     */
    @ExperimentalItineraApi
    public fun reportersGlobalMetadata(key: String, value: Any?): InstanceBuilder = apply {
        draft.metadata(key, value)
    }

    /**
     * The instance: captures the initial data, then produces the journey ID with the workflow's ID
     * generator, or a UUID version 4 when it has none, then makes the reporters.
     *
     * @throws IllegalStateException when the ID generator throws or returns an empty or `null` ID,
     *   or a reporter factory throws or returns `null`, with no journey and no event.
     * @sample org.itineradev.itinera.instance.samples.instances
     */
    public fun create(): WorkflowInstance<Sync> = draft.create()

    /** Starts instances. */
    public companion object {
        /**
         * Starts an instance of a synchronous workflow that [descriptor] describes, whose parts are
         * called with [workflow].
         *
         * @sample org.itineradev.itinera.instance.samples.instances
         */
        @JvmStatic
        public fun <W> of(descriptor: WorkflowDescriptor<W, Sync>, workflow: W): InstanceBuilder =
            InstanceBuilder(Draft(descriptor, workflow))
    }
}

/**
 * Starts an instance of the synchronous workflow this descriptor describes, whose parts are called
 * with [workflow], as [InstanceBuilder.of] does.
 *
 * @sample org.itineradev.itinera.instance.samples.instances
 */
@JvmSynthetic
public fun <W> WorkflowDescriptor<W, Sync>.instance(workflow: W): InstanceBuilder =
    InstanceBuilder.of(this, workflow)

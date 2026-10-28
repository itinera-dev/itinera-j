package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.service.ProviderDescriptor

/**
 * A workflow's declaration, fixed once built and shared by every instance: its name, its mode, its
 * steps in order, its workflow policies, the roles it provides, its input adapters, its service
 * providers, and how each instance's journey ID is produced. It holds descriptions and ways to
 * build each part, never the parts themselves, which the executor builds for every journey.
 *
 * [builder] makes one for a synchronous workflow, and [of] reads one from an annotated class. Any
 * other implementation runs too: the executor checks it for every violation before the journey.
 *
 * @param W the class of the workflow object each instance is created with.
 * @param M the workflow's mode.
 * @sample org.itineradev.itinera.workflow.samples.building
 */
@ExperimentalItineraApi
public interface WorkflowDescriptor<W, M : Mode> {
    /** The workflow's name, which every event of its journeys carries. */
    public val name: WorkflowName

    /** The workflow's mode, which every part shares. */
    public val mode: M

    /** The step descriptors, in the order the steps run. */
    public val steps: List<StepDescriptor<M>>

    /** The workflow policies attached to the workflow, in the order their hooks are called. */
    public val policies: List<WorkflowPolicyDescriptor<*, M>>

    /**
     * The roles the workflow provides: interfaces marked `@Role`, which every workflow object of
     * its instances implements, and whose operations its policies' hooks may call.
     */
    public val roles: List<Class<in W>>

    /** The input adapters, each attached to the steps whose inputs it supplies. */
    public val inputAdapters: List<InputAdapterDescriptor<W, M>>

    /** The service providers, which supply the services the steps request. */
    public val providers: List<ProviderDescriptor<W, M>>

    /** How each instance's journey ID is produced; `null` for a UUID version 4. */
    public val idGenerator: JourneyIdGenerator<W>?

    /** Makes workflow descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous workflow named [name], whose instances are created
         * with objects of [type], which only fixes the builder's type.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.workflow.samples.building
         */
        @JvmStatic
        public fun <W> builder(
            @Suppress("UNUSED_PARAMETER") type: Class<W>,
            name: String,
        ): WorkflowBuilder<W, Sync> = WorkflowBuilder(WorkflowName.of(name), Sync)

        /**
         * The descriptor of a synchronous workflow written as an annotated class, read once and
         * then shared by every caller. A Kotlin class needs `itinera-kotlin` to be read.
         *
         * @throws Violations with every mistake in the class's declaration.
         * @sample org.itineradev.itinera.workflow.annotated.samples.describing
         */
        @JvmStatic
        public fun <W : ItineraWorkflow> of(type: Class<W>): WorkflowDescriptor<W, Sync> =
            Described.synchronous(type)
    }
}

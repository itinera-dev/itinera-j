package org.itineradev.itinera.workflow

import org.itineradev.itinera.engine.Checked
import org.itineradev.itinera.engine.isRole
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.report.ReporterFactoryOf
import org.itineradev.itinera.service.ProviderDescriptor

/**
 * Builds a [WorkflowDescriptor] of mode [M], whose parts it takes only in that mode.
 *
 * @param W the class of the workflow object each instance is created with.
 * @param M the workflow's mode.
 * @sample org.itineradev.itinera.workflow.samples.building
 */
public class WorkflowBuilder<W, M : Mode>
internal constructor(private val name: WorkflowName, private val mode: M) {
    private val steps = mutableListOf<StepDescriptor<M>>()
    private val policies = mutableListOf<WorkflowPolicyDescriptor<*, M>>()
    private val roles = mutableListOf<Class<in W>>()
    private val inputAdapters = mutableListOf<InputAdapterDescriptor<W, M>>()
    private val providers = mutableListOf<ProviderDescriptor<W, M>>()
    private val reporters = mutableListOf<ReporterFactoryOf<M>>()
    private var idGenerator: JourneyIdGenerator<W>? = null

    /**
     * Adds a step after those already added.
     *
     * @sample org.itineradev.itinera.workflow.samples.building
     */
    public fun step(step: StepDescriptor<M>): WorkflowBuilder<W, M> = apply { steps += step }

    /**
     * Attaches a workflow policy after those already attached, whose hooks are called in that
     * order.
     *
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun policy(policy: WorkflowPolicyDescriptor<*, M>): WorkflowBuilder<W, M> = apply {
        policies += policy
    }

    /**
     * Provides the role [type], an interface marked `@Role` that every workflow object of the
     * workflow's instances implements, so its policies' hooks may request it.
     *
     * @throws IllegalArgumentException when [type] is not an interface marked `@Role`.
     * @sample org.itineradev.itinera.policy.samples.notifying
     */
    public fun role(type: Class<in W>): WorkflowBuilder<W, M> = apply {
        require(isRole(type)) { "A role is an interface marked @Role, which ${type.name} is not." }
        roles += type
    }

    /**
     * Adds an input adapter after those already added, which supplies the inputs of the steps it is
     * attached to.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public fun inputAdapter(adapter: InputAdapterDescriptor<W, M>): WorkflowBuilder<W, M> = apply {
        inputAdapters += adapter
    }

    /**
     * Adds a service provider after those already added, which supplies its service to the steps it
     * serves.
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public fun provider(provider: ProviderDescriptor<W, M>): WorkflowBuilder<W, M> = apply {
        providers += provider
    }

    /**
     * Lists the reporter [factory] makes, after those already listed. Each instance makes its own
     * with it, given its journey ID, before the reporters it is given itself. From Java, a factory
     * whose reporter is itself a lambda is written as a `ReporterFactory`, such as `ReporterFactory
     * printing = id -> event -> …`.
     *
     * @sample org.itineradev.itinera.report.samples.listed
     */
    public fun reporter(factory: ReporterFactoryOf<M>): WorkflowBuilder<W, M> = apply {
        reporters += factory
    }

    /**
     * Produces each instance's journey ID with this generator, in place of a UUID version 4.
     *
     * @sample org.itineradev.itinera.workflow.samples.ids
     */
    public fun idGenerator(generator: JourneyIdGenerator<W>): WorkflowBuilder<W, M> = apply {
        idGenerator = generator
    }

    /**
     * The descriptor, once every violation is checked.
     *
     * @throws Violations with every violation found.
     * @sample org.itineradev.itinera.workflow.samples.building
     */
    public fun build(): WorkflowDescriptor<W, M> =
        Checked.of(
            name,
            mode,
            steps,
            policies,
            roles,
            inputAdapters,
            providers,
            reporters,
            idGenerator,
        )
}

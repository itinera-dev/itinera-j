package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Checked
import org.itineradev.itinera.engine.isRole
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor

/**
 * Builds a [WorkflowDescriptor] of mode [M], whose parts it takes only in that mode.
 *
 * @param W the class of the workflow object each instance is created with.
 * @param M the workflow's mode.
 * @sample org.itineradev.itinera.workflow.samples.building
 */
@ExperimentalItineraApi
public class WorkflowBuilder<W, M : Mode>
internal constructor(private val name: WorkflowName, private val mode: M) {
    private val steps = mutableListOf<StepDescriptor<M>>()
    private val policies = mutableListOf<WorkflowPolicyDescriptor<*, M>>()
    private val roles = mutableListOf<Class<in W>>()
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
        Checked.of(name, mode, steps, policies, roles, idGenerator)
}

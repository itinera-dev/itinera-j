package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.JourneyIdGenerator
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.WorkflowName

/**
 * A workflow descriptor checked for every violation when it was made, which the executor therefore
 * does not check again.
 *
 * Part of itinera's engine, not of its API.
 */
public class Checked<W, M : Mode>
private constructor(
    override val name: WorkflowName,
    override val mode: M,
    override val steps: List<StepDescriptor<M>>,
    override val policies: List<WorkflowPolicyDescriptor<*, M>>,
    override val roles: List<Class<in W>>,
    override val idGenerator: JourneyIdGenerator<W>?,
) : WorkflowDescriptor<W, M> {
    /** Makes checked descriptors. */
    public companion object {
        /**
         * The descriptor, once every violation is checked.
         *
         * @throws Violations with every violation found.
         */
        public fun <W, M : Mode> of(
            name: WorkflowName,
            mode: M,
            steps: List<StepDescriptor<M>>,
            policies: List<WorkflowPolicyDescriptor<*, M>>,
            roles: List<Class<in W>>,
            idGenerator: JourneyIdGenerator<W>?,
        ): Checked<W, M> {
            val described =
                Checked(name, mode, steps.toList(), policies.toList(), roles.toList(), idGenerator)
            val violations = violationsOf(described)
            if (violations.isNotEmpty()) throw Violations(violations)
            return described
        }
    }
}

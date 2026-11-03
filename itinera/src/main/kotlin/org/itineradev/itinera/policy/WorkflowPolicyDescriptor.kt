package org.itineradev.itinera.policy

import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations

/**
 * How one workflow policy of workflows of mode [M] is declared: its name, how it is built, and the
 * workflow hooks it defines, each with what it requests. It defines at least one hook, and each
 * hook at most once. It is attached to a workflow with the workflow builder's `policy`.
 *
 * [builder] makes one for a synchronous workflow, [of] reads one from a class marked
 * `@WorkflowPolicy`, and `asyncWorkflowPolicy` and `asyncWorkflowPolicyOf` from `itinera-kotlin` do
 * the same for an asynchronous one. Any other implementation runs too, checked by the executor
 * before the journey.
 *
 * @param P the policy its hooks are called on.
 * @param M the mode of the workflows it can join.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
public interface WorkflowPolicyDescriptor<P : Any, M : Mode> {
    /** The policy's name. */
    public val name: PolicyName

    /** How the policy is built, once per journey. */
    public val factory: PolicyFactory<P>

    /** Its `on workflow success`, or `null` when it does not define it. */
    public val onWorkflowSuccess: HookDefinition<M, P, WorkflowHookKind<M>, Unit>?

    /** Its `on workflow failure`, or `null` when it does not define it. */
    public val onWorkflowFailure: HookDefinition<M, P, WorkflowHookKind<M>, Unit>?

    /** Makes workflow policy descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous workflow policy named [name], which [factory]
         * builds once per journey.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.policy.samples.policies
         */
        @JvmStatic
        public fun <P : Any> builder(
            name: String,
            factory: PolicyFactory<P>,
        ): WorkflowPolicyBuilder<P> =
            WorkflowPolicyBuilder(PolicyDraft(PolicyName.of(name), factory))

        /**
         * The descriptor of a synchronous workflow policy written as a class marked
         * `@WorkflowPolicy`, named as it says, built with its constructor once per journey, and
         * whose hooks are its methods marked with a hook's annotation. A Kotlin class needs
         * `itinera-kotlin` to be read.
         *
         * @throws Violations with every mistake in the class's declaration.
         * @sample org.itineradev.itinera.workflow.annotated.samples.describingPolicies
         */
        @JvmStatic
        public fun <P : ItineraWorkflowPolicy> of(
            type: Class<P>
        ): WorkflowPolicyDescriptor<P, Sync> =
            returned(Described.synchronousPartClasses.workflowPolicy(type))
    }
}

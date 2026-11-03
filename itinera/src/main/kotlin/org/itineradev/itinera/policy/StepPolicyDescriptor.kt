package org.itineradev.itinera.policy

import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations

/**
 * How one step policy of workflows of mode [M] is declared: its name, how it is built, and the step
 * hooks it defines, each with what it requests. It defines at least one hook, and each hook at most
 * once. It is attached to steps with the step builder's `policy`.
 *
 * [builder] makes one for a synchronous workflow, [of] reads one from a class marked `@StepPolicy`,
 * and `asyncStepPolicy` and `asyncStepPolicyOf` from `itinera-kotlin` do the same for an
 * asynchronous one. Any other implementation runs too, checked by the executor before the journey.
 *
 * @param P the policy its hooks are called on.
 * @param M the mode of the workflows it can join.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
public interface StepPolicyDescriptor<P : Any, M : Mode> {
    /** The policy's name. */
    public val name: PolicyName

    /** How the policy is built, for every attempt of a step it is attached to. */
    public val factory: PolicyFactory<P>

    /** Its `on step success`, or `null` when it does not define it. */
    public val onStepSuccess: HookDefinition<M, P, StepHookKind<M>, OnSuccess>?

    /** Its `on step failure`, or `null` when it does not define it. */
    public val onStepFailure: HookDefinition<M, P, StepFailureKind<M>, OnFailure>?

    /** Its `on step retry`, or `null` when it does not define it. */
    public val onStepRetry: HookDefinition<M, P, StepRetryKind<M>, OnFailure>?

    /** Its `on step abnormal termination`, or `null` when it does not define it. */
    public val onStepAbnormalTermination: HookDefinition<M, P, StepHookKind<M>, OnFailure>?

    /** Makes step policy descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous step policy named [name], which [factory] builds
         * for every attempt.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.policy.samples.policies
         */
        @JvmStatic
        public fun <P : Any> builder(
            name: String,
            factory: PolicyFactory<P>,
        ): StepPolicyBuilder<P> = StepPolicyBuilder(PolicyDraft(PolicyName.of(name), factory))

        /**
         * The descriptor of a synchronous step policy written as a class marked `@StepPolicy`,
         * named as it says, built with its constructor for every attempt, and whose hooks are its
         * methods marked with a hook's annotation. A Kotlin class needs `itinera-kotlin` to be
         * read.
         *
         * @throws Violations with every mistake in the class's declaration.
         * @sample org.itineradev.itinera.workflow.annotated.samples.describingPolicies
         */
        @JvmStatic
        public fun <P : ItineraStepPolicy> of(type: Class<P>): StepPolicyDescriptor<P, Sync> =
            returned(Described.synchronousPartClasses.stepPolicy(type))
    }
}

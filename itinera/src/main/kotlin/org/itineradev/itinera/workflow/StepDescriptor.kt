package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepFactory

/**
 * How one step of a workflow of mode [M] is declared: its name, what it requests, how it is built
 * for each attempt, how its failed attempts are retried, the policies attached to it and its rescue
 * step. Every step states its own retry budget, whether it retries abnormal terminations and
 * whether it has a rescue step, so the declaration shows everything.
 *
 * [builder] makes one for a synchronous workflow, `asyncStep` from `itinera-kotlin` for an
 * asynchronous one, and annotated workflows get theirs from `@WithStep`. Any other implementation
 * runs too, checked by the executor before the journey.
 *
 * @param M the mode of the workflows the step can join.
 * @sample org.itineradev.itinera.workflow.samples.steps
 */
@ExperimentalItineraApi
public interface StepDescriptor<M : Mode> {
    /** The step's name, unique in its workflow. */
    public val name: StepName

    /**
     * What the step requests, in the order declared, which its factory reads: its inputs are
     * resolved first, in this order, then its services.
     */
    public val requests: List<Request<StepKind<M>, *>>

    /** How the step is built for each attempt. */
    public val factory: StepFactory<M>

    /** How many times a failed attempt may be retried after the first: never below 0. */
    public val retryBudget: Int

    /** Whether an abnormal termination is retried while the retry budget lasts. */
    public val retryOnAbnormalTermination: Boolean

    /** The step policies attached to the step, in the order their hooks are called. */
    public val policies: List<StepPolicyDescriptor<*, M>>

    /**
     * The step's rescue step, named `<step name>_rescue`, which runs once when an attempt fails,
     * the step being attempted once more if it succeeds, or `null` when the step has none. A step
     * with both a rescue step and a retry budget above 0 is refused, as `rescued step retried`.
     */
    public val rescue: RescueStepDescriptor<M>?

    /** Makes step descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous step, which the factory builds for each attempt,
         * with a retry budget of 0 and abnormal terminations retried, unless the builder says
         * otherwise.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.workflow.samples.steps
         */
        @JvmStatic
        public fun builder(name: String, factory: SyncStepFactory): StepBuilder<Sync> =
            StepBuilder(StepName.of(name), factory)
    }
}

/**
 * Builds a [StepDescriptor] of mode [M], with a retry budget of 0 and abnormal terminations retried
 * until it says otherwise.
 *
 * @param M the mode of the workflows the step can join.
 * @sample org.itineradev.itinera.workflow.samples.steps
 */
@ExperimentalItineraApi
public class StepBuilder<M : Mode>
internal constructor(private val name: StepName, private val factory: StepFactory<M>) {
    private val requests = mutableListOf<Request<StepKind<M>, *>>()
    private val policies = mutableListOf<StepPolicyDescriptor<*, M>>()
    private var retryBudget = 0
    private var retryOnAbnormalTermination = true
    private var rescue: RescueStepDescriptor<M>? = null

    /**
     * Declares a request after those already declared, which the step's factory then reads.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    public fun request(request: Request<StepKind<M>, *>): StepBuilder<M> = apply {
        requests += request
    }

    /**
     * Attaches a step policy after those already attached, whose hooks are called in that order.
     *
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun policy(policy: StepPolicyDescriptor<*, M>): StepBuilder<M> = apply {
        policies += policy
    }

    /**
     * Allows this many retries after the first attempt.
     *
     * @throws IllegalArgumentException when [retries] is below 0.
     * @sample org.itineradev.itinera.workflow.samples.steps
     */
    public fun retryBudget(retries: Int): StepBuilder<M> = apply {
        require(retries >= 0) { "A retry budget is never below 0, but was given $retries." }
        retryBudget = retries
    }

    /**
     * Turns `retry on abnormal termination` off, so an abnormal termination fails the step at once,
     * whatever its retry budget.
     *
     * @sample org.itineradev.itinera.workflow.samples.steps
     */
    public fun noRetryOnAbnormalTermination(): StepBuilder<M> = apply {
        retryOnAbnormalTermination = false
    }

    /**
     * Gives the step its rescue step, which runs once when an attempt fails, the step being
     * attempted once more if it succeeds. A retry budget above 0 is then refused when the workflow
     * is built, as `rescued step retried`.
     *
     * @throws IllegalStateException when the step already has a rescue step, since it has one at
     *   most.
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public fun rescue(rescue: RescueStepDescriptor<M>): StepBuilder<M> = apply {
        check(this.rescue == null) { "A step has one rescue step at most, and $name has one." }
        this.rescue = rescue
    }

    /**
     * The descriptor.
     *
     * @sample org.itineradev.itinera.workflow.samples.steps
     */
    public fun build(): StepDescriptor<M> =
        Described(
            name,
            requests.toList(),
            factory,
            retryBudget,
            retryOnAbnormalTermination,
            policies.toList(),
            rescue,
        )

    private class Described<M : Mode>(
        override val name: StepName,
        override val requests: List<Request<StepKind<M>, *>>,
        override val factory: StepFactory<M>,
        override val retryBudget: Int,
        override val retryOnAbnormalTermination: Boolean,
        override val policies: List<StepPolicyDescriptor<*, M>>,
        override val rescue: RescueStepDescriptor<M>?,
    ) : StepDescriptor<M>
}

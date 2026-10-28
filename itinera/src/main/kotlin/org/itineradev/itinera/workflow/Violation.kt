package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName

/**
 * One thing wrong with how a workflow is put together, found before any journey: when it is
 * described or built, or when its instance is handed to an executor. [Violations] lists every one
 * found at once.
 *
 * Those the specification names say so through [named]; the others are mistakes in an annotated
 * class, which the annotations' types cannot catch.
 *
 * @sample org.itineradev.itinera.workflow.samples.violations
 */
@ExperimentalItineraApi
public sealed interface Violation {
    /**
     * The specification's name for this violation, such as `duplicate step name`, if it has one.
     */
    public val named: String?

    /**
     * Two steps of the workflow have this name.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class DuplicateStepName(
        /** The name the steps share. */
        val step: StepName
    ) : Violation {
        override val named: String
            get() = "duplicate step name"
    }

    /**
     * Two or more step policies attached to one step define the same hook.
     *
     * @sample org.itineradev.itinera.policy.samples.misattached
     */
    @JvmRecord
    public data class StepHookDefinedTwice(
        /** The step the policies are attached to. */
        val step: StepName,
        /** The hook they all define. */
        val hook: StepHook,
        /** The policies that define it, in the order they are attached. */
        val policies: List<PolicyName>,
    ) : Violation {
        override val named: String
            get() = "hook defined twice"
    }

    /**
     * Two or more workflow policies define the same hook.
     *
     * @sample org.itineradev.itinera.policy.samples.misattached
     */
    @JvmRecord
    public data class WorkflowHookDefinedTwice(
        /** The hook they all define. */
        val hook: WorkflowHook,
        /** The policies that define it, in the order they are attached. */
        val policies: List<PolicyName>,
    ) : Violation {
        override val named: String
            get() = "hook defined twice"
    }

    /**
     * A policy attached to the workflow, or to one of its steps, requests a role the workflow does
     * not provide.
     *
     * @sample org.itineradev.itinera.policy.samples.misattached
     */
    @JvmRecord
    public data class RoleNotProvided(
        /** The policy. */
        val policy: PolicyName,
        /** The role's interface, which no role the workflow provides is or extends. */
        val role: Class<*>,
    ) : Violation {
        override val named: String
            get() = "role not provided"
    }

    /**
     * Two input adapters of the workflow have this name, whether or not they are attached to the
     * same steps.
     *
     * @sample org.itineradev.itinera.workflow.samples.misadapted
     */
    @JvmRecord
    public data class DuplicateAdapterName(
        /** The name the adapters share. */
        val adapter: AdapterName
    ) : Violation {
        override val named: String
            get() = "duplicate adapter name"
    }

    /**
     * A step has several input adapters: two adapters are attached to it, or one adapter is
     * attached to it more than once.
     *
     * @sample org.itineradev.itinera.workflow.samples.misadapted
     */
    @JvmRecord
    public data class StepAdaptedTwice(
        /** The step. */
        val step: StepName,
        /** The adapters attached to it, once for each time they are, in the order declared. */
        val adapters: List<AdapterName>,
    ) : Violation {
        override val named: String
            get() = "step adapted twice"
    }

    /**
     * An input adapter is attached to a step the workflow does not have.
     *
     * @sample org.itineradev.itinera.workflow.samples.misadapted
     */
    @JvmRecord
    public data class InputAdapterForUnknownStep(
        /** The adapter. */
        val adapter: AdapterName,
        /** The step it names, which the workflow does not have. */
        val step: StepName,
    ) : Violation {
        override val named: String
            get() = "input adapter for unknown step"
    }

    /**
     * A step requests this key twice among its services.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class ServiceRequestedTwice(
        /** The step. */
        val step: StepName,
        /** The key it requests twice. */
        val key: String,
    ) : Violation {
        override val named: String
            get() = "service requested twice"
    }

    /**
     * A step requests a service under this key that no provider serves for it.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class ServiceNotProvided(
        /** The step. */
        val step: StepName,
        /** The key no provider serves for it. */
        val key: String,
    ) : Violation {
        override val named: String
            get() = "service not provided"
    }

    /**
     * Several attachments of providers of this key serve one step: two providers attached to it, or
     * one attached to it twice, or, when [step] is `null`, two providers attached to no step.
     *
     * @sample org.itineradev.itinera.service.samples.misprovided
     */
    @JvmRecord
    public data class ServiceProvidedTwice(
        /** The key of the service they supply. */
        val key: String,
        /** The step they are attached to, or `null` when none of them is attached to a step. */
        val step: StepName?,
        /**
         * The provider of each attachment, in the order declared, so one attached twice is twice.
         */
        val providers: List<ProviderName>,
    ) : Violation {
        override val named: String
            get() = "service provided twice"
    }

    /**
     * Two service providers of the workflow have this name.
     *
     * @sample org.itineradev.itinera.service.samples.misprovided
     */
    @JvmRecord
    public data class DuplicateProviderName(
        /** The name the providers share. */
        val provider: ProviderName
    ) : Violation {
        override val named: String
            get() = "duplicate provider name"
    }

    /**
     * A service provider is attached to a step the workflow does not have.
     *
     * @sample org.itineradev.itinera.service.samples.misprovided
     */
    @JvmRecord
    public data class ServiceProviderForUnknownStep(
        /** The provider. */
        val provider: ProviderName,
        /** The name it gives for a step, which no step of the workflow has. */
        val step: StepName,
    ) : Violation {
        override val named: String
            get() = "service provider for unknown step"
    }

    /**
     * The provider serving a step supplies another type than the step requests under this key.
     *
     * @sample org.itineradev.itinera.service.samples.misprovided
     */
    @JvmRecord
    public data class WrongServiceType(
        /** The step. */
        val step: StepName,
        /** The key of the service it requests. */
        val key: String,
        /** The provider that serves it. */
        val provider: ProviderName,
    ) : Violation {
        override val named: String
            get() = "wrong service type"
    }

    /**
     * A service provider of journey scope requests the name of the step being built or the attempt,
     * which only one of attempt scope receives. The builders cannot make such a provider, so only a
     * descriptor written by hand, which an unchecked cast hands over, or an annotated class has
     * one.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.misscoped
     */
    @JvmRecord
    public data class AttemptRequestInJourneyScope(
        /** The provider. */
        val provider: ProviderName
    ) : Violation {
        override val named: String
            get() = "attempt request in journey scope"
    }

    /**
     * A part of the workflow is in a mode the executor, or the workflow itself, does not accept,
     * such as an asynchronous step in a synchronous workflow.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class ModeNotAccepted(
        /** The part, such as `step charge` or `reporter 2`. */
        val part: String,
        /** The mode accepted. */
        val accepted: Mode,
    ) : Violation {
        override val named: String
            get() = "mode not accepted"
    }

    /**
     * The instance's initial data, or its initial global metadata, holds something that is not a
     * value under this key, or a value whose copy failed, with what the copy threw.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class NotAValue(
        /** Where it is held. */
        val held: Held,
        /** The key it is held under. */
        val key: String,
        /** What the application's code threw while it was copied, if it threw. */
        val error: Throwable?,
    ) : Violation {
        override val named: String?
            get() = null
    }

    /**
     * The instance's initial global metadata sets `journey_id`, which only the executor sets.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    public data object ReservedMetadataKey : Violation {
        override val named: String?
            get() = null
    }

    /**
     * A declaration itinera cannot use, in an annotated class or a hand-written descriptor, such as
     * a step class with no method named `run`, or a hand-written step with a retry budget below 0.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class Misdeclared(
        /**
         * The class, constructor or method, as Java reflection writes it, or a hand-written step,
         * as `step <name>`.
         */
        val declaration: String,
        /** What is wrong with it. */
        val problem: String,
    ) : Violation {
        override val named: String?
            get() = null
    }

    /**
     * Where a workflow instance holds a value it was created with.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    public enum class Held {
        /** In its initial data. */
        INITIAL_DATA,
        /** In its initial global metadata. */
        INITIAL_GLOBAL_METADATA,
    }
}

package org.itineradev.itinera.event

import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.workflow.AdapterName

/**
 * A step, as an abort names who did or requested what caused it; the step itself is the abort's own
 * [JourneyAbort.step].
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public data object ByStep : Requester, BuildSource, ValueSource, ReasonSource, ResultSource

/**
 * A rescue step, requesting the failure's reason or the error of the step it rescues; the rescue
 * step is the abort's own [JourneyAbort.step].
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public data object ByRescueStep : FailureRequester

/**
 * An input adapter, resolving one of a step's inputs.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByAdapter(
    /** The adapter's name. */
    val adapter: AdapterName
) : Requester

/**
 * An input adapter, with the key of the step's input it was resolving.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByAdapterForKey(
    /** The adapter's name. */
    val adapter: AdapterName,
    /** The key of the input. */
    val key: String,
) : BuildSource, ResultSource

/**
 * A hook of a step policy, acting on a step.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByStepHook(
    /** The policy's name. */
    val policy: PolicyName,
    /** The hook. */
    val hook: StepHook,
) : Requester, FailureRequester, ValueSource, ReasonSource, ResultSource

/**
 * A hook of a workflow policy, which acts on no step.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByWorkflowHook(
    /** The policy's name. */
    val policy: PolicyName,
    /** The hook. */
    val hook: WorkflowHook,
) : Requester, ValueSource, HookSource

/**
 * A service provider, supplying a step's service.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByProvider(
    /** The provider's name. */
    val provider: ProviderName
) : Requester, BuildSource, ValueSource

/**
 * A service provider, with the key of the step's service it was providing.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByProviderForKey(
    /** The provider's name. */
    val provider: ProviderName,
    /** The key of the service. */
    val key: String,
) : ResultSource

/**
 * A step policy, while it was built for an attempt of a step.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class ByPolicy(
    /** The policy's name. */
    val policy: PolicyName
) : ResultSource

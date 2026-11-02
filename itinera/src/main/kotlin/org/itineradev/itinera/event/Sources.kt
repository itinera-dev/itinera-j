package org.itineradev.itinera.event

import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName

/**
 * Who made an optional request that had no value: a step for one of its inputs, an input adapter, a
 * hook or a service provider.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
public sealed interface RequestSource

/**
 * Who made a contribution: a step or a hook.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
public sealed interface ContributionSource

/**
 * Who set an entry of global metadata: a step, a hook or a service provider.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
public sealed interface MetadataSource

/**
 * Who emitted a `journey_info`, `journey_warning` or `journey_error` event: a hook or a service
 * provider.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
public sealed interface EmitterSource : RequestSource, MetadataSource

/**
 * A hook that was called: a hook of a step policy, with the step and attempt that triggered it, or
 * a hook of a workflow policy.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
public sealed interface HookSource : EmitterSource, ContributionSource

/**
 * A step, during one of its attempts.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
@JvmRecord
public data class StepSource(
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
) : RequestSource, ContributionSource, MetadataSource

/**
 * An input adapter, resolving an input of a step for one of its attempts.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
@JvmRecord
public data class AdapterSource(
    /** The adapter's name. */
    val adapter: AdapterName,
    /** The name of the step whose input it resolved. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
) : RequestSource

/**
 * A hook of a step policy, with the step and attempt that triggered it.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
@JvmRecord
public data class StepHookSource(
    /** The policy's name. */
    val policy: PolicyName,
    /** The hook. */
    val hook: StepHook,
    /** The name of the step that triggered it. */
    val step: StepName,
    /** The attempt that triggered it. */
    val attempt: Attempt,
) : HookSource

/**
 * A service provider, with the step and attempt it was building.
 *
 * @sample org.itineradev.itinera.event.samples.sources
 */
@JvmRecord
public data class ProviderSource(
    /** The provider's name. */
    val provider: ProviderName,
    /** The name of the step being built. */
    val step: StepName,
    /** The attempt being built. */
    val attempt: Attempt,
) : EmitterSource

package org.itineradev.itinera.engine

import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.workflow.Mode

/** What a journey starts with, once nothing refused it. */
internal class Started(
    val initial: Initial,
    val policies: List<BuiltPolicy<WorkflowHook>>,
    val delivery: Delivery,
    val failures: Failures,
)

/**
 * Does everything that comes before a journey on the executor of [parts], in order: admits the
 * instance, builds the workflow policies, makes the dispatcher, adds the guarded reporters, then
 * sets the journey ID and the initial global metadata.
 */
internal class Start<M : Mode>(private val parts: Parts<M>) {
    /** @throws Refusal when any of it refuses the instance or fails. */
    @JvmSynthetic
    suspend fun of(instance: WorkflowInstance<M>): Started {
        val initial =
            (instance as? Created)?.initial
                ?: Initial.of(instance.initialData, instance.initialGlobalMetadata)
        val violations = admission(instance, parts, initial)
        if (violations.isNotEmpty()) throw Refusal(Refused.Inadmissible(violations))
        val policies = workflowPolicies(parts, instance.descriptor.policies)
        val delivery = dispatcher()
        val failures = Failures()
        added(instance, delivery, failures)
        introduced(instance, initial, delivery, failures)
        return Started(initial, policies, delivery, failures)
    }

    private suspend fun dispatcher(): Delivery =
        parts
            .calling { parts.dispatcher() }
            .getOrElse { thrown ->
                throw Refusal(Refused.DispatcherFactoryFailed(thrown, messageOf(thrown)))
            } ?: throw Refusal(Refused.DispatcherFactoryReturnedNull)

    private suspend fun added(
        instance: WorkflowInstance<M>,
        delivery: Delivery,
        failures: Failures,
    ) {
        parts
            .calling { addAll(instance, delivery, failures) }
            .onFailure { thrown ->
                throw Refusal(Refused.DispatcherFailed(thrown, messageOf(thrown)))
            }
    }

    private suspend fun addAll(
        instance: WorkflowInstance<M>,
        delivery: Delivery,
        failures: Failures,
    ) {
        for (reporter in instance.reporters) delivery.add(reporter, Guard(failures))
    }

    private suspend fun introduced(
        instance: WorkflowInstance<M>,
        initial: Initial,
        delivery: Delivery,
        failures: Failures,
    ) {
        parts
            .calling { setAll(instance, initial, delivery) }
            .onFailure { thrown -> throw notSet(failures.first ?: thrown) }
        failures.first?.let { failed -> throw notSet(failed) }
    }

    private suspend fun setAll(
        instance: WorkflowInstance<M>,
        initial: Initial,
        delivery: Delivery,
    ) {
        delivery.setGlobalMetadata(JOURNEY_ID_KEY, instance.journeyId.toString())
        for ((key, value) in initial.metadata.values) delivery.setGlobalMetadata(key, value.value)
    }

    private fun notSet(thrown: Throwable): Refusal =
        Refusal(Refused.GlobalMetadataNotSet(thrown, messageOf(thrown)))
}

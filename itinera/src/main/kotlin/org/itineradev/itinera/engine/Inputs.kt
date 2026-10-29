package org.itineradev.itinera.engine

import org.itineradev.itinera.event.AdapterSource
import org.itineradev.itinera.event.ByAdapter
import org.itineradev.itinera.event.ByAdapterForKey
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.InputAdapterFailed
import org.itineradev.itinera.event.InputAdapterSupplied
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Supplied

/**
 * Resolves the inputs of the steps of [instance]'s journey: from a step's input adapter, when it
 * has one that supplies the input, and otherwise from the data bag.
 */
internal class Inputs<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val instance: WorkflowInstance<M>,
) {
    /**
     * The receiver's own copy of [input] for [step]'s [attempt], or `null` when an optional input
     * has no value, keeping in [found] the value it is a copy of.
     *
     * @throws JourneyAbortSignal when resolving it aborts the journey.
     */
    @JvmSynthetic
    suspend fun of(
        step: StepName,
        attempt: Attempt,
        input: InputAsked,
        found: MutableMap<String, Captured>,
    ): Any? {
        val adapter =
            instance.descriptor.inputAdapters.find { adapter -> isAttachedTo(adapter, step) }
        val adapting = adapter?.let { attached -> Adapting(attached, step, attempt, input.key) }
        val supplied = adapting?.supplied()
        if (adapting == null || supplied == null) {
            journey.data.stored(input.key)?.let { stored -> found[input.key] = stored }
            val by = RequestedBy(step, ByStep, StepSource(step, attempt))
            return journey.fromWorkflow(input.key, input.type, input.required, by)
        }
        found[input.key] = supplied
        return journey.typed(supplied, input.key, input.type, adapting.call.by)
    }

    /** Calling [adapter] for the input under [key] of [step]'s [attempt]. */
    private inner class Adapting(
        private val adapter: InputAdapterDescriptor<*, M>,
        private val step: StepName,
        private val attempt: Attempt,
        private val key: String,
    ) {
        private val access = DataBags.access(journey.data)

        val call =
            Call(
                RequestedBy(
                    step,
                    ByAdapter(adapter.name),
                    AdapterSource(adapter.name, step, attempt),
                ),
                mapOf(
                    Known.STEP_NAME to step,
                    Known.KEY to key,
                    Known.JOURNEY_ID to instance.journeyId,
                ),
                mapOf(Handle.DATA_BAG_ACCESS to access),
            )

        /**
         * Calls the adapter, once what it requests is resolved: the value it supplied, captured, or
         * `null` when it leaves the input to the data bag.
         *
         * @throws JourneyAbortSignal when the adapter fails, supplies what is not a value or
         *   returns `null`, and when resolving what it requests aborts the journey.
         */
        @JvmSynthetic
        suspend fun supplied(): Captured? {
            val got =
                Answered<InputAdapterKind>(
                    adapter.requests,
                    journey.resolved(adapter.requests, call),
                )
            val returned = parts.calling { parts.adapt(adapter.call, instance.workflow, got) }
            access.close()
            return when (val supplied = returned.getOrElse { thrown -> throw failed(thrown) }) {
                null -> throw unexpected()
                Supplied.FromDataBag -> null
                is Supplied.Value -> captured(supplied.value)
            }
        }

        private suspend fun captured(value: Any?): Captured =
            when (val capture = Values.capture(value)) {
                is Captured -> capture.also { emitSupplied() }
                is NotAValue -> throw failed(capture.error, capture.error?.let(::messageOf))
            }

        private suspend fun failed(thrown: Throwable): JourneyAbortSignal =
            failed(thrown, messageOf(thrown))

        private suspend fun failed(error: Throwable?, message: String?): JourneyAbortSignal {
            journey.emit {
                InputAdapterFailed(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step,
                    attempt,
                    key,
                    adapter.name,
                )
            }
            val source = ByAdapterForKey(adapter.name, key)
            return journey.abort(Abort(StepCouldNotBeBuilt(step, source, message), error))
        }

        private fun unexpected(): JourneyAbortSignal {
            val message = "the input adapter returned null, not what it supplies"
            val source = ByAdapterForKey(adapter.name, key)
            return journey.abort(Abort(UnexpectedResult(step, source, message), null))
        }

        private suspend fun emitSupplied() {
            journey.emit {
                InputAdapterSupplied(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step,
                    attempt,
                    key,
                    adapter.name,
                )
            }
        }
    }
}

private fun isAttachedTo(adapter: InputAdapterDescriptor<*, *>, step: StepName): Boolean =
    step in adapter.steps

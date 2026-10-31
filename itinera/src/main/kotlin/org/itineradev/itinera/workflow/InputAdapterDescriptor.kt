package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.StepName

/**
 * How one input adapter of a workflow of mode [M] is declared: its name, unique among the
 * workflow's adapters, the steps it is attached to, what it requests, and how it is called. A step
 * has at most one adapter, and the adapter supplies the step's inputs, one call for each.
 *
 * [builder] makes one for a synchronous workflow, and `asyncInputAdapter` from `itinera-kotlin` for
 * an asynchronous one. Any other implementation runs too, checked by the executor before the
 * journey.
 *
 * @param W the class of the workflow object it is called with.
 * @param M the mode of the workflows the adapter can join.
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
@ExperimentalItineraApi
public interface InputAdapterDescriptor<in W, M : Mode> {
    /** The adapter's name, unique among the workflow's input adapters. */
    public val name: AdapterName

    /** The steps it is attached to, at least one, in the order declared. */
    public val steps: List<StepName>

    /** What it requests, in the order declared, resolved in that order before each call. */
    public val requests: List<Request<InputAdapterKind, *>>

    /** How it is called. */
    public val call: InputAdapterCall<W, M>

    /** Makes input adapter descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous input adapter named [name], attached to the step
         * named [step], to which the builder may attach others.
         *
         * @throws IllegalArgumentException when [name] or [step] is empty.
         * @sample org.itineradev.itinera.workflow.samples.adapters
         */
        @JvmStatic
        public fun <W> builder(
            name: String,
            step: String,
            adapter: SyncInputAdapter<W>,
        ): InputAdapterBuilder<W, Sync> =
            InputAdapterBuilder(AdapterName.of(name), adapter).step(step)
    }
}

/**
 * Builds an [InputAdapterDescriptor] of mode [M].
 *
 * @param W the class of the workflow object the adapter is called with.
 * @param M the mode of the workflows the adapter can join.
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
@ExperimentalItineraApi
public class InputAdapterBuilder<W, M : Mode>
internal constructor(private val name: AdapterName, private val call: InputAdapterCall<W, M>) {
    private val steps = mutableListOf<StepName>()
    private val requests = mutableListOf<Request<InputAdapterKind, *>>()

    /**
     * Attaches the adapter to the step named [name], after the steps already attached.
     *
     * @throws IllegalArgumentException when [name] is empty.
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public fun step(name: String): InputAdapterBuilder<W, M> = apply { steps += StepName.of(name) }

    /**
     * Declares a request after those already declared, which the adapter then reads.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public fun request(request: Request<InputAdapterKind, *>): InputAdapterBuilder<W, M> = apply {
        requests += request
    }

    /**
     * The descriptor.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public fun build(): InputAdapterDescriptor<W, M> =
        Described(name, steps.toList(), requests.toList(), call)

    private class Described<W, M : Mode>(
        override val name: AdapterName,
        override val steps: List<StepName>,
        override val requests: List<Request<InputAdapterKind, *>>,
        override val call: InputAdapterCall<W, M>,
    ) : InputAdapterDescriptor<W, M>
}

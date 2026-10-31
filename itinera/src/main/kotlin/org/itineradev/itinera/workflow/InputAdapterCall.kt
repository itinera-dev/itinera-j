package org.itineradev.itinera.workflow

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind

/**
 * How an input adapter of a workflow of mode [M] is called: [SyncInputAdapter] in a synchronous
 * workflow, and `AsyncInputAdapter` from `itinera-kotlin` in an asynchronous one. An adapter's
 * descriptor holds the call of its own mode, so an adapter of the other mode cannot join a
 * workflow; the executor checks it again before the journey.
 *
 * @param W the class of the workflow object it is called with.
 * @param M the mode of the workflows the adapter can join.
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public interface InputAdapterCall<in W, M : Mode>

/**
 * A synchronous input adapter, called with the workflow object and the answers to its requests in
 * `got`, once for each input of a step it is attached to. What it throws aborts the journey with
 * `step could not be built`.
 *
 * @param W the class of the workflow object it is called with.
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public fun interface SyncInputAdapter<in W> : InputAdapterCall<W, Sync> {
    /**
     * Supplies the input whose key `got` answers, or leaves it to the data bag.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public fun adapt(workflow: W, got: Got<InputAdapterKind>): Supplied
}

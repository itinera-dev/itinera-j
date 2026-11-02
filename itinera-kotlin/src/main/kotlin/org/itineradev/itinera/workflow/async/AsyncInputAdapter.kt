package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Supplied

/**
 * An asynchronous input adapter, called with the workflow object and the answers to its requests in
 * `got`, once for each input of a step it is attached to, and awaited. What it throws aborts the
 * journey with `step could not be built`.
 *
 * @param W the class of the workflow object it is called with.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncAdapters
 */
public fun interface AsyncInputAdapter<in W> : InputAdapterCall<W, Async> {
    /**
     * Supplies the input whose key `got` answers, or leaves it to the data bag.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncAdapters
     */
    @JvmSynthetic public suspend fun adapt(workflow: W, got: Got<InputAdapterKind>): Supplied
}

package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Mode

/**
 * What reading the input adapters of a workflow class takes in one mode: how each adapter method is
 * called.
 *
 * Part of itinera's engine, not of its API.
 */
public fun interface AdapterCalls<M : Mode> {
    /** How the adapter method [adapter] is called on the workflow object. */
    public fun of(adapter: AdapterMethod): InputAdapterCall<Any?, M>
}

/**
 * A method of a workflow class that is one of its input adapters, and what it requests, in the
 * order of its parameters.
 *
 * Part of itinera's engine, not of its API.
 */
public class AdapterMethod
internal constructor(
    /** The method. */
    public val method: Method,
    private val requests: List<Request<InputAdapterKind, *>>,
) {
    /** The arguments it is called with, from its answers in [got]. */
    public fun arguments(got: Got<InputAdapterKind>): Array<Any?> =
        requests.map { request -> got[request] }.toTypedArray()
}

package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.HookReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.Mode

/**
 * What reading policy classes takes in one mode: the hook reporter of that mode, which a hook
 * requests by its type, and how each hook method is called.
 *
 * Part of itinera's engine, not of its API.
 */
public interface HookCalls<M : Mode> {
    /** The request of this mode's hook reporter, by the class a hook's parameter names. */
    public val handles: Map<Class<*>, Request<HookReporterKind<M>, *>>

    /**
     * Why a hook cannot request each handle of the other mode, by the handle's class name, since
     * this mode's artifact may not hold the class.
     */
    public val otherModes: Map<String, String>

    /** How the step hook [hook] is called on its policy. */
    public fun <K, R> stepHook(hook: HookMethod): HookCall<M, Any, K, R>

    /** How the workflow hook [hook] is called on its policy. */
    public fun <K> workflowHook(hook: HookMethod): HookCall<M, Any, K, Unit>
}

/**
 * A method of a policy class that is one of its hooks, and what it requests, in the order of its
 * parameters.
 *
 * Part of itinera's engine, not of its API.
 */
public class HookMethod
internal constructor(
    /** The method. */
    public val method: Method,
    private val requests: List<Request<*, *>>,
) {
    /** What it requests, as requests of the kind [K] of its hook. */
    internal fun <K> requests(): List<Request<K, *>> = returned(requests)

    /** The arguments it is called with, from its answers in [got]. */
    public fun <K> arguments(got: Got<K>): Array<Any?> =
        requests<K>().map { request -> got[request] }.toTypedArray()
}

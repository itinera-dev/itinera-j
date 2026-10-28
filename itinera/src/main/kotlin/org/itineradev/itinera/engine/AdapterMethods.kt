package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Violation

/**
 * Reads the input adapters of a workflow class in [mode], its [markedMethods] marked
 * `@InputAdapter`, recording each mistake in [problems].
 */
internal class AdapterMethods<M : Mode>(
    private val mode: M,
    private val calls: AdapterCalls<M>,
    private val problems: Problems,
) {
    private val requests = AdapterRequests(problems)

    /** The adapters [type] declares, leaving out those with a mistake. */
    fun <W> of(type: Class<*>): List<InputAdapterDescriptor<W, M>> =
        markedMethods(type, InputAdapter::class.java).mapNotNull(::adapter).toList()

    private fun adapter(method: Method): InputAdapterDescriptor<Any?, M>? {
        val marked = method.getAnnotation(InputAdapter::class.java)
        val function = functionOf(method)
        val name = marked.name.ifEmpty { function.name }
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode) problems += Violation.ModeNotAccepted("input adapter $name", mode)
        val stepsNamed = stepsNamed(method, marked)
        val asks = requests.of(method, function)
        val returns = problems.returns(method, function, Supplied::class.java)
        val opened = problems.opened(method.declaringClass, method)
        val usable = inMode && returns && opened && stepsNamed
        return if (usable && asks != null) described(method, name, marked, asks) else null
    }

    private fun stepsNamed(method: Method, marked: InputAdapter): Boolean {
        val empty = marked.steps.any(String::isEmpty)
        if (empty) problems.misdeclared(method.toString(), "names a step with an empty name")
        return !empty
    }

    private fun described(
        method: Method,
        name: String,
        marked: InputAdapter,
        asks: List<Request<InputAdapterKind, *>>,
    ): InputAdapterDescriptor<Any?, M> {
        val builder =
            InputAdapterBuilder(AdapterName.of(name), calls.of(AdapterMethod(method, asks)))
        marked.steps.forEach(builder::step)
        asks.forEach(builder::request)
        return builder.build()
    }
}

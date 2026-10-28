package org.itineradev.itinera.engine

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Supplied

/**
 * Reads the input adapters of a workflow class in [mode], its methods marked `@InputAdapter` and
 * those of its superclasses, the class's before its superclass's and each class's in the order of
 * their names, recording each mistake in [problems]. A method that overrides another replaces it,
 * whether or not it is marked.
 */
internal class AdapterMethods<M : Mode>(
    private val mode: M,
    private val calls: AdapterCalls<M>,
    private val problems: Problems,
) {
    private val requests = AdapterRequests(problems)

    /** The adapters [type] declares, leaving out those with a mistake. */
    fun <W> of(type: Class<*>): List<InputAdapterDescriptor<W, M>> =
        generateSequence(type, Class<*>::getSuperclass)
            .flatMap(::byName)
            .distinctBy(::overridden)
            .filter(::isDeclared)
            .filter(::isMarked)
            .mapNotNull(::adapter)
            .toList()

    private fun adapter(method: Method): InputAdapterDescriptor<Any?, M>? {
        val marked = method.getAnnotation(InputAdapter::class.java)
        val function = functionOf(method)
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode) problems.misdeclared(method.toString(), modeProblem(function.isSuspend))
        val stepsNamed = stepsNamed(method, marked)
        val asks = requests.of(method, function)
        val returns = problems.returns(method, function, Supplied::class.java)
        val opened = problems.opened(method.declaringClass, method)
        val usable = inMode && returns && opened && stepsNamed
        return if (usable && asks != null) described(method, function, marked, asks) else null
    }

    private fun stepsNamed(method: Method, marked: InputAdapter): Boolean {
        val empty = marked.steps.any(String::isEmpty)
        if (empty) problems.misdeclared(method.toString(), "names a step with an empty name")
        return !empty
    }

    private fun described(
        method: Method,
        function: KotlinFunction,
        marked: InputAdapter,
        asks: List<Request<InputAdapterKind, *>>,
    ): InputAdapterDescriptor<Any?, M> {
        val name = AdapterName.of(marked.name.ifEmpty { function.name })
        val builder = InputAdapterBuilder(name, calls.of(AdapterMethod(method, asks)))
        marked.steps.forEach(builder::step)
        asks.forEach(builder::request)
        return builder.build()
    }
}

private fun byName(type: Class<*>): Sequence<Method> =
    declaredMethods(type).sortedWith(compareBy(Method::getName, Method::toString))

/**
 * What a method overriding [method] shares with it, its name and its parameters' types, or [method]
 * itself when nothing can override it. A bridge the compiler adds for a narrower override shares
 * the overridden method's erased types, so it hides that method.
 */
private fun overridden(method: Method): Any =
    if (isOverridable(method)) method.name to method.parameterTypes.toList() else method

private fun isOverridable(method: Method): Boolean =
    !Modifier.isPrivate(method.modifiers) && !Modifier.isStatic(method.modifiers)

private fun isMarked(method: Method): Boolean = method.isAnnotationPresent(InputAdapter::class.java)

private fun modeProblem(suspending: Boolean): String =
    if (suspending) "is suspend, though an input adapter of a synchronous workflow is not"
    else "is not suspend, though an input adapter of an asynchronous workflow is"

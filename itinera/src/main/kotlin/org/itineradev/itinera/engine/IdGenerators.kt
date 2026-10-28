package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.JourneyIdGenerator

/** Reads the ID generator of a workflow class, recording each mistake in [problems]. */
internal class IdGenerators(private val problems: Problems) {
    /** The class's ID generator, or `null` when it has none or it has a mistake. */
    fun <W> of(type: Class<*>): JourneyIdGenerator<W>? {
        val generators =
            generateSequence(type, Class<*>::getSuperclass)
                .flatMap(::declaredMethods)
                .filter(::isIdGenerator)
                .toList()
        if (generators.size > 1) problems.misdeclared(type.name, "has several ID generators")
        return generators.singleOrNull()?.let(::checked)
    }

    private fun <W> checked(method: Method): JourneyIdGenerator<W>? {
        val function = functionOf(method)
        if (function.isSuspend)
            problems.misdeclared(method.toString(), "is suspend, which an ID generator never is")
        val parameters = parameterClasses(function)
        val takesData = parameters == listOf(DataBag::class.java)
        val takes = parameters.isEmpty() || takesData
        if (!takes)
            problems.misdeclared(
                method.toString(),
                "takes what an ID generator cannot: it takes nothing or a DataBag",
            )
        val usable =
            !function.isSuspend &&
                takes &&
                problems.returns(method, function, String::class.java) &&
                problems.opened(method.declaringClass, method)
        return if (usable) generator(method, takesData) else null
    }
}

private fun <W> generator(method: Method, takesData: Boolean): JourneyIdGenerator<W> =
    if (takesData)
        JourneyIdGenerator { workflow, data -> returned(invoked(method, workflow, data)) }
    else JourneyIdGenerator { workflow, _ -> returned(invoked(method, workflow)) }

private fun isIdGenerator(method: Method): Boolean =
    method.isAnnotationPresent(IdGenerator::class.java)

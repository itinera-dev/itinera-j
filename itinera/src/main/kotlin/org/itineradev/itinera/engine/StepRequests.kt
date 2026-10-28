package org.itineradev.itinera.engine

import java.lang.reflect.Executable
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.workflow.Mode

/**
 * Reads what a step class requests from its parameters, recording each mistake in [problems]:
 * inputs from its constructor's, and services and handles from its run's.
 */
internal class StepRequests<M : Mode>(
    private val factories: StepFactories<M>,
    private val problems: Problems,
) {
    private val data = DataRequests(problems)

    /** The inputs [constructor] requests, or `null` when one of them has a mistake. */
    fun inputs(constructor: Executable): List<Request<StepKind<M>, *>>? =
        parametersOf(constructor)?.map { parameter -> input(constructor, parameter) }?.let(::all)

    /** The services and handles [run] requests, or `null` when one of them has a mistake. */
    fun ofRun(run: Executable): List<Request<StepKind<M>, *>>? =
        parametersOf(run)?.map { parameter -> ofRun(run, parameter) }?.let(::all)

    private fun parametersOf(declaration: Executable): List<DeclaredParameter>? {
        val function = functionOf(declaration)
        val owned = ownsParameters(declaration, function)
        if (!owned)
            problems.misdeclared(
                declaration.toString(),
                "captures what its enclosing code holds, which itinera cannot give: declare the " +
                    "step at the top level, or as a static nested class",
            )
        return function.parameters.takeIf { owned }
    }

    private fun input(
        constructor: Executable,
        parameter: DeclaredParameter,
    ): Request<StepKind<M>, *>? {
        val input =
            parameter.parameter.getAnnotation(Input::class.java)
                ?: return data.refused(
                    constructor,
                    "has a parameter that is not an input, ${nameOf(parameter)}: a step's " +
                        "constructor takes only inputs, marked @Input",
                )
        return data.data(constructor, parameter, input.value, input.required, ::InputAsked)
    }

    private fun ofRun(run: Executable, parameter: DeclaredParameter): Request<StepKind<M>, *>? {
        val type = parameter.parameter.type
        val service = parameter.parameter.getAnnotation(Service::class.java)
        return when {
            service != null -> service(run, parameter, service.value)
            parameter.parameter.isAnnotationPresent(Input::class.java) ->
                data.refused(
                    run,
                    "requests an input in run, though a step's inputs are its constructor's",
                )
            type == Contributor::class.java -> Requests.contributor()
            type in factories.handles -> factories.handles[type]
            type.name in factories.otherModes ->
                data.refused(run, factories.otherModes.getValue(type.name))
            else ->
                data.refused(
                    run,
                    "has a parameter that requests nothing, ${nameOf(parameter)}: mark a " +
                        "service @Service, and request a handle by its type",
                )
        }
    }

    private fun service(
        run: Executable,
        parameter: DeclaredParameter,
        written: String,
    ): Request<StepKind<M>, *>? {
        val key = data.keyOf(run, parameter, written) ?: return null
        return if (isKotlin(run.declaringClass) && admitsNull(parameter.type))
            data.refused(
                run,
                "requests service $key as a type that admits null, though a service is always " +
                    "required",
            )
        else
            requestOf<StepKind<M>, Any?>(ServiceAsked(key, parameter.parameter.type), Answer.ITSELF)
    }
}

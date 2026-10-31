package org.itineradev.itinera.engine

import java.lang.reflect.Executable
import java.util.Optional
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.value.requestable
import org.itineradev.itinera.workflow.Mode

/**
 * Reads what a step class requests from its parameters, recording each mistake in [problems]:
 * inputs from its constructor's, and services and handles from its run's.
 */
internal class StepRequests<M : Mode>(
    private val factories: StepFactories<M>,
    private val problems: Problems,
) {
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
                ?: return refused(
                    constructor,
                    "has a parameter that is not an input, ${nameOf(parameter)}: a step's " +
                        "constructor takes only inputs, marked @Input",
                )
        val written =
            WrittenRequest(parameter.type, input.required, isKotlin(constructor.declaringClass))
        return keyOf(constructor, parameter, input.value)?.let { key ->
            input(constructor, key, written)
        }
    }

    private fun input(
        constructor: Executable,
        key: String,
        written: WrittenRequest,
    ): Request<StepKind<M>, *>? {
        val answer = written.answer() ?: return javaOptional(constructor)
        return requestable(constructor, key, written.type())?.let { type ->
            requestOf<StepKind<M>, Any?>(InputAsked(key, type, written.required()), answer)
        }
    }

    private fun ofRun(run: Executable, parameter: DeclaredParameter): Request<StepKind<M>, *>? {
        val type = parameter.parameter.type
        val service = parameter.parameter.getAnnotation(Service::class.java)
        return when {
            service != null -> service(run, parameter, service.value)
            parameter.parameter.isAnnotationPresent(Input::class.java) ->
                refused(
                    run,
                    "requests an input in run, though a step's inputs are its constructor's",
                )
            type == Contributor::class.java -> Requests.contributor()
            type in factories.handles -> factories.handles[type]
            type.name in factories.otherModes ->
                refused(run, factories.otherModes.getValue(type.name))
            else ->
                refused(
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
        val key = keyOf(run, parameter, written) ?: return null
        return if (isKotlin(run.declaringClass) && admitsNull(parameter.type))
            refused(
                run,
                "requests service $key as a type that admits null, though a service is always " +
                    "required",
            )
        else
            requestOf<StepKind<M>, Any?>(ServiceAsked(key, parameter.parameter.type), Answer.ITSELF)
    }

    private fun keyOf(
        declaration: Executable,
        parameter: DeclaredParameter,
        written: String,
    ): String? {
        val key = written.ifEmpty { parameter.name }
        if (key == null)
            problems.misdeclared(
                declaration.toString(),
                "names no key for ${nameOf(parameter)}, whose name was not kept: write " +
                    "the key, or compile with -parameters",
            )
        return key
    }

    private fun requestable(declaration: Executable, key: String, type: ValueType): ValueType? =
        try {
            requestable(type)
        } catch (refusal: IllegalArgumentException) {
            problems.misdeclared(declaration.toString(), "requests $key: ${refusal.message}")
            null
        }

    private fun javaOptional(declaration: Executable): Request<StepKind<M>, *>? =
        refused(
            declaration,
            "requests an Optional, which itinera reads only with the artifact " +
                "org.itinera-dev:itinera-java: add it to the application's dependencies",
        )

    private fun refused(declaration: Executable, problem: String): Request<StepKind<M>, *>? {
        problems.misdeclared(declaration.toString(), problem)
        return null
    }
}

/**
 * How a request's parameter is written: its declared [type], whether its annotation says it is
 * [required][requiredWritten], and whether it is in a Kotlin class, whose nullable types make a
 * request optional, where a Java class writes `Optional`.
 */
private class WrittenRequest(
    private val type: ValueType,
    private val requiredWritten: Boolean,
    private val kotlin: Boolean,
) {
    private val optional = !requiredWritten && if (kotlin) admitsNull(type) else isOptional(type)

    fun required(): Boolean = !optional

    /**
     * The type the value is read as: what an `Optional` holds, which is never `null`, or the type
     * itself.
     */
    fun type(): ValueType =
        if (optional && !kotlin)
            excludingNull((type as ClassType).arguments.singleOrNull() ?: AnyValueType)
        else type

    /** How the answer is given, or `null` for an `Optional` without `itinera-java`. */
    fun answer(): Answer? = if (optional && !kotlin) javaForms?.optional else Answer.ITSELF
}

private fun isOptional(type: ValueType): Boolean =
    type is ClassType && type.raw == Optional::class.java

private fun admitsNull(type: ValueType): Boolean =
    when (type) {
        is ClassType -> type.nullable
        is VariableType -> type.nullness != Nullness.NON_NULL
        AnyValueType -> true
    }

/** The parameter's name, or the name Java reflection gives it when it was not kept. */
private fun nameOf(parameter: DeclaredParameter): String =
    parameter.name ?: parameter.parameter.name

/** Every one of [read], or `null` when one of them could not be read. */
private fun <T : Any> all(read: List<T?>): List<T>? =
    if (null in read) null else read.filterNotNull()

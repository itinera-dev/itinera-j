package org.itineradev.itinera.engine

import java.lang.reflect.Executable
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.value.requestable

/**
 * Reads the requests that parameters of steps and hooks make, recording each mistake in [problems]:
 * the checks the readers of both share.
 */
internal class DataRequests(private val problems: Problems) {
    /**
     * The request of data that [parameter] of [declaration] makes under the key [written], or its
     * name when that is empty, which [asked] asks for by key, type and whether it is required; or
     * `null` when it has a mistake.
     */
    fun <K> data(
        declaration: Executable,
        parameter: DeclaredParameter,
        written: String,
        required: Boolean,
        asked: (String, ValueType, Boolean) -> Asked,
    ): Request<K, *>? {
        val form = WrittenRequest(parameter.type, required, isKotlin(declaration.declaringClass))
        val key = keyOf(declaration, parameter, written)
        val answer = form.answer()
        return when {
            key == null -> null
            answer == null -> javaOptional(declaration)
            else ->
                requestable(declaration, key, form.type())?.let { type ->
                    requestOf<K, Any?>(asked(key, type, form.required()), answer)
                }
        }
    }

    /** The key [parameter] of [declaration] names, [written] or its name, or `null` for none. */
    fun keyOf(declaration: Executable, parameter: DeclaredParameter, written: String): String? {
        val key = written.ifEmpty { parameter.name }
        if (key == null)
            problems.misdeclared(
                declaration.toString(),
                "names no key for ${nameOf(parameter)}, whose name was not kept: write " +
                    "the key, or compile with -parameters",
            )
        return key
    }

    /** Records that [declaration] requests an `Optional` without `itinera-java`. */
    fun <K> javaOptional(declaration: Executable): Request<K, *>? =
        refused(
            declaration,
            "requests an Optional, which itinera reads only with the artifact " +
                "org.itinera-dev:itinera-java: add it to the application's dependencies",
        )

    /** Records [problem] in [declaration], which therefore requests nothing. */
    fun <K> refused(declaration: Executable, problem: String): Request<K, *>? {
        problems.misdeclared(declaration.toString(), problem)
        return null
    }

    private fun requestable(declaration: Executable, key: String, type: ValueType): ValueType? =
        try {
            requestable(type)
        } catch (refusal: IllegalArgumentException) {
            problems.misdeclared(declaration.toString(), "requests $key: ${refusal.message}")
            null
        }
}

/** The parameter's name, or the name Java reflection gives it when it was not kept. */
internal fun nameOf(parameter: DeclaredParameter): String =
    parameter.name ?: parameter.parameter.name

/** Every one of [read], or `null` when one of them could not be read. */
internal fun <T : Any> all(read: List<T?>): List<T>? =
    if (null in read) null else read.filterNotNull()

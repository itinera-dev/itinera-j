package org.itineradev.itinera.engine

import java.util.Optional

/**
 * How a request's parameter is written: its declared [type], whether its annotation says it is
 * [required][requiredWritten], and whether it is in a Kotlin class, whose nullable types make a
 * request optional, where a Java class writes `Optional`.
 */
internal class WrittenRequest(
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

internal fun isOptional(type: ValueType): Boolean =
    type is ClassType && type.raw == Optional::class.java

internal fun admitsNull(type: ValueType): Boolean =
    when (type) {
        is ClassType -> type.nullable
        is VariableType -> type.nullness != Nullness.NON_NULL
        AnyValueType -> true
    }

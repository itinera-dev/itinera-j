package org.itineradev.itinera.engine

/**
 * A type a value is checked against, as a type token, a record's component or a data class's
 * property declares it, with whether `null` is of it at each level.
 *
 * Part of itinera's engine, not of its API: `itinera-kotlin` builds these from Kotlin's types.
 */
public sealed interface ValueType

/**
 * A class, with its type arguments, or none when it is used raw, and whether `null` is of it.
 *
 * A primitive class is never held: its wrapper is, so `long` is `Long`, refusing `null`.
 */
public data class ClassType(
    /** The class, never a primitive. */
    public val raw: Class<*>,
    /** The type arguments, in order; empty when the class is generic but used raw. */
    public val arguments: List<ValueType>,
    /** Whether `null` is of this type. */
    public val nullable: Boolean,
) : ValueType {
    init {
        require(!raw.isPrimitive) {
            "A type holds ${raw.kotlin.javaObjectType.simpleName}, not ${raw.name}."
        }
    }

    override fun toString(): String =
        raw.simpleName +
            (if (arguments.isEmpty()) "" else arguments.joinToString(", ", "<", ">")) +
            (if (nullable) "?" else "")
}

/** A type variable of a generic record or data class, bound by the type its value is read as. */
public data class VariableType(
    /** The variable's name, as the class declares it. */
    public val name: String,
    /** Whether `null` is of it. */
    public val nullness: Nullness,
) : ValueType {
    override fun toString(): String =
        when (nullness) {
            Nullness.NULLABLE -> "$name?"
            Nullness.NON_NULL -> "$name!!"
            Nullness.PARAMETRIC -> name
        }
}

/**
 * Any value at all, `null` included: an unbounded wildcard, a star projection, or a variable no
 * argument binds.
 */
public data object AnyValueType : ValueType {
    override fun toString(): String = "*"
}

/** Whether `null` is of a type variable. */
public enum class Nullness {
    /** `null` is of it, whatever binds it: `T?` in Kotlin, `@Nullable T` in Java. */
    NULLABLE,

    /** `null` is never of it: `@NonNull T` in Java. */
    NON_NULL,

    /** `null` is of it when it is of the type that binds it. */
    PARAMETRIC,
}

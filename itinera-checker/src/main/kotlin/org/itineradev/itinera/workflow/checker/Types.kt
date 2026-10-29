package org.itineradev.itinera.workflow.checker

/** A type as a declaration writes it, with whether `null` is of it at each level. */
internal sealed interface TypeUse

/**
 * A class, with its type arguments, empty when it is used raw, and whether `null` is of it. A
 * primitive type is its wrapper class, marked [primitive].
 */
internal data class ClassUse(
    val raw: ClassSymbol,
    val arguments: List<TypeUse>,
    val nullable: Boolean,
    val primitive: Boolean,
) : TypeUse

/** An array, named as `Class.getSimpleName` names its class, such as `String[]`. */
internal data class ArrayUse(val simpleName: String, val nullable: Boolean) : TypeUse

/** A type variable, and whether `null` is of it. */
internal data class VariableUse(val name: String, val nullness: Nullness) : TypeUse

/** Any value at all: an unbounded wildcard, a star projection or a contravariant projection. */
internal data object AnyUse : TypeUse

/** Nothing: what a Java method declared `void` returns, and what a constructor does. */
internal data object VoidUse : TypeUse

/** Whether `null` is of a type variable. */
internal enum class Nullness {
    /** Always. */
    NULLABLE,

    /** Never. */
    NON_NULL,

    /** When it is of the type that binds the variable. */
    PARAMETRIC,
}

/** [type] as the run-time reader writes it in a mistake, as its `ValueType` does. */
internal fun written(type: TypeUse): String =
    when (type) {
        is ClassUse ->
            type.raw.simpleName +
                (if (type.arguments.isEmpty()) ""
                else type.arguments.joinToString(", ", "<", ">", transform = ::written)) +
                (if (type.nullable) "?" else "")
        is ArrayUse -> type.simpleName + if (type.nullable) "?" else ""
        is VariableUse ->
            when (type.nullness) {
                Nullness.NULLABLE -> "${type.name}?"
                Nullness.NON_NULL -> "${type.name}!!"
                Nullness.PARAMETRIC -> type.name
            }
        AnyUse -> "*"
        VoidUse -> "void"
    }

/** Whether `null` is of [type] at its top level. */
internal fun admitsNull(type: TypeUse): Boolean =
    when (type) {
        is ClassUse -> type.nullable
        is ArrayUse -> type.nullable
        is VariableUse -> type.nullness != Nullness.NON_NULL
        AnyUse -> true
        VoidUse -> false
    }

/** The class [type] names, if it names one. */
internal fun classOf(type: TypeUse): ClassSymbol? = (type as? ClassUse)?.raw

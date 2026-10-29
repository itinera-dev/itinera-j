package org.itineradev.itinera.workflow.checker

private val wrappers: Map<String, String> =
    mapOf(
        "boolean" to "java.lang.Boolean",
        "char" to "java.lang.Character",
        "byte" to "java.lang.Byte",
        "short" to "java.lang.Short",
        "int" to "java.lang.Integer",
        "long" to "java.lang.Long",
        "float" to "java.lang.Float",
        "double" to "java.lang.Double",
    )

/** The wrapper class of the primitive type [name], or the class [name] itself, as `boxed` does. */
internal fun boxed(name: String): String = wrappers[name] ?: name

/** Whether [function] declares it returns [expected] or one of its subtypes. */
internal fun returnsSubtype(function: FunctionSymbol, expected: String): Boolean =
    function.returnClass?.isSubtypeOf(expected) ?: false

/** Whether [type] is written to admit `null`, as Kotlin writes `Outcome?`. */
internal fun isMarkedNullable(type: TypeUse): Boolean =
    when (type) {
        is ClassUse -> type.nullable
        is ArrayUse -> type.nullable
        is VariableUse -> type.nullness == Nullness.NULLABLE
        AnyUse,
        VoidUse -> false
    }

/**
 * Whether [function] returns [expected], and never `null`, recording in [findings] why not: a
 * Kotlin return type that admits `null` is refused, as Java's are never read so.
 */
internal fun returns(findings: Findings, function: FunctionSymbol, expected: String): Boolean {
    val assignable = returnsSubtype(function, expected)
    val simple = simpleNameOf(expected)
    if (!assignable) findings.misdeclared(function, "returns ${function.returnName}, not $simple")
    val nullable = function.owner.isKotlin && isMarkedNullable(function.returns)
    if (nullable) findings.misdeclared(function, "returns $simple?, which admits null")
    return assignable && !nullable
}

package org.itineradev.itinera.workflow.checker

/** What a `String` is, so a map whose keys are of one of these may hold strings as keys. */
internal val stringTypes =
    setOf(
        Names.STRING,
        Names.OBJECT,
        "java.lang.CharSequence",
        "java.lang.Comparable",
        "java.io.Serializable",
        "java.lang.constant.Constable",
        "java.lang.constant.ConstantDesc",
    )

/**
 * Why no value can be of [type] for the keys it gives a map, as the run-time reader says it, or
 * `null` when it is no map or its keys can be strings.
 */
internal fun whyNotKeyed(type: ClassUse): String? {
    val keys = supertypeArguments(type, Names.MAP)?.firstOrNull() ?: return null
    val stringKeyed =
        when (keys) {
            is ClassUse -> keys.raw.name in stringTypes
            is ArrayUse -> false
            is VariableUse,
            AnyUse,
            VoidUse -> true
        }
    return if (stringKeyed) null
    else "No value is a ${written(type)}, since the keys of a map that is a value are strings."
}

/**
 * The arguments [type] gives the supertype [target], or `null` when it is not one, or names it raw.
 */
private fun supertypeArguments(type: ClassUse, target: String): List<TypeUse>? =
    if (type.raw.name == target) type.arguments.takeIf(List<TypeUse>::isNotEmpty)
    else
        supertypesOf(type).firstNotNullOfOrNull { supertype ->
            supertypeArguments(supertype, target)
        }

private fun supertypesOf(type: ClassUse): List<ClassUse> {
    val bindings = type.raw.typeParameters.zip(type.arguments).toMap()
    return type.raw.supertypes.map { supertype -> substituted(supertype, bindings) }
}

private fun substituted(type: ClassUse, bindings: Map<String, TypeUse>): ClassUse =
    type.copy(arguments = type.arguments.map { argument -> substituted(argument, bindings) })

private fun substituted(type: TypeUse, bindings: Map<String, TypeUse>): TypeUse =
    when (type) {
        is ClassUse -> substituted(type, bindings)
        is VariableUse -> bindings[type.name] ?: AnyUse
        is ArrayUse,
        AnyUse,
        VoidUse -> type
    }

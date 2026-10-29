package org.itineradev.itinera.workflow.checker

/**
 * The methods of [type] and of its superclasses marked [mark], the class's before its superclass's
 * and each class's in the order of their names. A method that overrides another replaces it,
 * whether or not it is marked.
 */
internal fun markedMethods(type: ClassSymbol, mark: String): List<FunctionSymbol> =
    generateSequence(type, ClassSymbol::superclass)
        .flatMap(::byName)
        .fold(emptyList(), ::unlessOverridden)
        .filter { method -> method.isMarked(mark) }

private fun byName(type: ClassSymbol): Sequence<FunctionSymbol> =
    type.methods.sortedWith(compareBy(FunctionSymbol::jvmName, ::parametersOf)).asSequence()

private fun parametersOf(method: FunctionSymbol): String =
    method.parameters.joinToString(",", transform = ParameterSymbol::jvmClass)

/** [kept], with [method] added unless one of them overrides it. */
private fun unlessOverridden(
    kept: List<FunctionSymbol>,
    method: FunctionSymbol,
): List<FunctionSymbol> = if (kept.any { other -> other.overrides(method) }) kept else kept + method

package org.itineradev.itinera.value

import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.Nullness
import org.itineradev.itinera.engine.ValueType
import org.itineradev.itinera.engine.VariableType

/** A class as its declaration names it, with its own type variables as arguments. */
internal fun declared(type: Class<*>): ClassType =
    ClassType(type, type.typeParameters.map(::genericType), true)

/**
 * The arguments a type gives one of its supertypes, or `null` when it is not one, or names it raw.
 */
internal fun supertypeArguments(type: ClassType, target: Class<*>): List<ValueType>? =
    if (type.raw == target) type.arguments.takeIf { it.isNotEmpty() }
    else
        supertypesOf(type).firstNotNullOfOrNull { supertype ->
            supertypeArguments(supertype, target)
        }

private fun supertypesOf(type: ClassType): List<ClassType> {
    val bindings = type.raw.typeParameters.map { it.name }.zip(type.arguments).toMap()
    return (listOfNotNull(type.raw.genericSuperclass) + type.raw.genericInterfaces)
        .map(::genericType)
        .filterIsInstance<ClassType>()
        .map { supertype -> substitutedClass(supertype, bindings) }
}

/** A type with the variables a binding names put in, keeping what each says of `null`. */
internal fun substituted(type: ValueType, bindings: Map<String, ValueType>): ValueType =
    when (type) {
        AnyValueType -> type
        is ClassType -> substitutedClass(type, bindings)
        is VariableType -> bound(bindings[type.name], type.nullness)
    }

private fun substitutedClass(type: ClassType, bindings: Map<String, ValueType>): ClassType =
    type.copy(arguments = type.arguments.map { argument -> substituted(argument, bindings) })

private fun bound(type: ValueType?, nullness: Nullness): ValueType =
    when (nullness) {
        Nullness.PARAMETRIC -> type ?: AnyValueType
        Nullness.NULLABLE -> withNull(type ?: AnyValueType, true)
        Nullness.NON_NULL -> withNull(type ?: AnyValueType, false)
    }

private fun withNull(type: ValueType, nullable: Boolean): ValueType =
    when (type) {
        is ClassType -> type.copy(nullable = nullable)
        is VariableType -> type
        AnyValueType -> if (nullable) type else ClassType(Any::class.java, emptyList(), false)
    }

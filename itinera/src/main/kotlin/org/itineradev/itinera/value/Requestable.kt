package org.itineradev.itinera.value

import java.lang.reflect.Modifier
import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.ValueType
import org.itineradev.itinera.engine.VariableType

/** The classes of the copies a receiver gets, which a request may name though they are concrete. */
private val copyClasses: Set<Class<*>> =
    setOf(ArrayList::class.java, LinkedHashSet::class.java, LinkedHashMap::class.java)

/**
 * The type itself, when some value can be of it, at every level; otherwise throws
 * [IllegalArgumentException], saying why.
 */
internal fun requestable(type: ValueType): ValueType {
    when (type) {
        AnyValueType -> {}
        is VariableType ->
            throw IllegalArgumentException(
                "A type token needs a type with no type variable, but $type is one."
            )
        is ClassType -> {
            require(canBeValue(type.raw)) { whyNot(type.raw) }
            requireStringKeys(type)
            type.arguments.forEach(::requestable)
        }
    }
    return type
}

private fun canBeValue(type: Class<*>): Boolean =
    when {
        type.isArray -> false
        type.isInterface || type in copyClasses || type == Any::class.java -> true
        isA<Collection<*>>(type) || isA<Map<*, *>>(type) -> false
        shape(type) != Shape.None -> true
        Modifier.isAbstract(type.modifiers) -> holdsNoFields(type)
        else -> false
    }

private fun holdsNoFields(type: Class<*>): Boolean =
    generateSequence(type, Class<*>::getSuperclass).flatMap(::instanceFieldsOf).none()

private fun requireStringKeys(type: ClassType) {
    val keys = supertypeArguments(type, Map::class.java)?.firstOrNull() ?: return
    require(keys !is ClassType || keys.raw.isAssignableFrom(String::class.java)) {
        "No value is a $type, since the keys of a map that is a value are strings."
    }
}

private fun whyNot(type: Class<*>): String =
    when {
        type.isArray ->
            "No value is an array, such as ${type.simpleName}: a value holds a list instead."
        type.isRecord || isA<Enum<*>>(type) ->
            "No value is a ${type.name}: a record or an enum constant is a value only when its " +
                "module opens its package to org.itineradev.itinera, and an enum constant only " +
                "when its fields are final."
        isKotlin(type) && kotlinClasses == null ->
            "${type.name} is a Kotlin class, which itinera reads only with the artifact " +
                "org.itinera-dev:itinera-kotlin: add it to the application's dependencies."
        isA<Collection<*>>(type) || isA<Map<*, *>>(type) ->
            "No value is a ${type.name}: a value's lists, sets and maps are read as List, Set and " +
                "Map, or as ArrayList, LinkedHashSet and LinkedHashMap."
        else ->
            "No value is a ${type.name}: values are records, data classes, enum constants, " +
                "lists, sets, maps with string keys, and the JDK's immutable values."
    }

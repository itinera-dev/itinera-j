package org.itineradev.itinera.value

import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.Component
import org.itineradev.itinera.engine.Nullness
import org.itineradev.itinera.engine.ValueType
import org.itineradev.itinera.engine.VariableType

/**
 * Whether a value is of a type, as Java's `instanceof` says, with the type's arguments checked
 * against every element of a list or set, every value of a map and every component of a record or
 * data class, which the JVM erases. Nothing converts: an `Int` is not of `Long`. It calls no custom
 * code.
 */
internal fun isOf(value: Any?, type: ValueType): Boolean =
    when (type) {
        AnyValueType -> true
        is VariableType -> value != null || type.nullness != Nullness.NON_NULL
        is ClassType ->
            if (value == null) type.nullable else type.raw.isInstance(value) && holdsOf(value, type)
    }

private fun holdsOf(value: Any, type: ClassType): Boolean {
    val bindings = bindings(value.javaClass, type) ?: return false
    return when (val shape = shape(value.javaClass)) {
        Shape.Sequence,
        Shape.Distinct ->
            elementsAre(value as Iterable<*>, contentType(value, bindings, Iterable::class.java, 0))
        Shape.Keyed ->
            elementsAre(
                (value as Map<*, *>).values,
                contentType(value, bindings, Map::class.java, 1),
            )
        is Shape.Composite ->
            shape.components.all { component -> componentIsOf(value, component, bindings) }
        Shape.Shared,
        Shape.Floating,
        is Shape.Singleton,
        Shape.None -> true
    }
}

private fun elementsAre(elements: Iterable<*>, type: ValueType): Boolean = elements.all { element ->
    isOf(element, type)
}

private fun componentIsOf(
    value: Any,
    component: Component,
    bindings: Map<String, ValueType>,
): Boolean = isOf(component.field.get(value), substituted(component.type, bindings))

/** The type of a collection's elements or a map's values, as the value's class declares it. */
private fun contentType(
    value: Any,
    bindings: Map<String, ValueType>,
    target: Class<*>,
    argument: Int,
): ValueType =
    supertypeArguments(declared(value.javaClass), target)?.getOrNull(argument)?.let { declared ->
        substituted(declared, bindings)
    } ?: AnyValueType

/**
 * What a class's type variables stand for when its instance is read as a type, or `null` when the
 * type's arguments cannot be those the class gives that supertype.
 */
private fun bindings(type: Class<*>, requested: ClassType): Map<String, ValueType>? {
    val seen =
        if (requested.arguments.isEmpty()) null
        else supertypeArguments(declared(type), requested.raw)
    if (seen == null) return emptyMap()
    val bound = mutableMapOf<String, ValueType>()
    return bound.takeIf { unifiesAll(seen, requested.arguments, bound) }
}

private fun unifiesAll(
    own: List<ValueType>,
    asked: List<ValueType>,
    bound: MutableMap<String, ValueType>,
): Boolean = own.zip(asked).all { (inner, wanted) -> unify(inner, wanted, bound) }

private fun unify(own: ValueType, asked: ValueType, bound: MutableMap<String, ValueType>): Boolean =
    when {
        own is VariableType -> bind(own.name, asked, bound)
        own !is ClassType || asked !is ClassType -> true
        own.raw != asked.raw -> false
        else -> unifiesAll(own.arguments, asked.arguments, bound)
    }

private fun bind(name: String, type: ValueType, bound: MutableMap<String, ValueType>): Boolean {
    bound.putIfAbsent(name, type)
    return true
}

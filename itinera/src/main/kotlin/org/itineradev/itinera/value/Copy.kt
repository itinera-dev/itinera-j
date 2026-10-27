package org.itineradev.itinera.value

import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.IdentityHashMap
import org.itineradev.itinera.engine.Component

/**
 * A copy of a value that shares nothing that can change with it: every list, set and map is copied,
 * every record and data class rebuilt through its constructor from copied components, and the
 * immutable leaves shared.
 *
 * Throws [NotAValueException] when what it copies is not a value, and whatever custom code throws
 * while it copies: a constructor, or the `equals` and `hashCode` of a set's elements.
 */
internal fun copy(value: Any?): Any? = Copier().copy(value)

private class Copier {
    private val path: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
    private val checked: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

    fun copy(value: Any?): Any? =
        when (value) {
            null -> null
            else -> copyOf(value, shape(value.javaClass))
        }

    private fun copyOf(value: Any, shape: Shape): Any =
        when (shape) {
            Shape.Shared -> value
            Shape.Floating -> value.takeIf(::isFinite) ?: throw NotAValueException()
            Shape.Sequence -> inside(value) { (value as List<*>).mapTo(ArrayList(), ::copy) }
            Shape.Distinct -> inside(value) { (value as Set<*>).mapTo(LinkedHashSet(), ::copy) }
            Shape.Keyed ->
                inside(value) { (value as Map<*, *>).entries.associateTo(LinkedHashMap(), ::entry) }
            is Shape.Composite -> inside(value) { rebuilt(value, shape) }
            is Shape.Singleton -> value.also { checkSingleton(it, shape) }
            Shape.None -> throw NotAValueException()
        }

    private fun entry(entry: Map.Entry<*, *>): Pair<String, Any?> =
        (entry.key as? String ?: throw NotAValueException()) to copy(entry.value)

    private fun rebuilt(value: Any, shape: Shape.Composite): Any =
        construct(shape, shape.components.map { component -> copiedComponent(value, component) })

    /**
     * An enum constant or `data object` is shared, so every field must hold an immutable value: a
     * shared or floating one, another such constant, or a record holding only immutable values.
     */
    private fun checkSingleton(value: Any, shape: Shape.Singleton) {
        if (checked.add(value))
            shape.components.forEach { component -> checkImmutable(value, component) }
    }

    private fun checkImmutable(value: Any, component: Component) {
        requireImmutable(component.field.get(value))
    }

    private fun requireImmutable(value: Any?) {
        if (value != null) requireImmutable(value, shape(value.javaClass))
    }

    private fun requireImmutable(value: Any, shape: Shape) {
        when (shape) {
            Shape.Shared -> {}
            Shape.Floating -> if (!isFinite(value)) throw NotAValueException()
            is Shape.Singleton -> checkSingleton(value, shape)
            is Shape.Composite ->
                shape.components.forEach { component -> checkImmutable(value, component) }
            Shape.Sequence,
            Shape.Distinct,
            Shape.Keyed,
            Shape.None -> throw NotAValueException()
        }
    }

    private fun copiedComponent(value: Any, component: Component): Any? =
        copy(component.field.get(value))

    /** Copies a container, refusing one that holds itself, which no value does. */
    private fun <T> inside(value: Any, copier: () -> T): T {
        if (!path.add(value)) throw NotAValueException()
        try {
            return copier()
        } finally {
            path.remove(value)
        }
    }
}

private fun isFinite(value: Any): Boolean =
    when (value) {
        is Double -> value.isFinite()
        is Float -> value.isFinite()
        else -> false
    }

/** Builds a record or data class, throwing what its constructor throws. */
private fun construct(shape: Shape.Composite, arguments: List<Any?>): Any =
    try {
        shape.constructor.newInstance(*arguments.toTypedArray())
    } catch (thrown: InvocationTargetException) {
        throw thrown.cause ?: thrown
    }

package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Calls [method] on [target] with [arguments], and throws what the method itself threw, never the
 * reflective wrapper around it.
 *
 * Part of itinera's engine, not of its API.
 */
public fun invoked(method: Method, target: Any?, vararg arguments: Any?): Any? = unwrapped {
    method.invoke(target, *arguments)
}

/**
 * A new object made with [constructor] and [arguments], throwing what the constructor itself threw,
 * never the reflective wrapper around it.
 *
 * Part of itinera's engine, not of its API.
 */
public fun constructed(constructor: Constructor<*>, vararg arguments: Any?): Any = unwrapped {
    constructor.newInstance(*arguments)
}

/**
 * What a reflective call returned, as the type its caller declares, unchecked: a `null` that Java
 * code returned stays `null`, for the engine to find.
 *
 * Part of itinera's engine, not of its API.
 */
@Suppress("UNCHECKED_CAST") public fun <T> returned(value: Any?): T = value as T

/** The methods [type] itself declares, of every visibility. */
internal fun declaredMethods(type: Class<*>): Sequence<Method> = type.declaredMethods.asSequence()

private inline fun <T> unwrapped(call: () -> T): T =
    try {
        call()
    } catch (wrapper: InvocationTargetException) {
        throw wrapper.targetException ?: wrapper
    }

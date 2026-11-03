package org.itineradev.itinera.engine

import java.lang.reflect.Method

/**
 * The method [method] stands for: itself, or, for a bridge that javac adds so that a public class
 * makes public a method it inherits from a class that is not public, the method it inherits.
 */
internal fun bridged(method: Method): Method =
    if (isVisibilityBridge(method)) inherited(method) ?: method else method

/**
 * Whether [method] is a bridge that stands for no method of its own class, as a bridge for a
 * narrower override or a covariant return type stands for the override.
 */
private fun isVisibilityBridge(method: Method): Boolean =
    method.isBridge &&
        declaredMethods(method.declaringClass).none { other -> narrows(other, method) }

/** Whether [bridge] could forward to [other], a narrower method of the same name. */
private fun narrows(other: Method, bridge: Method): Boolean =
    !other.isBridge && other.name == bridge.name && takesAndReturnsWithin(other, bridge)

private fun takesAndReturnsWithin(other: Method, bridge: Method): Boolean =
    bridge.returnType.isAssignableFrom(other.returnType) &&
        other.parameterCount == bridge.parameterCount &&
        bridge.parameterTypes.zip(other.parameterTypes).all(::widens)

private fun widens(types: Pair<Class<*>, Class<*>>): Boolean =
    types.first.isAssignableFrom(types.second)

private fun inherited(bridge: Method): Method? =
    generateSequence(bridge.declaringClass.superclass, Class<*>::getSuperclass)
        .flatMap(::declaredMethods)
        .find { method -> isBridgedBy(method, bridge) }

private fun isBridgedBy(method: Method, bridge: Method): Boolean =
    !method.isBridge &&
        method.name == bridge.name &&
        method.parameterTypes.contentEquals(bridge.parameterTypes)

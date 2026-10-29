package org.itineradev.itinera.engine

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * The methods of [type] and of its superclasses marked [mark], the class's before its superclass's
 * and each class's in the order of their names, since Java reflection gives methods in no order. A
 * method that overrides another replaces it, whether or not it is marked.
 */
internal fun markedMethods(type: Class<*>, mark: Class<out Annotation>): Sequence<Method> =
    generateSequence(type, Class<*>::getSuperclass)
        .flatMap(::byName)
        .map(::bridged)
        .distinctBy(::overridden)
        .filter(::isDeclared)
        .filter { method -> method.isAnnotationPresent(mark) }

private fun byName(type: Class<*>): Sequence<Method> =
    declaredMethods(type).sortedWith(compareBy(Method::getName, Method::toString))

/**
 * What a method overriding [method] shares with it, its name and its parameters' types, or [method]
 * itself when nothing can override it. A bridge the compiler adds for a narrower override shares
 * the overridden method's erased types, so it hides that method.
 */
private fun overridden(method: Method): Any =
    if (isOverridable(method)) method.name to method.parameterTypes.toList() else method

private fun isOverridable(method: Method): Boolean =
    !Modifier.isPrivate(method.modifiers) && !Modifier.isStatic(method.modifiers)

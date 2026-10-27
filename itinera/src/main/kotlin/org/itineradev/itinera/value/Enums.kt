package org.itineradev.itinera.value

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.Component

/**
 * An enum constant is shared once its fields are checked to hold immutable values, so they must all
 * be final: those of its enum class, and of its own body when it has one. The enums of the JDK and
 * of Kotlin's standard library, such as `ChronoUnit` and `DurationUnit`, are immutable and shared
 * as they are, since neither opens their fields.
 */
internal fun enumShape(type: Class<*>): Shape =
    if (isPlatform(type) || isKotlinStandard(type)) Shape.Shared else applicationEnumShape(type)

private fun isPlatform(type: Class<*>): Boolean =
    type.classLoader == null || type.classLoader == ClassLoader.getPlatformClassLoader()

private fun isKotlinStandard(type: Class<*>): Boolean = type.packageName.startsWith("kotlin.")

private fun applicationEnumShape(type: Class<*>): Shape {
    val fields =
        generateSequence(type, Class<*>::getSuperclass)
            .takeWhile(::isBelowEnum)
            .flatMap(::instanceFieldsOf)
            .toList()
    return if (fields.all(::isFinal)) singleton(fields.map(::untyped)) else Shape.None
}

private fun isBelowEnum(type: Class<*>): Boolean = type != Enum::class.java

private fun isFinal(field: Field): Boolean = Modifier.isFinal(field.modifiers)

private fun untyped(field: Field): Component = Component(field, AnyValueType)

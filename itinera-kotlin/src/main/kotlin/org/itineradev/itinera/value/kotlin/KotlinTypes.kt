package org.itineradev.itinera.value.kotlin

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.Nullness
import org.itineradev.itinera.engine.ValueType
import org.itineradev.itinera.engine.VariableType

/** A Kotlin type, with Kotlin's nullability at every level: `List<Long?>` is not `List<Long>`. */
internal fun valueType(type: KType): ValueType =
    when (val classifier = type.classifier) {
        is KClass<*> ->
            ClassType(
                classifier.javaObjectType,
                type.arguments.map(::argumentType),
                type.isMarkedNullable,
            )
        is KTypeParameter -> VariableType(classifier.name, variableNullness(type))
        else -> AnyValueType
    }

private fun variableNullness(type: KType): Nullness =
    if (type.isMarkedNullable) Nullness.NULLABLE else Nullness.PARAMETRIC

/** A star projection or an `in` projection reads as any value; an `out` projection as its type. */
private fun argumentType(argument: KTypeProjection): ValueType =
    argument.type?.takeUnless { isContravariant(argument) }?.let(::valueType) ?: AnyValueType

private fun isContravariant(argument: KTypeProjection): Boolean = argument.variance == KVariance.IN

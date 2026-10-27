package org.itineradev.itinera.value

import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.AnnotatedWildcardType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.Nullness
import org.itineradev.itinera.engine.ValueType
import org.itineradev.itinera.engine.VariableType

/**
 * The type a Java declaration names, with whether `null` is of it at each level, as JSpecify says:
 * in null-marked code, a type admits `null` only when marked `@Nullable`; elsewhere, every
 * reference type admits it unless marked `@NonNull`.
 */
internal fun declaredType(type: AnnotatedType, nullMarked: Boolean): ValueType =
    when (val written = type.type) {
        is Class<*> -> ClassType(boxed(written), emptyList(), admitsNull(written, type, nullMarked))
        is ParameterizedType ->
            ClassType(
                written.rawType as Class<*>,
                (type as AnnotatedParameterizedType).annotatedActualTypeArguments.map { argument ->
                    declaredType(argument, nullMarked)
                },
                admitsNull(written, type, nullMarked),
            )
        is TypeVariable<*> -> VariableType(written.name, variableNullness(type, nullMarked))
        is WildcardType -> boundOf(type as AnnotatedWildcardType, nullMarked)
        else -> ClassType(Array<Any>::class.java, emptyList(), true)
    }

/**
 * The type a generic declaration names, from reflection alone, with no nullness: what a class
 * passes to its supertypes' type parameters.
 */
internal fun genericType(type: Type): ValueType =
    when (type) {
        is Class<*> -> ClassType(boxed(type), emptyList(), true)
        is ParameterizedType ->
            ClassType(type.rawType as Class<*>, type.actualTypeArguments.map(::genericType), true)
        is TypeVariable<*> -> VariableType(type.name, Nullness.PARAMETRIC)
        is GenericArrayType -> ClassType(Array<Any>::class.java, emptyList(), true)
        else -> AnyValueType
    }

private fun boundOf(type: AnnotatedWildcardType, nullMarked: Boolean): ValueType =
    type.annotatedUpperBounds
        .singleOrNull()
        ?.takeUnless { bound -> isUnbounded(type, bound) }
        ?.let { bound -> declaredType(bound, nullMarked) } ?: AnyValueType

/** Whether a wildcard says nothing of its type: `?`, `? extends Object` or `? super T`. */
private fun isUnbounded(type: AnnotatedWildcardType, upper: AnnotatedType): Boolean =
    type.annotatedLowerBounds.isNotEmpty() || upper.type == Any::class.java

/** The class whose instances a value of this class is: the wrapper of a primitive. */
internal fun boxed(type: Class<*>): Class<*> = type.kotlin.javaObjectType

package org.itineradev.itinera.value

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Executable
import java.lang.reflect.Type
import org.itineradev.itinera.engine.Nullness

private const val JSPECIFY = "org.jspecify.annotations."

/** Whether a class is null-marked, by JSpecify's rules: its innermost mark decides. */
internal fun isNullMarked(type: Class<*>): Boolean =
    enclosingScopes(type).firstNotNullOfOrNull(::mark) ?: false

/**
 * Whether a constructor or a method is null-marked, by JSpecify's rules: its own mark, or else its
 * class's.
 */
internal fun isNullMarked(executable: Executable): Boolean =
    mark(executable) ?: isNullMarked(executable.declaringClass)

private fun enclosingScopes(type: Class<*>): Sequence<AnnotatedElement> =
    generateSequence(type, Class<*>::getEnclosingClass).flatMap(::scopesOf) +
        listOfNotNull(type.`package`, type.module)

private fun scopesOf(type: Class<*>): Sequence<AnnotatedElement> =
    sequenceOf(type, type.enclosingMethod, type.enclosingConstructor).filterNotNull()

private fun mark(scope: AnnotatedElement): Boolean? =
    when {
        has(scope, "NullMarked") -> true
        has(scope, "NullUnmarked") -> false
        else -> null
    }

private fun has(element: AnnotatedElement, name: String): Boolean =
    element.annotations.any { annotation -> isJSpecify(annotation, name) }

private fun isJSpecify(annotation: Annotation, name: String): Boolean =
    annotation.annotationClass.java.name == JSPECIFY + name

internal fun admitsNull(written: Type, type: AnnotatedType, nullMarked: Boolean): Boolean =
    when {
        written is Class<*> && written.isPrimitive -> false
        has(type, "Nullable") -> true
        has(type, "NonNull") -> false
        else -> !nullMarked
    }

internal fun variableNullness(type: AnnotatedType, nullMarked: Boolean): Nullness =
    when {
        has(type, "Nullable") -> Nullness.NULLABLE
        has(type, "NonNull") -> Nullness.NON_NULL
        nullMarked -> Nullness.PARAMETRIC
        else -> Nullness.NULLABLE
    }

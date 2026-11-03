package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.validate

/**
 * Whether KSP resolves every type that [type] declares, and every type that the classes its
 * annotations name declare, such as the steps a workflow names and what they request, their
 * superclasses' included.
 */
internal fun isResolved(type: KSClassDeclaration): Boolean {
    val named = type.annotations.flatMap(::valuesOf).flatMap(::typesIn).toList()
    return named.none(KSType::isError) &&
        (listOf(type) + named.mapNotNull(::classOf)).all(::isWhollyValid)
}

/** Whether KSP resolves every type [type] declares, and every type its supertypes declare. */
private fun isWhollyValid(type: KSClassDeclaration): Boolean =
    isValid(type) && type.getAllSuperTypes().all(::isValidType)

private fun isValidType(type: KSType): Boolean =
    !type.isError && classOf(type)?.let(::isValid) ?: true

private fun classOf(type: KSType): KSClassDeclaration? = type.declaration as? KSClassDeclaration

private fun isValid(type: KSClassDeclaration): Boolean = type.validate(enableNewFeatures = true)

private fun valuesOf(annotation: KSAnnotation): Sequence<Any?> =
    annotation.arguments.asSequence().map(KSValueArgument::value)

/** The types [value] holds, an annotation's value: a class, or classes in lists and annotations. */
private fun typesIn(value: Any?): Sequence<KSType> =
    when (value) {
        is KSType -> sequenceOf(value)
        is KSAnnotation -> valuesOf(value).flatMap(::typesIn)
        is List<*> -> value.asSequence().flatMap(::typesIn)
        is Array<*> -> value.asSequence().flatMap(::typesIn)
        else -> emptySequence()
    }

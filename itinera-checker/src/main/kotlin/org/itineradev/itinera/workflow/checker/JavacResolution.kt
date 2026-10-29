package org.itineradev.itinera.workflow.checker

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Whether every class that the annotations of [type] name is one javac knows: one that another
 * processor generates in a later round is not known yet, nor is one whose name is mistyped.
 */
internal fun namesOnlyKnownClasses(type: TypeElement): Boolean =
    type.annotationMirrors.all(::isResolved)

private fun isResolved(mirror: AnnotationMirror): Boolean =
    mirror.elementValues.entries.all(::isResolved)

private fun isResolved(entry: Map.Entry<ExecutableElement, AnnotationValue>): Boolean =
    isResolved(entry.key, entry.value)

private fun isResolved(member: ExecutableElement, value: AnnotationValue): Boolean =
    when (val held = value.value) {
        is AnnotationMirror -> isResolved(held)
        is List<*> ->
            held.filterIsInstance<AnnotationValue>().all { item -> isResolved(member, item) }
        else -> !namesClasses(member) || isKnownClass(held)
    }

/** Whether the element [member] of an annotation holds a class or classes. */
private fun namesClasses(member: ExecutableElement): Boolean {
    val type = member.returnType
    val held = if (type.kind == TypeKind.ARRAY) (type as ArrayType).componentType else type
    return held.kind == TypeKind.DECLARED &&
        ((held as DeclaredType).asElement() as TypeElement).qualifiedName.contentEquals(CLASS)
}

private fun isKnownClass(held: Any?): Boolean = held is TypeMirror && held.kind != TypeKind.ERROR

private const val CLASS = "java.lang.Class"

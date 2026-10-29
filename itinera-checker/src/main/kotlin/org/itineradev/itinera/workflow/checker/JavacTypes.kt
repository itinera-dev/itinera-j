package org.itineradev.itinera.workflow.checker

import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType

/**
 * Reads the types javac knows as the run-time reader reads a Java declaration's: with whether
 * `null` is of them at each level, as JSpecify says. In null-marked code, a type admits `null` only
 * when marked `@Nullable`; elsewhere, every reference type admits it unless marked `@NonNull`.
 */
internal class JavacTypes(private val symbols: JavacSymbols) {
    /** [type], as a declaration in null-marked code writes it when [nullMarked]. */
    fun of(type: TypeMirror, nullMarked: Boolean): TypeUse =
        when (type.kind) {
            TypeKind.VOID -> VoidUse
            TypeKind.DECLARED -> declared(type as DeclaredType, nullMarked)
            TypeKind.ARRAY -> ArrayUse(arrayName(type as ArrayType), admitsNull(type, nullMarked))
            TypeKind.TYPEVAR ->
                VariableUse(
                    (type as TypeVariable).asElement().simpleName.toString(),
                    nullness(type, nullMarked),
                )
            TypeKind.WILDCARD -> boundOf(type as WildcardType, nullMarked)
            else -> if (type.kind.isPrimitive) primitive(type as PrimitiveType) else AnyUse
        }

    /** A supertype a class names, read with no nullness, as reflection reads a generic one. */
    fun supertype(type: TypeMirror): ClassUse = of(type, nullMarked = false) as ClassUse

    private fun declared(type: DeclaredType, nullMarked: Boolean): ClassUse =
        ClassUse(
            symbols.classOf(type.asElement() as TypeElement),
            type.typeArguments.map { argument -> of(argument, nullMarked) },
            admitsNull(type, nullMarked),
            primitive = false,
        )

    private fun primitive(type: PrimitiveType): ClassUse =
        ClassUse(
            symbols.classOf(symbols.types.boxedClass(type)),
            emptyList(),
            false,
            primitive = true,
        )

    private fun boundOf(type: WildcardType, nullMarked: Boolean): TypeUse {
        val upper = type.extendsBound
        return if (type.superBound != null || upper == null || isObject(upper)) AnyUse
        else of(upper, nullMarked)
    }

    private fun isObject(type: TypeMirror): Boolean =
        type.kind == TypeKind.DECLARED &&
            ((type as DeclaredType).asElement() as TypeElement)
                .qualifiedName
                .contentEquals(Names.OBJECT)

    /**
     * The simple name of an array's class: its component's with `[]`, or `Object[]` for an array of
     * a type variable or of a parameterized type, whose class reflection cannot name.
     */
    private fun arrayName(type: ArrayType): String =
        when (val component = type.componentType) {
            is ArrayType -> arrayOf(arrayName(component))
            is DeclaredType ->
                if (component.typeArguments.isEmpty()) "${component.asElement().simpleName}[]"
                else OBJECTS
            is PrimitiveType -> "${component.kind.name.lowercase()}[]"
            else -> OBJECTS
        }
}

private const val OBJECTS = "Object[]"

/** The simple name of the class of an array of arrays of [component]. */
private fun arrayOf(component: String): String =
    if (component == OBJECTS) OBJECTS else "$component[]"

private fun admitsNull(type: TypeMirror, nullMarked: Boolean): Boolean =
    when {
        has(type, Names.NULLABLE) -> true
        has(type, Names.NON_NULL) -> false
        else -> !nullMarked
    }

private fun nullness(type: TypeMirror, nullMarked: Boolean): Nullness =
    when {
        has(type, Names.NULLABLE) -> Nullness.NULLABLE
        has(type, Names.NON_NULL) -> Nullness.NON_NULL
        nullMarked -> Nullness.PARAMETRIC
        else -> Nullness.NULLABLE
    }

private fun has(construct: AnnotatedConstruct, name: String): Boolean =
    construct.annotationMirrors.any { mirror -> isOfClass(mirror, name) }

private fun isOfClass(mirror: AnnotationMirror, name: String): Boolean =
    (mirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals(name)

/**
 * Whether [element] is null-marked, by JSpecify's rules: its innermost mark decides, among itself,
 * the classes, methods and constructors enclosing it, its package and its module.
 */
internal fun isNullMarked(element: Element): Boolean =
    generateSequence(element, Element::getEnclosingElement).firstNotNullOfOrNull(::mark) ?: false

private fun mark(scope: Element): Boolean? =
    when {
        has(scope, Names.NULL_MARKED) -> true
        has(scope, Names.NULL_UNMARKED) -> false
        else -> null
    }

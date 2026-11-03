package org.itineradev.itinera.workflow.checker

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/** A method or a constructor javac knows, as the checker reads it. */
internal class JavacFunction(
    val element: ExecutableElement,
    override val owner: JavacClass,
    private val symbols: JavacSymbols,
) : FunctionSymbol {
    private val nullMarked = isNullMarked(element)

    override val isConstructor: Boolean = element.kind == ElementKind.CONSTRUCTOR
    override val jvmName: String = element.simpleName.toString()
    override val name: String = jvmName
    override val isPublic: Boolean = Modifier.PUBLIC in element.modifiers
    override val isPrivate: Boolean = Modifier.PRIVATE in element.modifiers
    override val isStatic: Boolean = Modifier.STATIC in element.modifiers
    override val isSuspend: Boolean = false
    override val origin: Any = element
    override val capturesEnclosing: Boolean = isConstructor && owner.isInner

    override val returns: TypeUse by lazy {
        if (isConstructor) VoidUse else symbols.typeUses.of(element.returnType, nullMarked)
    }

    override val returnClass: ClassSymbol? by lazy { symbols.erasure.classOf(element.returnType) }

    override val returnName: String by lazy { symbols.erasure.simpleNameOf(element.returnType) }

    override val returnArguments: List<String?> by lazy {
        (element.returnType as? DeclaredType)
            ?.typeArguments
            .orEmpty()
            .map(symbols.erasure::argumentOf)
    }

    override val parameters: List<ParameterSymbol> by lazy {
        element.parameters.map(::parameterOf)
    }

    private fun parameterOf(parameter: VariableElement): ParameterSymbol =
        JavacParameter(parameter, nullMarked, symbols)

    override fun overrides(other: FunctionSymbol): Boolean =
        other is JavacFunction && overrides(other.element)

    private fun overrides(other: ExecutableElement): Boolean =
        symbols.elements.overrides(element, other, element.enclosingElement as TypeElement)

    override fun annotation(name: String): AnnotationSymbol? =
        symbols.annotations(element, name).firstOrNull()
}

/** A parameter javac knows, whose type is read as null-marked code reads it when [nullMarked]. */
internal class JavacParameter(
    private val element: VariableElement,
    nullMarked: Boolean,
    private val symbols: JavacSymbols,
) : ParameterSymbol {
    override val name: String = element.simpleName.toString()
    override val type: TypeUse = symbols.typeUses.of(element.asType(), nullMarked)
    override val jvmClass: String = symbols.erasure.nameOf(element.asType())
    override val erasedClass: ClassSymbol? by lazy { symbols.erasure.classOf(element.asType()) }

    override fun annotation(name: String): AnnotationSymbol? =
        symbols.annotations(element, name).firstOrNull()
}

/** An annotation javac knows, with the value of each of its elements, written or by default. */
internal class JavacAnnotation(
    private val mirror: AnnotationMirror,
    private val symbols: JavacSymbols,
) : AnnotationSymbol {
    private val values: Map<String, Any?> =
        symbols.elements.getElementValuesWithDefaults(mirror).entries.associate(::entryOf)

    override fun string(element: String): String = values[element] as String

    override fun strings(element: String): List<String> =
        elementsOf(element).map(AnnotationValue::getValue).filterIsInstance<String>()

    override fun boolean(element: String): Boolean = values[element] as Boolean

    override fun int(element: String): Int = values[element] as Int

    override fun type(element: String): ClassSymbol = classOf(values[element] as TypeMirror)

    override fun types(element: String): List<ClassSymbol> =
        elementsOf(element)
            .map(AnnotationValue::getValue)
            .filterIsInstance<TypeMirror>()
            .map(::classOf)

    /** The annotations [element] holds, as a repeatable annotation's container holds them. */
    fun nested(element: String): List<AnnotationSymbol> =
        elementsOf(element)
            .map(AnnotationValue::getValue)
            .filterIsInstance<AnnotationMirror>()
            .map(::annotationOf)

    private fun annotationOf(mirror: AnnotationMirror): AnnotationSymbol =
        JavacAnnotation(mirror, symbols)

    private fun elementsOf(element: String): List<AnnotationValue> =
        (values[element] as List<*>).filterIsInstance<AnnotationValue>()

    private fun classOf(type: TypeMirror): ClassSymbol =
        symbols.classOf((type as DeclaredType).asElement() as TypeElement)
}

private fun entryOf(entry: Map.Entry<ExecutableElement, AnnotationValue>): Pair<String, Any?> =
    entry.key.simpleName.toString() to entry.value.value

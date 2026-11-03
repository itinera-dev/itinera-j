package org.itineradev.itinera.workflow.checker

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/** The classes javac knows, as the checker reads them, each read once. */
internal class JavacSymbols(val elements: Elements, val types: Types) {
    private val classes = HashMap<TypeElement, JavacClass>()
    val typeUses = JavacTypes(this)
    val erasure = JavacErasure(this)

    /** The class [element] declares. */
    fun classOf(element: TypeElement): JavacClass =
        classes.getOrPut(element) { JavacClass(element, this) }

    /** The method or constructor [element] declares. */
    fun functionOf(element: ExecutableElement): FunctionSymbol =
        JavacFunction(element, classOf(element.enclosingElement as TypeElement), this)

    /** The class whose binary name is [name], if javac knows it. */
    fun named(name: String): TypeElement? = elements.getTypeElement(name.replace('$', '.'))

    /**
     * The annotations marking [element] of the class [name], unpacking a repeatable's container.
     */
    fun annotations(element: Element, name: String): List<AnnotationSymbol> =
        element.annotationMirrors.flatMap { mirror -> marking(mirror, name) }

    private fun marking(mirror: AnnotationMirror, name: String): List<AnnotationSymbol> {
        val type = binaryNameOf(mirror.annotationType)
        return when (type) {
            name -> listOf(JavacAnnotation(mirror, this))
            "$name\$Container" -> JavacAnnotation(mirror, this).nested("value")
            else -> emptyList()
        }
    }

    /** The binary name of the class [type] names. */
    fun binaryNameOf(type: DeclaredType): String =
        elements.getBinaryName(type.asElement() as TypeElement).toString()
}

/** A class javac knows, as the checker reads it. */
internal class JavacClass(private val element: TypeElement, private val symbols: JavacSymbols) :
    ClassSymbol {
    override val name: String = symbols.elements.getBinaryName(element).toString()
    override val simpleName: String = element.simpleName.toString()
    override val origin: Any = element
    override val isInterface: Boolean = element.kind.isInterface
    override val isAbstract: Boolean = isInterface || Modifier.ABSTRACT in element.modifiers
    override val isRecord: Boolean = element.kind == ElementKind.RECORD
    override val isEnum: Boolean = element.kind == ElementKind.ENUM
    override val isKotlin: Boolean = symbols.annotations(element, Names.METADATA).isNotEmpty()
    override val kotlinData: Boolean? = null
    override val typeParameters: List<String> = element.typeParameters.map(::simpleNameOf)

    override val superclass: ClassSymbol? by lazy {
        (element.superclass as? DeclaredType)?.asElement()?.let { superclass ->
            symbols.classOf(superclass as TypeElement)
        }
    }

    override val supertypes: List<ClassUse> by lazy {
        (listOf(element.superclass) + element.interfaces)
            .filter(::isDeclared)
            .map(symbols.typeUses::supertype)
    }

    override val constructors: List<FunctionSymbol> by lazy {
        ElementFilter.constructorsIn(element.enclosedElements).map(symbols::functionOf)
    }

    override val methods: List<FunctionSymbol> by lazy {
        ElementFilter.methodsIn(element.enclosedElements).map(symbols::functionOf)
    }

    override val publicMethods: List<FunctionSymbol> by lazy {
        ElementFilter.methodsIn(symbols.elements.getAllMembers(element))
            .filter(::isPublicMember)
            .map(symbols::functionOf)
    }

    override val instanceFields: List<Boolean> by lazy {
        ElementFilter.fieldsIn(element.enclosedElements).filterNot(::isStatic).map(::isFinal)
    }

    /** Whether it is an inner class, which holds the instance of the class enclosing it. */
    val isInner: Boolean =
        element.nestingKind != NestingKind.TOP_LEVEL &&
            Modifier.STATIC !in element.modifiers &&
            !element.kind.isInterface &&
            element.kind != ElementKind.ENUM &&
            element.kind != ElementKind.RECORD

    override fun annotation(name: String): AnnotationSymbol? = annotations(name).firstOrNull()

    override fun annotations(name: String): List<AnnotationSymbol> =
        symbols.annotations(element, name)

    override fun isSubtypeOf(name: String): Boolean {
        val target = symbols.named(name) ?: return false
        return symbols.types.isSubtype(
            symbols.types.erasure(element.asType()),
            symbols.types.erasure(target.asType()),
        )
    }
}

/** Whether a member of a class is a public method that `Class.getMethods` gives. */
private fun isPublicMember(method: ExecutableElement): Boolean =
    Modifier.PUBLIC in method.modifiers &&
        !(Modifier.STATIC in method.modifiers && method.enclosingElement.kind.isInterface)

private fun simpleNameOf(element: Element): String = element.simpleName.toString()

private fun isDeclared(type: TypeMirror): Boolean = type.kind == TypeKind.DECLARED

private fun isStatic(element: Element): Boolean = Modifier.STATIC in element.modifiers

private fun isFinal(element: Element): Boolean = Modifier.FINAL in element.modifiers

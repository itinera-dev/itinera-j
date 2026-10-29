package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin

/** A class KSP knows, as the checker reads it, as the JVM holds it. */
internal class KspClass(
    private val declaration: KSClassDeclaration,
    private val symbols: KspSymbols,
) : ClassSymbol {
    override val name: String = binaryNameOf(declaration)
    override val simpleName: String = declaration.simpleName.asString()
    override val origin: Any = declaration
    override val isInterface: Boolean = declaration.classKind in interfaceKinds
    override val isAbstract: Boolean = isInterface || declaration.isAbstract()
    override val isEnum: Boolean = declaration.classKind == ClassKind.ENUM_CLASS
    override val isKotlin: Boolean = declaration.origin in kotlinOrigins
    override val typeParameters: List<String> = declaration.typeParameters.map(::nameOf)

    override val isRecord: Boolean by lazy { isSubtypeOf(RECORD) && name != RECORD }

    override val kotlinData: Boolean? by lazy { if (isKotlin) isData(this, declaration) else null }

    override val superclass: ClassSymbol? by lazy {
        if (isInterface || name == Names.OBJECT) null
        else supertypeClasses().find(::isClass)?.let(symbols::classOf) ?: objectClass()
    }

    override val supertypes: List<ClassUse> by lazy {
        if (name == Names.OBJECT) emptyList()
        else declaration.superTypes.map(symbols.typeUses::of).filterIsInstance<ClassUse>().toList()
    }

    override val constructors: List<FunctionSymbol> by lazy {
        declaration.getConstructors().flatMap(::onTheJvm).toList()
    }

    override val methods: List<FunctionSymbol> by lazy {
        declaration
            .getDeclaredFunctions()
            .filterNot(KSFunctionDeclaration::isConstructor)
            .map(::own)
            .toList()
    }

    override val publicMethods: List<FunctionSymbol> by lazy {
        declaration
            .getAllFunctions()
            .filterNot(KSFunctionDeclaration::isConstructor)
            .map(symbols::functionOf)
            .filter(FunctionSymbol::isPublic)
            .toList()
    }

    override val instanceFields: List<Boolean> by lazy {
        if (declaration.classKind == ClassKind.OBJECT) emptyList()
        else declaration.getDeclaredProperties().filter(::isField).map(::isFinal).toList()
    }

    /** Whether it is an inner class, which holds the instance of the class enclosing it. */
    val isInner: Boolean = Modifier.INNER in declaration.modifiers

    override fun annotation(name: String): AnnotationSymbol? = annotations(name).firstOrNull()

    override fun annotations(name: String): List<AnnotationSymbol> =
        symbols.annotations(declaration, name)

    override fun isSubtypeOf(name: String): Boolean =
        this.name == name || declaration.getAllSuperTypes().any { type -> isNamed(type, name) }

    private fun supertypeClasses(): Sequence<KSClassDeclaration> =
        declaration.superTypes.map(KSTypeReference::resolve).mapNotNull(::declarationOf)

    private fun objectClass(): ClassSymbol? = symbols.classNamed(Names.OBJECT)

    private fun own(function: KSFunctionDeclaration): FunctionSymbol =
        KspFunction(function, this, symbols)

    /**
     * The constructors the JVM holds for [constructor]: itself, and those Kotlin adds, one for each
     * parameter with a default value under `@JvmOverloads`, or, for a primary constructor whose
     * every parameter has one, a constructor that takes nothing.
     */
    private fun onTheJvm(constructor: KSFunctionDeclaration): List<FunctionSymbol> {
        val parameters = constructor.parameters
        val defaults = parameters.count(KSValueParameter::hasDefault)
        val added =
            when {
                symbols.isMarked(constructor, JVM_OVERLOADS) -> defaults
                isTakingNothingAdded(constructor) -> 1
                else -> 0
            }
        return (parameters.size - added..parameters.size).reversed().map { count ->
            taking(constructor, count)
        }
    }

    private fun taking(constructor: KSFunctionDeclaration, count: Int): FunctionSymbol =
        KspFunction(constructor, this, symbols, count)

    private fun isTakingNothingAdded(constructor: KSFunctionDeclaration): Boolean =
        isKotlin &&
            constructor == declaration.primaryConstructor &&
            constructor.parameters.isNotEmpty() &&
            constructor.parameters.all(KSValueParameter::hasDefault)

    private fun isNamed(type: KSType, name: String): Boolean =
        declarationOf(type)?.let(symbols::classOf)?.name == name
}

private const val RECORD = "java.lang.Record"

private const val JVM_OVERLOADS = "kotlin.jvm.JvmOverloads"

private val interfaceKinds = setOf(ClassKind.INTERFACE, ClassKind.ANNOTATION_CLASS)

private val kotlinOrigins = setOf(Origin.KOTLIN, Origin.KOTLIN_LIB)

private fun declarationOf(type: KSType): KSClassDeclaration? =
    type.declaration as? KSClassDeclaration

private fun isClass(type: KSClassDeclaration): Boolean = type.classKind == ClassKind.CLASS

private fun isField(property: KSPropertyDeclaration): Boolean =
    isStored(property) && Modifier.JAVA_STATIC !in property.modifiers

/** Whether the JVM holds a field for [property]: its backing field, or its delegate. */
internal fun isStored(property: KSPropertyDeclaration): Boolean =
    property.hasBackingField || property.isDelegated()

/** Whether the field the JVM holds for [property] is final, as a delegate's always is. */
internal fun isFinal(property: KSPropertyDeclaration): Boolean =
    property.isDelegated() || !property.isMutable

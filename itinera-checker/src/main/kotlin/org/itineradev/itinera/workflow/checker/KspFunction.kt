package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility

/**
 * A method or a constructor KSP knows, as the checker reads it: one the JVM holds, taking the first
 * [count] of the parameters its declaration writes, fewer for a constructor Kotlin adds for default
 * values.
 */
internal class KspFunction(
    private val declaration: KSFunctionDeclaration,
    override val owner: KspClass,
    private val symbols: KspSymbols,
    count: Int = declaration.parameters.size,
) : FunctionSymbol {
    private val signature by lazy { symbols.signatureOf(declaration)?.let(::JvmSignature) }

    override val isConstructor: Boolean = declaration.isConstructor()
    override val name: String = declaration.simpleName.asString()
    override val jvmName: String =
        if (isConstructor) name else symbols.jvmNameOf(declaration) ?: name
    override val isPublic: Boolean = declaration.getVisibility() in publicOnTheJvm
    override val isPrivate: Boolean = declaration.getVisibility() == Visibility.PRIVATE
    override val isStatic: Boolean =
        Modifier.JAVA_STATIC in declaration.modifiers || symbols.isMarked(declaration, JVM_STATIC)
    override val isSuspend: Boolean = Modifier.SUSPEND in declaration.modifiers
    override val origin: Any = declaration
    override val capturesEnclosing: Boolean = isConstructor && owner.isInner

    override val returns: TypeUse by lazy {
        if (isConstructor || returnsVoid()) VoidUse
        else declaration.returnType?.let(symbols.typeUses::of) ?: AnyUse
    }

    override val returnClass: ClassSymbol? by lazy {
        if (owner.isKotlin) kotlinReturnClass() else symbols.classNamed(javaReturn())
    }

    override val returnName: String by lazy {
        if (owner.isKotlin) kotlinReturnName() else simpleNameOfJvm(javaReturn())
    }

    override val returnArguments: List<String?> by lazy {
        val returned = declaration.returnType?.resolve()?.let(symbols.aliases::expanded)
        val arguments = returned?.arguments.orEmpty()
        if (owner.isKotlin) arguments.map(symbols.erasure::kotlinArgumentOf)
        else arguments.map(symbols.erasure::javaArgumentOf)
    }

    override val parameters: List<ParameterSymbol> by lazy {
        declaration.parameters.take(count).mapIndexed(::parameterOf)
    }

    override fun overrides(other: FunctionSymbol): Boolean =
        other is KspFunction && symbols.resolver.overrides(declaration, other.declaration)

    override fun annotation(name: String): AnnotationSymbol? =
        symbols.annotations(declaration, name).firstOrNull()

    /**
     * Kotlin's class of what it returns, as its Java object type: `Object` for a type parameter,
     * and `null` for an array.
     */
    private fun kotlinReturnClass(): ClassSymbol? =
        when (val type = returns) {
            is ClassUse -> type.raw
            is ArrayUse -> null
            is VariableUse,
            AnyUse,
            VoidUse -> symbols.classNamed(Names.OBJECT)
        }

    private fun kotlinReturnName(): String =
        when (val type = returns) {
            is ClassUse -> type.raw.simpleName
            is ArrayUse -> type.simpleName
            is VariableUse,
            AnyUse,
            VoidUse -> OBJECT
        }

    private fun javaReturn(): String = signature?.returns ?: Names.OBJECT

    /** Whether it is a Java method declared `void`, which KSP reads as returning `Unit`. */
    private fun returnsVoid(): Boolean = !owner.isKotlin && javaReturn() == VOID

    private fun parameterOf(index: Int, parameter: KSValueParameter): ParameterSymbol =
        KspParameter(parameter, jvmClassOf(index), symbols)

    /**
     * The JVM class of the parameter at [index], after what the JVM passes before those the
     * declaration writes, such as the enclosing instance an inner class's constructor takes, and
     * before what it passes after them, as the continuation of a `suspend` function.
     */
    private fun jvmClassOf(index: Int): String {
        val classes = signature?.parameters.orEmpty()
        val before = if (isSuspend) 0 else classes.size - declaration.parameters.size
        return classes.getOrNull(before + index) ?: Names.OBJECT
    }
}

/** A parameter KSP knows, whose class on the JVM is [jvmClass], as the checker reads it. */
internal class KspParameter(
    private val parameter: KSValueParameter,
    override val jvmClass: String,
    private val symbols: KspSymbols,
) : ParameterSymbol {
    override val name: String = parameter.name?.asString().orEmpty()
    override val type: TypeUse by lazy { symbols.typeUses.of(parameter.type) }
    override val erasedClass: ClassSymbol? by lazy { symbols.classNamed(jvmClass) }

    override fun annotation(name: String): AnnotationSymbol? =
        symbols.annotations(parameter, name).firstOrNull()
}

private const val JVM_STATIC = "kotlin.jvm.JvmStatic"

private const val OBJECT = "Object"

private const val VOID = "void"

private val publicOnTheJvm = setOf(Visibility.PUBLIC, Visibility.INTERNAL)

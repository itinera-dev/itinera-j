package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

/** The classes KSP knows, as the checker reads them, each read once. */
internal class KspSymbols(val resolver: Resolver) {
    private val classes = HashMap<String, KspClass>()
    val typeUses = KspTypes(this)
    val erasure = KspErasure(this)
    val aliases = KspAliases(resolver)

    /**
     * The class [declaration] declares, as the JVM knows it: a class Kotlin maps to a Java one,
     * such as `kotlin.String`, is read as the Java class, `java.lang.String`.
     */
    fun classOf(declaration: KSClassDeclaration): KspClass {
        val java = javaOf(declaration) ?: declaration
        return classes.getOrPut(binaryNameOf(java)) { KspClass(java, this) }
    }

    /** The class whose binary name is [name], if KSP knows it. */
    fun named(name: String): KSClassDeclaration? =
        resolver.getClassDeclarationByName(resolver.getKSNameFromString(name.replace('$', '.')))

    /**
     * The class whose binary name is [name], as the checker reads it, if KSP knows it, as it does
     * not know a primitive type or an array.
     */
    fun classNamed(name: String): ClassSymbol? =
        if (name.startsWith("[")) null else named(name)?.let(::classOf)

    /** Whether the annotation of the class [name] marks [annotated]. */
    fun isMarked(annotated: KSAnnotated, name: String): Boolean =
        annotations(annotated, name).isNotEmpty()

    /** The method or constructor [declaration] declares. */
    fun functionOf(declaration: KSFunctionDeclaration): FunctionSymbol =
        KspFunction(declaration, ownerOf(declaration), this)

    /** The class that declares [declaration]. */
    fun ownerOf(declaration: KSFunctionDeclaration): KspClass =
        classOf(declaration.parentDeclaration as KSClassDeclaration)

    /**
     * The annotations marking [annotated] of the class [name], unpacking a repeatable's container.
     */
    fun annotations(annotated: KSAnnotated, name: String): List<AnnotationSymbol> =
        annotated.annotations.toList().flatMap { annotation -> marking(annotation, name) }

    private fun marking(annotation: KSAnnotation, name: String): List<AnnotationSymbol> {
        val type = annotation.annotationType.resolve().declaration as? KSClassDeclaration
        return when (type?.let(::binaryNameOf)) {
            name -> listOf(KspAnnotation(annotation, this))
            "$name\$Container" -> KspAnnotation(annotation, this).nested("value")
            else -> emptyList()
        }
    }

    /** The name of [function] on the JVM, which Kotlin changes for an `internal` function. */
    @OptIn(KspExperimental::class)
    fun jvmNameOf(function: KSFunctionDeclaration): String? = resolver.getJvmName(function)

    /** The JVM descriptor of [function], such as `(J)Ljava/lang/String;`. */
    @OptIn(KspExperimental::class)
    fun signatureOf(function: KSFunctionDeclaration): String? = resolver.mapToJvmSignature(function)

    @OptIn(KspExperimental::class)
    private fun javaOf(declaration: KSClassDeclaration): KSClassDeclaration? =
        declaration.qualifiedName
            ?.let(resolver::mapKotlinNameToJava)
            ?.let(resolver::getClassDeclarationByName)
}

/** The binary name of the class [declaration] declares, as `Class.getName` writes it. */
internal fun binaryNameOf(declaration: KSClassDeclaration): String {
    val nesting =
        generateSequence(declaration, ::enclosingClass).map(::simpleNameOf).toList().asReversed()
    val inPackage = declaration.packageName.asString()
    val name = nesting.joinToString("$")
    return if (inPackage.isEmpty()) name else "$inPackage.$name"
}

private fun enclosingClass(declaration: KSClassDeclaration): KSClassDeclaration? =
    declaration.parentDeclaration as? KSClassDeclaration

private fun simpleNameOf(declaration: KSClassDeclaration): String =
    declaration.simpleName.asString()

/** An annotation KSP knows, with the value of each of its elements, written or by default. */
internal class KspAnnotation(
    annotation: KSAnnotation,
    private val symbols: KspSymbols,
) : AnnotationSymbol {
    private val values: Map<String, Any?> =
        (annotation.defaultArguments + annotation.arguments).associate(::entry)

    override fun string(element: String): String = values[element] as String

    override fun strings(element: String): List<String> = listed(element).filterIsInstance<String>()

    override fun boolean(element: String): Boolean = values[element] as Boolean

    override fun int(element: String): Int = values[element] as Int

    override fun type(element: String): ClassSymbol = classOf(values[element] as KSType)

    override fun types(element: String): List<ClassSymbol> =
        listed(element).filterIsInstance<KSType>().map(::classOf)

    /** The annotations [element] holds, as a repeatable annotation's container holds them. */
    fun nested(element: String): List<AnnotationSymbol> =
        listed(element).filterIsInstance<KSAnnotation>().map(::annotationOf)

    private fun listed(element: String): List<Any?> =
        when (val value = values[element]) {
            is List<*> -> value
            is Array<*> -> value.toList()
            else -> listOf(value)
        }

    private fun classOf(type: KSType): ClassSymbol =
        symbols.classOf(type.declaration as KSClassDeclaration)

    private fun annotationOf(annotation: KSAnnotation): AnnotationSymbol =
        KspAnnotation(annotation, symbols)
}

private fun entry(argument: KSValueArgument): Pair<String, Any?> =
    argument.name?.asString().orEmpty() to argument.value

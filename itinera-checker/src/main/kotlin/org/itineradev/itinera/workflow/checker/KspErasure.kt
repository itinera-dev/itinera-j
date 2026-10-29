package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

/**
 * A JVM method descriptor, such as `(JLjava/lang/String;)V`, with each class it names as
 * `Class.getName` names it: `long`, `java.lang.String` and `void` there.
 */
internal class JvmSignature(descriptor: String) {
    /** The classes of the parameters, in order, as the JVM holds them. */
    val parameters: List<String> =
        elements
            .findAll(descriptor.substringAfter('(').substringBefore(')'))
            .map(MatchResult::value)
            .map(::jvmNameOf)
            .toList()

    /** The class of what it returns. */
    val returns: String = jvmNameOf(descriptor.substringAfter(')'))
}

private val elements = Regex("""\[*(?:[ZBCSIJFD]|L[^;]+;)""")

/**
 * The class a descriptor element such as `J` or `Ljava/lang/String;` names, as `Class.getName`
 * does.
 */
private fun jvmNameOf(element: String): String =
    when {
        element.startsWith("[") -> element.replace('/', '.')
        element.startsWith("L") -> element.substring(1, element.length - 1).replace('/', '.')
        else -> primitiveNames.getValue(element)
    }

private val primitiveNames =
    mapOf(
        "Z" to "boolean",
        "B" to "byte",
        "C" to "char",
        "S" to "short",
        "I" to "int",
        "J" to "long",
        "F" to "float",
        "D" to "double",
        "V" to "void",
    )

/**
 * The simple name of the class [name] names as `Class.getName` does, as `Class.getSimpleName` gives
 * it.
 */
internal fun simpleNameOfJvm(name: String): String =
    when {
        name.startsWith("[") -> simpleNameOfJvm(componentOf(name)) + "[]"
        else -> name.substringAfterLast('.').substringAfterLast('$')
    }

private fun componentOf(array: String): String = jvmNameOf(array.substring(1).replace('.', '/'))

/**
 * Names the classes of KSP's types as the run-time reader reads a type argument: of a Kotlin
 * declaration, Kotlin's class of it, as its Java object type, and of a Java declaration, the class
 * Java reflection gives.
 */
internal class KspErasure(private val symbols: KspSymbols) {
    /**
     * The class of [argument] of a Kotlin declaration's type, by its binary name, or `null` for a
     * type parameter and a star projection.
     */
    fun kotlinArgumentOf(argument: KSTypeArgument): String? =
        if (argument.variance == Variance.STAR) null
        else argument.type?.resolve()?.let(::classNameOf)

    /**
     * The class of [argument] of a Java declaration's type, by its binary name: `Object[]` for an
     * array whose class the JVM does not know, and `null` for a type variable and a wildcard.
     */
    fun javaArgumentOf(argument: KSTypeArgument): String? {
        val type = argument.type?.resolve()
        return when {
            type == null || argument.variance != Variance.INVARIANT -> null
            isArray(type) && !isReifiable(type) -> OBJECTS
            else -> classNameOf(type)
        }
    }

    /** The class [type] is, as `Class.getName` names it, or `null` for a type parameter. */
    fun classNameOf(type: KSType): String? =
        when (val declaration = type.declaration) {
            is KSTypeAlias -> classNameOf(symbols.aliases.expanded(type))
            is KSClassDeclaration ->
                arrayNameOf(type, declaration) ?: symbols.classOf(declaration).name
            else -> null
        }

    /**
     * The class [type] erases to, as `jvmErasure` gives it: a type parameter's first bound that is
     * a class, or else its first bound.
     */
    private fun erasedNameOf(type: KSType): String? =
        when (val declaration = type.declaration) {
            is KSTypeParameter -> erasureBoundOf(declaration)?.let(::erasedNameOf)
            else -> classNameOf(type)
        }

    private fun arrayNameOf(type: KSType, declaration: KSClassDeclaration): String? {
        val name = declaration.qualifiedName?.asString()
        return primitiveArrays[name] ?: if (name == ARRAY) "[" + componentDescriptor(type) else null
    }

    private fun componentDescriptor(array: KSType): String {
        val name = componentOf(array)?.let(::erasedNameOf) ?: return OBJECT_DESCRIPTOR
        return if (name.startsWith("[")) name else "L$name;"
    }
}

private const val ARRAY = "kotlin.Array"

private const val OBJECTS = "[Ljava.lang.Object;"

private const val OBJECT_DESCRIPTOR = "Ljava.lang.Object;"

private val primitiveArrays =
    mapOf(
        "kotlin.BooleanArray" to "[Z",
        "kotlin.ByteArray" to "[B",
        "kotlin.CharArray" to "[C",
        "kotlin.ShortArray" to "[S",
        "kotlin.IntArray" to "[I",
        "kotlin.LongArray" to "[J",
        "kotlin.FloatArray" to "[F",
        "kotlin.DoubleArray" to "[D",
    )

private fun componentOf(array: KSType): KSType? = array.arguments.singleOrNull()?.type?.resolve()

private fun erasureBoundOf(parameter: KSTypeParameter): KSType? {
    val bounds = parameter.bounds.map(KSTypeReference::resolve).toList()
    return bounds.find(::isOfClass) ?: bounds.firstOrNull()
}

private fun isOfClass(type: KSType): Boolean =
    (type.declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS

private fun isArray(type: KSType): Boolean = type.declaration.qualifiedName?.asString() == ARRAY

/** Whether the JVM knows the class of [type], as it does not that of an array of type variables. */
private fun isReifiable(type: KSType): Boolean =
    when {
        type.declaration !is KSClassDeclaration -> false
        isArray(type) -> componentOf(type)?.let(::isReifiable) ?: false
        else -> type.arguments.isEmpty()
    }

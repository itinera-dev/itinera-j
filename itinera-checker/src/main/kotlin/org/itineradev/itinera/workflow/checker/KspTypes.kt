package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance

/**
 * Reads the types KSP knows as itinera-kotlin reads a Kotlin declaration's: with Kotlin's
 * nullability at every level, a class Kotlin maps to a Java one as that class, such as `Int` as
 * `Integer`, and a star or `in` projection as any value. A Java type, whose nullability Kotlin does
 * not know, admits `null`.
 */
internal class KspTypes(private val symbols: KspSymbols) {
    /** The type [reference] names. */
    fun of(reference: KSTypeReference): TypeUse = of(reference.resolve())

    /** [type], as the JVM and Kotlin's nullability know it. */
    fun of(type: KSType): TypeUse =
        when (val declaration = type.declaration) {
            is KSTypeParameter -> VariableUse(declaration.name.asString(), variableNullness(type))
            is KSTypeAlias -> of(symbols.aliases.expanded(type))
            is KSClassDeclaration -> classOrArray(type, declaration)
            else -> AnyUse
        }

    private fun classOrArray(type: KSType, declaration: KSClassDeclaration): TypeUse {
        val nullable = type.nullability != Nullability.NOT_NULL
        val array = arrayName(type, declaration)
        return if (array != null) ArrayUse(array, nullable)
        else ClassUse(symbols.classOf(declaration), type.arguments.map(::argument), nullable, false)
    }

    private fun argument(argument: KSTypeArgument): TypeUse {
        val type = argument.type
        return if (type == null || argument.variance in anyValue) AnyUse else of(type)
    }

    /**
     * The simple name of the array class [type] is on the JVM, such as `long[]` for `LongArray` and
     * `String[]` for `Array<String>`, or `null` when it is no array.
     */
    private fun arrayName(type: KSType, declaration: KSClassDeclaration): String? {
        val name = declaration.qualifiedName?.asString()
        return primitiveArrays[name] ?: if (name == ARRAY) "${componentName(type)}[]" else null
    }

    private fun componentName(type: KSType): String {
        val component = type.arguments.singleOrNull()?.type?.resolve() ?: return OBJECT
        return when (val declaration = component.declaration) {
            is KSClassDeclaration ->
                arrayName(component, declaration) ?: symbols.classOf(declaration).simpleName
            else -> OBJECT
        }
    }
}

private const val ARRAY = "kotlin.Array"

private const val OBJECT = "Object"

private val primitiveArrays =
    mapOf(
        "kotlin.BooleanArray" to "boolean[]",
        "kotlin.ByteArray" to "byte[]",
        "kotlin.CharArray" to "char[]",
        "kotlin.ShortArray" to "short[]",
        "kotlin.IntArray" to "int[]",
        "kotlin.LongArray" to "long[]",
        "kotlin.FloatArray" to "float[]",
        "kotlin.DoubleArray" to "double[]",
    )

private val anyValue = setOf(Variance.STAR, Variance.CONTRAVARIANT)

private fun variableNullness(type: KSType): Nullness =
    if (type.isMarkedNullable) Nullness.NULLABLE else Nullness.PARAMETRIC

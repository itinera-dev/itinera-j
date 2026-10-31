package org.itineradev.itinera.workflow.kotlin

import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction
import org.itineradev.itinera.engine.DeclaredParameter
import org.itineradev.itinera.engine.KotlinFunction
import org.itineradev.itinera.engine.rethrowError
import org.itineradev.itinera.value.kotlin.valueType

/**
 * What a method of a Kotlin class declares, as Kotlin wrote it: whether it is `suspend`, its return
 * type with whether that admits `null`, and its parameters with their Kotlin types. A method Kotlin
 * reflection cannot read is taken as Java reflection reads it.
 */
internal fun kotlinFunctionOf(method: Method): KotlinFunction =
    read { method.kotlinFunction }?.let { function -> declared(method, function) }
        ?: KotlinFunction.ofJava(method)

/** What a constructor of a Kotlin class declares, as [kotlinFunctionOf] reads a method. */
internal fun kotlinFunctionOf(constructor: Constructor<*>): KotlinFunction =
    read { constructor.kotlinFunction }?.let { function -> declared(constructor, function) }
        ?: KotlinFunction.ofJava(constructor)

private inline fun read(function: () -> KFunction<*>?): KFunction<*>? =
    runCatching(function).onFailure(::rethrowError).getOrNull()

private fun declared(executable: Executable, function: KFunction<*>): KotlinFunction {
    val values = function.parameters.filter(::isValue)
    val java =
        executable.parameters.dropLast(if (function.isSuspend) 1 else 0).takeLast(values.size)
    return KotlinFunction(
        isSuspend = function.isSuspend,
        returnType =
            (function.returnType.classifier as? KClass<*>)?.javaObjectType ?: Any::class.java,
        returnsNullable = function.returnType.isMarkedNullable,
        parameters = values.zip(java, ::parameter),
    )
}

private fun isValue(parameter: KParameter): Boolean = parameter.kind == KParameter.Kind.VALUE

private fun parameter(kotlin: KParameter, java: java.lang.reflect.Parameter): DeclaredParameter =
    DeclaredParameter(java, kotlin.name, valueType(kotlin.type))

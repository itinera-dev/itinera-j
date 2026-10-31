package org.itineradev.itinera.workflow.kotlin

import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction
import org.itineradev.itinera.engine.KotlinFunction
import org.itineradev.itinera.engine.rethrowError

/**
 * What a method of a Kotlin class declares, as Kotlin wrote it: whether it is `suspend`, and its
 * return type with whether that admits `null`. A method Kotlin reflection cannot read is taken as
 * Java reflection reads it.
 */
internal fun kotlinFunctionOf(method: Method): KotlinFunction =
    runCatching { method.kotlinFunction }.onFailure(::rethrowError).getOrNull()?.let(::declared)
        ?: KotlinFunction.ofJava(method)

private fun declared(function: KFunction<*>): KotlinFunction =
    KotlinFunction(
        isSuspend = function.isSuspend,
        returnType =
            (function.returnType.classifier as? KClass<*>)?.javaObjectType ?: Any::class.java,
        returnsNullable = function.returnType.isMarkedNullable,
    )

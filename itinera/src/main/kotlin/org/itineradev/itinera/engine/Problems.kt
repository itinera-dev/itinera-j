package org.itineradev.itinera.engine

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import org.itineradev.itinera.value.kotlinClasses
import org.itineradev.itinera.workflow.Violation

/** The mistakes found while an annotated class is read, with the checks several readers share. */
internal class Problems {
    private val recorded = mutableListOf<Violation>()

    val violations: List<Violation>
        get() = recorded.toList()

    val found: Boolean
        get() = recorded.isNotEmpty()

    operator fun plusAssign(violations: Iterable<Violation>) {
        recorded += violations
    }

    operator fun plusAssign(violation: Violation) {
        recorded += violation
    }

    fun misdeclared(declaration: String, problem: String) {
        recorded += Violation.Misdeclared(declaration, problem)
    }

    /** Whether [type] can be read: a Kotlin class needs `itinera-kotlin`. */
    fun readable(type: Class<*>): Boolean {
        val unread = isKotlin(type) && kotlinClasses == null
        if (unread)
            misdeclared(
                type.name,
                "is written in Kotlin, which itinera reads only with itinera-kotlin",
            )
        return !unread
    }

    /** Whether [method] returns [returns], and never `null`. */
    fun returns(method: Method, function: KotlinFunction, returns: Class<*>): Boolean {
        val type = function.returnType
        val assignable = returns.isAssignableFrom(type)
        if (!assignable)
            misdeclared(method.toString(), "returns ${type.simpleName}, not ${returns.simpleName}")
        if (function.returnsNullable)
            misdeclared(method.toString(), "returns ${returns.simpleName}?, which admits null")
        return assignable && !function.returnsNullable
    }

    /** Whether itinera may call every one of [members] of [declaring]. */
    fun opened(declaring: Class<*>, vararg members: AccessibleObject): Boolean {
        val opened = members.all(AccessibleObject::trySetAccessible)
        if (!opened)
            misdeclared(
                declaring.name,
                "is not open to itinera: open its package to org.itineradev.itinera",
            )
        return opened
    }
}

private fun isKotlin(type: Class<*>): Boolean = type.isAnnotationPresent(Metadata::class.java)

/**
 * What [method] declares: read by `itinera-kotlin` for a Kotlin class, and by Java reflection
 * otherwise.
 */
internal fun functionOf(method: Method): KotlinFunction =
    kotlinClasses?.takeIf { isKotlin(method.declaringClass) }?.functionOf(method)
        ?: KotlinFunction.ofJava(method)

/**
 * The parameters of [method] as its declaration writes them, without a `suspend` function's
 * continuation.
 */
internal fun declaredParameters(method: Method, function: KotlinFunction): List<Class<*>> =
    method.parameterTypes.dropLast(if (function.isSuspend) 1 else 0)

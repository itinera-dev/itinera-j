package org.itineradev.itinera.engine

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import org.itineradev.itinera.value.kotlinClasses
import org.itineradev.itinera.workflow.Violation

/** The mistakes found while an annotated class is read, with the checks several readers share. */
internal class Problems {
    private val recorded = mutableListOf<Violation>()

    val violations: List<Violation>
        get() = recorded.toList()

    val found: Boolean
        get() = recorded.isNotEmpty()

    /** How many mistakes are recorded so far, to tell whether a later read finds any. */
    val count: Int
        get() = recorded.size

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

internal fun isKotlin(type: Class<*>): Boolean = type.isAnnotationPresent(Metadata::class.java)

/**
 * What [executable] declares: read by `itinera-kotlin` for a Kotlin class, and by Java reflection
 * otherwise.
 */
internal fun functionOf(executable: Executable): KotlinFunction {
    val kotlin = kotlinClasses?.takeIf { isKotlin(executable.declaringClass) }
    return when {
        kotlin == null -> KotlinFunction.ofJava(executable)
        executable is Method -> kotlin.functionOf(executable)
        else -> kotlin.functionOf(executable as Constructor<*>)
    }
}

/**
 * Whether [executable] declares parameters of its own only, rather than capturing what its class's
 * enclosing code holds, as an inner or local class does.
 */
internal fun ownsParameters(executable: Executable, function: KotlinFunction): Boolean =
    function.parameters.size == executable.parameterCount - if (function.isSuspend) 1 else 0

/** The classes of [function]'s parameters, as its declaration writes them. */
internal fun parameterClasses(function: KotlinFunction): List<Class<*>> =
    function.parameters.map(DeclaredParameter::parameter).map(Parameter::getType)

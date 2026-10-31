package org.itineradev.itinera.engine

import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import org.itineradev.itinera.value.declaredType
import org.itineradev.itinera.value.isNullMarked

/**
 * A parameter of a method or a constructor as its declaration writes it: its name, when it was
 * kept, and its type, with whether `null` is of it.
 *
 * Part of itinera's engine, not of its API: `itinera-kotlin` reads those of Kotlin classes.
 */
public class DeclaredParameter(
    /** The parameter as Java reflection reads it, with its annotations. */
    public val parameter: Parameter,
    /** Its name, or `null` when the class was compiled without keeping it. */
    public val name: String?,
    /** Its type. */
    public val type: ValueType,
) {
    /** Reads parameters. */
    public companion object {
        /**
         * The parameters [executable] declares, as Java reflection reads them, by JSpecify's rules,
         * without those the compiler adds, such as a captured value or a `suspend` function's
         * continuation, which [isSuspend] says it has.
         */
        internal fun ofJava(executable: Executable, isSuspend: Boolean): List<DeclaredParameter> {
            val nullMarked = isNullMarked(executable)
            return executable.parameters
                .dropLast(if (isSuspend) 1 else 0)
                .filterNot(::isAdded)
                .map { parameter ->
                    javaParameter(parameter, nullMarked)
                }
        }
    }
}

private fun isAdded(parameter: Parameter): Boolean = parameter.isSynthetic || parameter.isImplicit

private fun javaParameter(parameter: Parameter, nullMarked: Boolean): DeclaredParameter =
    DeclaredParameter(
        parameter,
        parameter.name.takeIf { parameter.isNamePresent },
        declaredType(parameter.annotatedType, nullMarked),
    )

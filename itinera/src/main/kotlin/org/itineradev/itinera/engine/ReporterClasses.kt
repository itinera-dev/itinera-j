package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import org.itineradev.itinera.report.ReporterFactoryOf
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Violation

/**
 * Reads the reporter classes a workflow class lists, whose reporters of [mode] implement [type],
 * recording each mistake in [problems].
 */
internal class ReporterClasses<M : Mode>(
    private val mode: M,
    private val type: Class<out ReporterOf<M>>,
    private val problems: Problems,
) {
    /**
     * The factory with which each instance builds a reporter of [listed], or `null` when the class
     * has a mistake.
     */
    fun read(listed: Class<*>): ReporterFactoryOf<M>? =
        if (type.isAssignableFrom(listed))
            problems.builtWithNothing(listed, "reporter")?.let(::reporter)
        else {
            problems += Violation.ModeNotAccepted("reporter ${listed.name}", mode)
            null
        }

    private fun reporter(constructor: Constructor<*>): ReporterFactoryOf<M> = ReporterFactoryOf {
        type.cast(constructed(constructor))
    }
}

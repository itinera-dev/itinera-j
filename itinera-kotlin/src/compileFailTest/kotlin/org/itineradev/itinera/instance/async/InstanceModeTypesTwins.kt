package org.itineradev.itinera.instance.async

import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterFactory
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.async.AsyncReporterFactory

/** What [InstanceModeTypesTest] refuses, written so that it compiles. */
internal object InstanceModeTypesTwins {
    fun add(instance: InstanceBuilder, reporting: Reporter) = instance.reporter(reporting)

    fun add(instance: InstanceBuilder, reporting: ReporterFactory) = instance.reporterFor(reporting)

    fun add(instance: AsyncInstanceBuilder, reporting: AsyncReporter) = instance.reporter(reporting)

    fun add(instance: AsyncInstanceBuilder, reporting: AsyncReporterFactory) =
        instance.reporterFor(reporting)
}

package org.itineradev.itinera.report

/** What [ReportTypesTest] refuses, written so that it compiles. */
internal object ReportTypesTwins {
    val reporters = ReporterFactory { Reporter {} }
    val dispatchers = DispatcherFactory { DefaultDispatcher() }
}

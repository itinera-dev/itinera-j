package org.itineradev.itinera.report.async

/** What [AsyncReportTypesTest] refuses, written so that it compiles. */
internal object AsyncReportTypesTwins {
    val reporters = AsyncReporterFactory { AsyncReporter {} }
    val dispatchers = AsyncDispatcherFactory { AsyncDefaultDispatcher() }
}

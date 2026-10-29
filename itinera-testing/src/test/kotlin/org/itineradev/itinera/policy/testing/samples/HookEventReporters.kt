@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.testing.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.testing.AsyncHookEventReporter
import org.itineradev.itinera.policy.testing.HookEventReporter
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.Level

fun hookEventReporter(): List<Emitted> {
    val recorder = HookEventReporter()
    recorder.reporter.info("audited", mapOf("amount" to 40L))
    recorder.reporter.setGlobalMetadata("audited", true)
    check(recorder.globalMetadata == mapOf("audited" to true))
    check(recorder.emitted.single() == Emitted(Level.INFO, "audited", mapOf("amount" to 40L)))
    return recorder.emitted
}

fun failingHookEventReporter(): Boolean {
    val recorder = HookEventReporter.failing()
    val interrupted = runCatching { recorder.reporter.warning("the amount is unusual") }.isFailure
    runCatching { recorder.reporter.setGlobalMetadata("audited", true) }
    check(recorder.emitted.isEmpty())
    check(recorder.globalMetadata.isEmpty())
    return interrupted
}

suspend fun asyncHookEventReporter(): List<Emitted> {
    val recorder = AsyncHookEventReporter()
    recorder.reporter.warning("the amount is unusual", mapOf("amount" to 4000L))
    check(recorder.emitted.single().level == Level.WARNING)
    return recorder.emitted
}

suspend fun failingAsyncHookEventReporter(): Boolean {
    val recorder = AsyncHookEventReporter.failing()
    val interrupted = runCatching { recorder.reporter.warning("the amount is unusual") }.isFailure
    check(recorder.emitted.isEmpty())
    return interrupted
}

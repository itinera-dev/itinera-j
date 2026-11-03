@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.testing.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.step.testing.AsyncStepEventReporter
import org.itineradev.itinera.step.testing.StepEventReporter

fun stepEventReporter(): List<Emitted> {
    val recorder = StepEventReporter()
    recorder.reporter.info("charging", mapOf("amount" to 40L))
    recorder.reporter.warning("the gateway is slow")
    recorder.reporter.setGlobalMetadata("tenant", "corner")
    check(recorder.globalMetadata == mapOf("tenant" to "corner"))
    check(recorder.emitted.last() == Emitted(Level.WARNING, "the gateway is slow", null))
    return recorder.emitted
}

fun failingStepEventReporter(): Boolean {
    val recorder = StepEventReporter.failing()
    val interrupted = runCatching { recorder.reporter.info("charging") }.isFailure
    runCatching { recorder.reporter.setGlobalMetadata("tenant", "corner") }
    check(recorder.emitted.isEmpty())
    check(recorder.globalMetadata.isEmpty())
    return interrupted
}

suspend fun asyncStepEventReporter(): List<Emitted> {
    val recorder = AsyncStepEventReporter()
    recorder.reporter.error("the card was declined", mapOf("code" to "51"))
    check(recorder.emitted.single().level == Level.ERROR)
    return recorder.emitted
}

suspend fun failingAsyncStepEventReporter(): Boolean {
    val recorder = AsyncStepEventReporter.failing()
    val interrupted = runCatching { recorder.reporter.info("charging") }.isFailure
    check(recorder.emitted.isEmpty())
    return interrupted
}

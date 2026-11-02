@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.reporting
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmittingStepReporterTest {
    @Test
    fun `calls from several threads go through one at a time, so their events keep distinct increasing sequences`() {
        val log = Log()
        ran(journeyOf(reporting("charge", ::emittedFromThreads)).reporter(log).create())
        val sequences = log.events.filterIsInstance<StepInfo>().map(Event::sequence)
        assertEquals(THREADS * EMITS, sequences.size)
        assertEquals(sequences.distinct().sorted(), sequences)
    }

    @Test
    fun `closing waits for the call going through`() {
        val emits = Gated()
        val reporter = EmittingStepReporter(emits)
        val emitting = Thread { reporter.info("noted") }.also(Thread::start)
        emits.entered.await()
        val closing = Thread { driven { reporter.close() } }.also(Thread::start)
        closing.join(WAIT_MS)
        assertTrue(closing.isAlive)
        emits.released.countDown()
        emitting.join()
        closing.join()
        assertEquals(listOf("emit", "close"), emits.calls)
    }
}

private const val THREADS = 8
private const val EMITS = 50
private const val WAIT_MS = 100L

private fun emittedFromThreads(reporter: StepReporter): Outcome {
    val threads = List(THREADS) { emitting(reporter) }
    threads.forEach(Thread::start)
    threads.forEach(Thread::join)
    return Outcome.success()
}

private fun emitting(reporter: StepReporter): Thread = Thread {
    repeat(EMITS) { reporter.info("noted") }
}

/** Emits that hold a call until released, recording what went through. */
private class Gated : Emits {
    val entered = CountDownLatch(1)
    val released = CountDownLatch(1)
    val calls = CopyOnWriteArrayList<String>()

    override suspend fun emit(severity: Severity, message: String, data: Any?) {
        entered.countDown()
        released.await()
        calls.add("emit")
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) = Unit

    override fun close() {
        calls.add("close")
    }
}

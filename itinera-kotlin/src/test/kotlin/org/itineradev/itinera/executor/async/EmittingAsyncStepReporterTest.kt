package org.itineradev.itinera.executor.async

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.itineradev.itinera.engine.Emits
import org.itineradev.itinera.engine.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EmittingAsyncStepReporterTest {
    @Test
    fun `calls from several coroutines go through one at a time`() {
        val emits = Overlapping()
        val reporter = EmittingAsyncStepReporter(emits)
        runBlocking(Dispatchers.Default) { List(COROUTINES) { emitting(reporter) }.joinAll() }
        assertEquals(COROUTINES * EMITS, emits.calls.get())
        assertEquals(1, emits.most.get())
    }

    @Test
    fun `closing waits for the call going through`() {
        val emits = Gated()
        val reporter = EmittingAsyncStepReporter(emits)
        runBlocking {
            val emitting = launch { reporter.info("noted") }
            emits.entered.await()
            val closing = launch { reporter.close() }
            yield()
            assertFalse(closing.isCompleted)
            emits.released.complete(Unit)
            joinAll(emitting, closing)
        }
        assertEquals(listOf("emit", "close"), emits.calls)
    }
}

private const val COROUTINES = 8
private const val EMITS = 50

private fun CoroutineScope.emitting(reporter: EmittingAsyncStepReporter): Job = launch {
    repeat(EMITS) { reporter.info("noted") }
}

/** Emits that hold a call until released, recording what went through. */
private class Gated : Emits {
    val entered = CompletableDeferred<Unit>()
    val released = CompletableDeferred<Unit>()
    val calls = mutableListOf<String>()

    override suspend fun emit(severity: Severity, message: String, data: Any?) {
        entered.complete(Unit)
        released.await()
        calls.add("emit")
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) = Unit

    override fun close() {
        calls.add("close")
    }
}

/** Emits that count the calls going through at once, and the most there ever were. */
private class Overlapping : Emits {
    val calls = AtomicInteger()
    val most = AtomicInteger()
    private val inside = AtomicInteger()

    override suspend fun emit(severity: Severity, message: String, data: Any?) {
        most.accumulateAndGet(inside.incrementAndGet(), Math::max)
        yield()
        inside.decrementAndGet()
        calls.incrementAndGet()
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) = Unit

    override fun close() = Unit
}

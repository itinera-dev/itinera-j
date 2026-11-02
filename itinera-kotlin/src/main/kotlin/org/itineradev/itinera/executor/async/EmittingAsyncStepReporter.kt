package org.itineradev.itinera.executor.async

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.itineradev.itinera.engine.Emits
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.ReporterHandle
import org.itineradev.itinera.engine.Severity
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * An asynchronous step's reporter, which emits through [emits], letting one call through at a time,
 * whichever coroutine makes it.
 */
internal class EmittingAsyncStepReporter(private val emits: Emits) :
    AsyncStepReporter, ReporterHandle {
    private val mutex = Mutex()

    override val handle: Handle
        get() = Handle.ASYNC_STEP_REPORTER

    override val reporter: Any
        get() = this

    @JvmSynthetic override suspend fun info(message: String): Unit = info(message, null)

    @JvmSynthetic
    override suspend fun info(message: String, data: Any?): Unit = mutex.withLock {
        emits.emit(Severity.INFO, message, data)
    }

    @JvmSynthetic override suspend fun warning(message: String): Unit = warning(message, null)

    @JvmSynthetic
    override suspend fun warning(message: String, data: Any?): Unit = mutex.withLock {
        emits.emit(Severity.WARNING, message, data)
    }

    @JvmSynthetic override suspend fun error(message: String): Unit = error(message, null)

    @JvmSynthetic
    override suspend fun error(message: String, data: Any?): Unit = mutex.withLock {
        emits.emit(Severity.ERROR, message, data)
    }

    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?): Unit = mutex.withLock {
        emits.setGlobalMetadata(key, value)
    }

    /** Closes once no call is going through, even when the journey's coroutine was cancelled. */
    @JvmSynthetic
    override suspend fun close(): Unit =
        withContext(NonCancellable) { mutex.withLock { emits.close() } }
}

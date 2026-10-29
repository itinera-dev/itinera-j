package org.itineradev.itinera.report.testing

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.itineradev.itinera.engine.Emits
import org.itineradev.itinera.engine.Severity
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * The asynchronous reporter a recorder lends a step or a hook outside any journey, which emits and
 * sets global metadata through [emits], letting one call through at a time, whichever coroutine
 * makes it, as the reporter lent in a journey does.
 */
internal class LoneAsyncReporter(private val emits: Emits) : AsyncStepReporter, AsyncHookReporter {
    private val mutex = Mutex()

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
}

package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.step.StepReporter

/**
 * A synchronous step's, hook's or service provider's reporter, given to custom code that requests
 * [handle], which emits through [emits] and blocks until it is done, letting one call through at a
 * time, whichever thread makes it.
 */
internal class EmittingReporter(private val emits: Emits, override val handle: Handle) :
    StepReporter, HookReporter, ProviderReporter, ReporterHandle {
    private val lock = Any()

    override val reporter: Any
        get() = this

    override fun info(message: String) {
        info(message, null)
    }

    override fun info(message: String, data: Any?) {
        serialised { emits.emit(Severity.INFO, message, data) }
    }

    override fun warning(message: String) {
        warning(message, null)
    }

    override fun warning(message: String, data: Any?) {
        serialised { emits.emit(Severity.WARNING, message, data) }
    }

    override fun error(message: String) {
        error(message, null)
    }

    override fun error(message: String, data: Any?) {
        serialised { emits.emit(Severity.ERROR, message, data) }
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        serialised { emits.setGlobalMetadata(key, value) }
    }

    @JvmSynthetic override suspend fun close(): Unit = synchronized(lock) { emits.close() }

    /** Runs [call] to its end, which never suspends in this mode, while no other call runs. */
    private fun serialised(call: suspend () -> Unit) {
        synchronized(lock) { driven(call) }
    }
}

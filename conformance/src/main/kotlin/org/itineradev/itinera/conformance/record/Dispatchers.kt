package org.itineradev.itinera.conformance.record

import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.DefaultDispatcher
import org.itineradev.itinera.report.Dispatcher
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncDefaultDispatcher
import org.itineradev.itinera.report.async.AsyncDispatcher
import org.itineradev.itinera.report.async.AsyncDispatcherFactory
import org.itineradev.itinera.report.async.AsyncReporter

/**
 * The runner's dispatcher factory for synchronous workflows: every dispatcher it creates holds
 * [held], the same reporter each time, and delivers as a [DefaultDispatcher] does to it and to the
 * reporters the executor adds, except where [script] says otherwise.
 */
internal fun dispatchers(held: Reporter, script: DispatcherScript): DispatcherFactory =
    DispatcherFactory {
        ScriptedDispatcher(DefaultDispatcher().apply { add(held) }, script)
    }

/** The asynchronous twin of [dispatchers]. */
internal fun asyncDispatchers(
    held: AsyncReporter,
    script: DispatcherScript,
): AsyncDispatcherFactory = AsyncDispatcherFactory {
    AsyncScriptedDispatcher(AsyncDefaultDispatcher().apply { add(held) }, script)
}

/** A dispatcher factory that fails whenever the executor asks it for a dispatcher. */
internal val failingDispatchers = DispatcherFactory { throw DispatcherFailure("the factory fails") }

/** The asynchronous twin of [failingDispatchers]. */
internal val failingAsyncDispatchers = AsyncDispatcherFactory {
    throw DispatcherFailure("the factory fails")
}

private class ScriptedDispatcher(
    private val delivering: DefaultDispatcher,
    private val script: DispatcherScript,
) : Dispatcher {
    override fun add(reporter: Reporter) {
        if (script.adds()) delivering.add(reporter)
    }

    override fun dispatch(event: Event) {
        script.checkDispatching(event)
        delivering.dispatch(event)
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        delivering.setGlobalMetadata(key, value)
    }
}

private class AsyncScriptedDispatcher(
    private val delivering: AsyncDefaultDispatcher,
    private val script: DispatcherScript,
) : AsyncDispatcher {
    override suspend fun add(reporter: AsyncReporter) {
        if (script.adds()) delivering.add(reporter)
    }

    override suspend fun dispatch(event: Event) {
        script.checkDispatching(event)
        delivering.dispatch(event)
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        delivering.setGlobalMetadata(key, value)
    }
}

/** Whether a reporter the executor adds is added, throwing where the script says so. */
private fun DispatcherScript.adds(): Boolean =
    when (this) {
        DispatcherScript.Delivers -> true
        DispatcherScript.IgnoresAddedReporters -> false
        DispatcherScript.ThrowsWhenAdding ->
            throw DispatcherFailure("the dispatcher throws when a reporter is added")
        is DispatcherScript.ThrowsWhenDispatching -> true
    }

private fun DispatcherScript.checkDispatching(event: Event) {
    when (this) {
        DispatcherScript.Delivers,
        DispatcherScript.IgnoresAddedReporters,
        DispatcherScript.ThrowsWhenAdding -> Unit
        is DispatcherScript.ThrowsWhenDispatching ->
            if (event.kind == kind) {
                throw DispatcherFailure("the dispatcher throws when dispatching $kind")
            }
    }
}

/** What a scripted dispatcher or factory throws. */
internal class DispatcherFailure(message: String) : RuntimeException(message)

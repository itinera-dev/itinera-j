package org.itineradev.itinera.conformance.record

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.testing.AsyncRecordingReporter
import org.itineradev.itinera.report.testing.RecordingReporter

/**
 * One mode's scripted dispatcher, with the reporter it holds and one the executor adds, both
 * recording, driven the same way in both modes so one test checks both.
 */
internal interface Scripted {
    /** The events the held reporter received. */
    val held: List<Event>

    /** The global metadata the held reporter received. */
    val heldMetadata: Map<String, Any?>

    /** The events the added reporter received. */
    val added: List<Event>

    fun add()

    fun dispatch(event: Event)

    fun setGlobalMetadata(key: String, value: Any?)
}

/** A synchronous dispatcher from [dispatchers], created as the executor creates it. */
internal class Synchronous(script: DispatcherScript) : Scripted {
    private val holding = RecordingReporter()
    private val adding = RecordingReporter()
    private val dispatcher = dispatchers(holding, script).create()

    override val held
        get() = holding.events

    override val heldMetadata
        get() = holding.globalMetadata

    override val added
        get() = adding.events

    override fun add() = dispatcher.add(adding)

    override fun dispatch(event: Event) = dispatcher.dispatch(event)

    override fun setGlobalMetadata(key: String, value: Any?) =
        dispatcher.setGlobalMetadata(key, value)
}

/** An asynchronous dispatcher from [asyncDispatchers], created as the executor creates it. */
internal class Asynchronous(script: DispatcherScript) : Scripted {
    private val holding = AsyncRecordingReporter()
    private val adding = AsyncRecordingReporter()
    private val dispatcher = complete { asyncDispatchers(holding, script).create() }

    override val held
        get() = holding.events

    override val heldMetadata
        get() = holding.globalMetadata

    override val added
        get() = adding.events

    override fun add() = complete { dispatcher.add(adding) }

    override fun dispatch(event: Event) = complete { dispatcher.dispatch(event) }

    override fun setGlobalMetadata(key: String, value: Any?) = complete {
        dispatcher.setGlobalMetadata(key, value)
    }
}

/**
 * Runs `suspend` code that never suspends to its end on the caller's thread, so the tests need no
 * coroutine library.
 */
internal fun <T> complete(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result -> outcome = result })
    return checkNotNull(outcome) { "The code suspended." }.getOrThrow()
}

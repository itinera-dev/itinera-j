package org.itineradev.itinera.conformance.model

import org.itineradev.itinera.event.EventKind

/** What the executor is given to dispatch a journey's events. */
internal sealed interface Dispatching {
    /** No sentence says: the runner gives its recording dispatcher factory. */
    data object Unstated : Dispatching

    /** The executor uses its default dispatcher factory. */
    data object Default : Dispatching

    /** The factory fails when the executor asks it for a dispatcher. */
    data object FactoryFails : Dispatching

    /** Each dispatcher holds this scripted reporter, and behaves as [script] says. */
    data class Holding(val reporter: String, val script: DispatcherScript) : Dispatching
}

/** How the dispatchers of the runner's factory behave. */
internal sealed interface DispatcherScript {
    /** Delivers to the reporter it holds and to those the executor adds, as the default does. */
    data object Delivers : DispatcherScript

    /** Delivers only to the reporter it holds, ignoring those the executor adds. */
    data object IgnoresAddedReporters : DispatcherScript

    /** Throws when the executor adds a reporter, and adds none. */
    data object ThrowsWhenAdding : DispatcherScript

    /** Throws instead of delivering an event of [kind], and delivers every other. */
    data class ThrowsWhenDispatching(val kind: EventKind) : DispatcherScript
}

/** How a scripted reporter fails; a reporter with none never fails. */
internal sealed interface ReporterFailure {
    /** Throws on the first event it receives. */
    data object OnFirstEvent : ReporterFailure

    /** Throws, with [message] when there is one, on every event of [kind], and on no other. */
    data class OnEvent(val kind: EventKind, val message: String? = null) : ReporterFailure

    /** Fails on every entry of global metadata under [key], and on nothing else. */
    data class OnGlobalMetadata(val key: String) : ReporterFailure
}

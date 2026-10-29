package org.itineradev.itinera.workflow.checker

/**
 * What a request asks for, as far as the rules on how a workflow is put together need it: a
 * service, a role, or what only a provider of attempt scope receives.
 */
internal sealed interface Asked

/** The service a step requests under [key], of the class named [type]. */
internal data class ServiceAsked(val key: String, val type: String) : Asked

/** The role [type] that a hook requests. */
internal data class RoleAsked(val type: ClassSymbol) : Asked

/** The step's name or the attempt, which only a provider of attempt scope receives. */
internal data object OfTheAttempt : Asked

/** Anything else, which no such rule reads. */
internal data object Other : Asked

/** Every one of [read], or `null` when one of them could not be read. */
internal fun <T : Any> all(read: List<T?>): List<T>? =
    if (null in read) null else read.filterNotNull()

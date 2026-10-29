package org.itineradev.itinera.engine

import org.itineradev.itinera.step.Reason

/**
 * How one attempt of a step ended, with what it contributed, which its hooks see whatever the
 * outcome.
 */
internal sealed interface Ended {
    /** The attempt reported Success. */
    data class Succeeded(val contributions: Map<String, Captured>) : Ended

    /** The attempt reported Failure, for [reason]. */
    data class Failed(val reason: Reason, val contributions: Map<String, Captured>) : Ended

    /** The attempt reported Skipped, for [reason] when it gave one. */
    data class Skipped(val reason: Reason?, val contributions: Map<String, Captured>) : Ended

    /**
     * [error] escaped the running step: an abnormal termination, which carries no contributions.
     */
    data class Terminated(val error: Throwable) : Ended
}

/**
 * How the one attempt of a rescue step ended, with what it contributed when it succeeded. It never
 * skips, since a rescue step's run cannot report a skip.
 */
internal sealed interface RescueEnded {
    /** The rescue step reported Success. */
    data class Succeeded(val contributions: Map<String, Captured>) : RescueEnded

    /** The rescue step reported Failure, for [reason]. */
    data class Failed(val reason: Reason) : RescueEnded

    /** [error] escaped the running rescue step: an abnormal termination. */
    data class Terminated(val error: Throwable) : RescueEnded
}

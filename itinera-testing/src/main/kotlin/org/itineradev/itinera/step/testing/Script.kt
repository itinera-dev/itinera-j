package org.itineradev.itinera.step.testing

import java.util.concurrent.atomic.AtomicInteger

/**
 * What a step mock or a rescue step mock plays, shared by both modes: how each attempt ends, as an
 * ending of type [E], or the message every build fails with, and how often a step was built from
 * it.
 */
internal sealed class Script<out E> {
    private val built = AtomicInteger()

    /** How many times a step was built from it. */
    val builds: Int
        get() = built.get()

    /** The ending of the next attempt, counting the build, or the failure of its build. */
    abstract fun build(): E

    /** Counts a build, giving how many came before it. */
    protected fun counted(): Int = built.getAndIncrement()

    /**
     * The first attempt ends with [first], each later one with the next of [then], and every
     * attempt after them with the last.
     */
    class Playing<E>(first: E, then: List<E>) : Script<E>() {
        private val endings = listOf(first) + then

        override fun build(): E = endings.getOrElse(counted()) { endings.last() }
    }

    /** Every build fails with an exception carrying [errorMessage]. */
    class FailingToBuild(private val errorMessage: String) : Script<Nothing>() {
        override fun build(): Nothing = throw ScriptedError(errorMessage)
    }
}

package org.itineradev.itinera.step.testing

import java.util.concurrent.atomic.AtomicInteger

/**
 * What a step mock plays, shared by both modes: how each attempt ends, or the message every build
 * fails with, and how often a step was built from it.
 */
internal sealed class Script {
    private val built = AtomicInteger()

    /** How many times a step was built from it. */
    val builds: Int
        get() = built.get()

    /** The ending of the next attempt, counting the build, or the failure of its build. */
    fun build(): Ending =
        when (this) {
            is Playing -> endingOf(built.getAndIncrement())
            is FailingToBuild -> throw ScriptedError(errorMessage)
        }

    /**
     * The first attempt ends with [first], each later one with the next of [then], and every
     * attempt after them with the last.
     */
    class Playing(first: Ending, then: List<Ending>) : Script() {
        private val endings = listOf(first) + then

        /** The ending of the attempt that follows [built] builds. */
        fun endingOf(built: Int): Ending = endings.getOrElse(built) { endings.last() }
    }

    /** Every build fails with an exception carrying [errorMessage]. */
    class FailingToBuild(val errorMessage: String) : Script()
}

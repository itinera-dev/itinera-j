package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun

/**
 * The twin of [StepMock] for asynchronous workflows: the factory of an asynchronous step that ends
 * each attempt as its script says. It needs `itinera-kotlin`, which an asynchronous workflow has
 * anyway.
 *
 * @sample org.itineradev.itinera.step.testing.samples.asyncMocks
 */
@ExperimentalItineraApi
public class AsyncStepMock private constructor(private val script: Script) : AsyncStepFactory {
    /**
     * How many times a step was built from this mock, a build that failed not counted.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncMocks
     */
    public val builds: Int
        get() = script.builds

    /**
     * Builds a new asynchronous step, which ends its attempt with the next ending of the script.
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.step.testing.samples.asyncMocks
     */
    override fun build(): AsyncStepRun = runEnding(script.build())

    /** Makes asynchronous step mocks. */
    public companion object {
        /**
         * A mock whose first attempt ends with [first], each later one with the ending of [then] in
         * its turn, and every attempt after them with the last ending given.
         *
         * @sample org.itineradev.itinera.step.testing.samples.asyncMocks
         */
        public fun playing(first: Ending, vararg then: Ending): AsyncStepMock =
            AsyncStepMock(Script.Playing(first, then.toList()))

        /**
         * A mock whose build always fails, throwing an exception carrying [errorMessage], as a step
         * whose constructor fails does.
         *
         * @sample org.itineradev.itinera.step.testing.samples.asyncFailingToBuild
         */
        public fun failingToBuild(errorMessage: String): AsyncStepMock =
            AsyncStepMock(Script.FailingToBuild(errorMessage))
    }
}

/** An asynchronous step that ends its one attempt with [ending]. */
private fun runEnding(ending: Ending): AsyncStepRun = AsyncStepRun { ending.outcome() }

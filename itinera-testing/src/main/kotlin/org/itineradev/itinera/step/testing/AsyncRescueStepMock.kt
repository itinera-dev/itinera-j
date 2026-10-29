package org.itineradev.itinera.step.testing

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.async.AsyncRescueFactory
import org.itineradev.itinera.step.async.AsyncRescueRun
import org.itineradev.itinera.workflow.Async

/**
 * The twin of [RescueStepMock] for asynchronous workflows: the factory of an asynchronous rescue
 * step that ends each attempt as its script says. It needs `itinera-kotlin`, which an asynchronous
 * workflow has anyway.
 *
 * @sample org.itineradev.itinera.step.testing.samples.asyncRescueMocks
 */
public class AsyncRescueStepMock private constructor(private val script: Script<RescueEnding>) :
    AsyncRescueFactory {
    /**
     * How many times a rescue step was built from this mock, a build that failed not counted.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncRescueMocks
     */
    public val builds: Int
        get() = script.builds

    /**
     * Builds a new asynchronous rescue step, which ends its attempt with the next ending of the
     * script and reads nothing it [got].
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.step.testing.samples.asyncRescueMocks
     */
    override fun build(got: Got<RescueKind<Async>>): AsyncRescueRun = rescueEnding(script.build())

    /** Makes asynchronous rescue step mocks. */
    public companion object {
        /**
         * A mock whose first attempt ends with [first], each later one with the ending of [then] in
         * its turn, and every attempt after them with the last ending given.
         *
         * @sample org.itineradev.itinera.step.testing.samples.asyncRescueMocks
         */
        public fun playing(first: RescueEnding, vararg then: RescueEnding): AsyncRescueStepMock =
            AsyncRescueStepMock(Script.Playing(first, then.toList()))

        /**
         * A mock whose build always fails, throwing an exception carrying [errorMessage], as a
         * rescue step whose constructor fails does.
         *
         * @sample org.itineradev.itinera.step.testing.samples.asyncRescueFailingToBuild
         */
        public fun failingToBuild(errorMessage: String): AsyncRescueStepMock =
            AsyncRescueStepMock(Script.FailingToBuild(errorMessage))
    }
}

/** An asynchronous rescue step that ends its one attempt with [ending]. */
private fun rescueEnding(ending: RescueEnding): AsyncRescueRun = AsyncRescueRun { ending.outcome() }

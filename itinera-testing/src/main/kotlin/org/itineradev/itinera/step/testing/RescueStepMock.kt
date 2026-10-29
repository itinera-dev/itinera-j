package org.itineradev.itinera.step.testing

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.workflow.Sync

/**
 * A synchronous rescue step for tests: the factory of a rescue step that ends each attempt as its
 * script says, through a [RescueEnding], so that it ends exactly as a real rescue step would. Each
 * build makes a new rescue step and counts as the next attempt of the script, across every journey
 * that builds from the mock, as a [StepMock] does. [AsyncRescueStepMock] is its twin for
 * asynchronous workflows.
 *
 * @sample org.itineradev.itinera.step.testing.samples.rescueMocks
 */
public class RescueStepMock private constructor(private val script: Script<RescueEnding>) :
    SyncRescueFactory {
    /**
     * How many times a rescue step was built from this mock, a build that failed not counted.
     *
     * @sample org.itineradev.itinera.step.testing.samples.rescueMocks
     */
    public val builds: Int
        get() = script.builds

    /**
     * Builds a new rescue step, which ends its attempt with the next ending of the script and reads
     * nothing it [got].
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.step.testing.samples.rescueMocks
     */
    override fun build(got: Got<RescueKind<Sync>>): SyncRescueRun =
        SyncRescueRun(script.build()::outcome)

    /** Makes rescue step mocks. */
    public companion object {
        /**
         * A mock whose first attempt ends with [first], each later one with the ending of [then] in
         * its turn, and every attempt after them with the last ending given.
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueMocks
         */
        @JvmStatic
        public fun playing(first: RescueEnding, vararg then: RescueEnding): RescueStepMock =
            RescueStepMock(Script.Playing(first, then.toList()))

        /**
         * A mock whose build always fails, throwing an exception carrying [errorMessage], as a
         * rescue step whose constructor fails does.
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueFailingToBuild
         */
        @JvmStatic
        public fun failingToBuild(errorMessage: String): RescueStepMock =
            RescueStepMock(Script.FailingToBuild(errorMessage))
    }
}

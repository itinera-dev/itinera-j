package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.Sync

/**
 * A synchronous step for tests: the factory of a step that ends each attempt as its script says,
 * through an [Ending], so that it ends exactly as a real step would. Each build makes a new step
 * and counts as the next attempt of the script, across every journey that builds from the mock, so
 * a test that scripts a journey from its first attempt gives it a new mock. [AsyncStepMock] is its
 * twin for asynchronous workflows.
 *
 * @sample org.itineradev.itinera.step.testing.samples.mocks
 */
@ExperimentalItineraApi
public class StepMock private constructor(private val script: Script) : SyncStepFactory {
    /**
     * How many times a step was built from this mock, a build that failed not counted.
     *
     * @sample org.itineradev.itinera.step.testing.samples.mocks
     */
    public val builds: Int
        get() = script.builds

    /**
     * Builds a new step, which ends its attempt with the next ending of the script and reads
     * nothing it [got].
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.step.testing.samples.mocks
     */
    override fun build(got: Got<StepKind<Sync>>): SyncStepRun = SyncStepRun(script.build()::outcome)

    /** Makes step mocks. */
    public companion object {
        /**
         * A mock whose first attempt ends with [first], each later one with the ending of [then] in
         * its turn, and every attempt after them with the last ending given.
         *
         * @sample org.itineradev.itinera.step.testing.samples.mocks
         */
        @JvmStatic
        public fun playing(first: Ending, vararg then: Ending): StepMock =
            StepMock(Script.Playing(first, then.toList()))

        /**
         * A mock whose build always fails, throwing an exception carrying [errorMessage], as a step
         * whose constructor fails does.
         *
         * @sample org.itineradev.itinera.step.testing.samples.failingToBuild
         */
        @JvmStatic
        public fun failingToBuild(errorMessage: String): StepMock =
            StepMock(Script.FailingToBuild(errorMessage))
    }
}

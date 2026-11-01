package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorsTest {
    @AfterEach
    fun clearTheFlag() {
        Thread.interrupted()
    }

    @Test
    fun `an interrupted step fails as a throw does, and the flag is set again once run returns`() {
        val result =
            LocalExecutor()
                .run(
                    journeyOf(
                            playing("charge", InterruptedException(), null).retryBudget(1),
                            pausing(),
                        )
                        .create()
                )
        assertTrue(Thread.interrupted())
        assertTrue(result.isSucceeded)
    }

    @Test
    fun `a journey whose custom code was never interrupted leaves the flag clear`() {
        LocalExecutor().run(journeyOf(pausing()).create())
        assertFalse(Thread.interrupted())
    }

    @Test
    fun `a journey run inside a step sets the flag when it returns, which outlives the outer one`() {
        val nested =
            building("order") {
                SyncStepRun {
                    LocalExecutor()
                        .run(journeyOf(playing("charge", InterruptedException())).create())
                    Outcome.success()
                }
            }
        LocalExecutor().run(journeyOf(nested).create())
        assertTrue(Thread.interrupted())
    }

    /** A step that blocks for a moment, which an interrupt flag already set would end at once. */
    private fun pausing() =
        building("pack") {
            SyncStepRun {
                Thread.sleep(1)
                Outcome.success()
            }
        }
}

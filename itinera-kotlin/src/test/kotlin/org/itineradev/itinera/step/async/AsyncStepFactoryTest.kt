package org.itineradev.itinera.step.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.samples.asyncFactories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncStepFactoryTest {
    @Test
    fun `a factory builds a run whose outcome the caller awaits`() =
        runBlocking<Unit> {
            assertEquals(Outcome.success(), asyncFactories(Answers(emptyMap())))
        }
}

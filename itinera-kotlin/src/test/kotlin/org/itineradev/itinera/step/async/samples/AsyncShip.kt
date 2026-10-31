@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async

/** Ships a parcel, waiting for the carrier without blocking a thread. */
class AsyncShip {
    suspend fun run(): Outcome {
        delay(1)
        return Outcome.success()
    }
}

suspend fun asyncFactories(got: Got<StepKind<Async>>): Outcome {
    val factory = AsyncStepFactory {
        val step = AsyncShip()
        AsyncStepRun { step.run() }
    }
    return factory.build(got).run()
}

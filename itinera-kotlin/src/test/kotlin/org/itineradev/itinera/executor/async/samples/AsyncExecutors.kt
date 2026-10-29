package org.itineradev.itinera.executor.async.samples

import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.async.AsyncDefaultDispatcherFactory
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncBuilding

suspend fun asyncExecutors(): JourneyResult {
    val executor = AsyncLocalExecutor(AsyncDefaultDispatcherFactory())
    val result = executor.run(asyncBuilding().instance(AsyncOrders("corner")).create())
    check(result.isSucceeded)
    return result
}

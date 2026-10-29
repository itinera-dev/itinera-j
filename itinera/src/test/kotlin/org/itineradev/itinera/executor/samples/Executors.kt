package org.itineradev.itinera.executor.samples

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.DefaultDispatcherFactory
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.building

fun executors(): JourneyResult {
    val executor = LocalExecutor(DefaultDispatcherFactory())
    val result = executor.run(building().instance(Orders("corner")).create())
    check(result.isSucceeded)
    return result
}

fun refusals(): Refused {
    val executor = LocalExecutor()
    val instance = building().instance(Orders("corner")).create()
    executor.run(instance)
    return try {
        executor.run(instance)
        error("An instance runs once.")
    } catch (refusal: Refusal) {
        refusal.refused
    }
}

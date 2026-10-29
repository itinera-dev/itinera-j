package org.itineradev.itinera.workflow.kotlin

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName
import org.itineradev.itinera.workflow.kotlin.samples.Shop
import org.itineradev.itinera.workflow.kotlin.samples.reifiedBuilding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildersTest {
    @Test
    fun `the reified builder makes a workflow called with objects of its type`() {
        val orders = reifiedBuilding()
        assertEquals(WorkflowName.of("orders"), orders.name)
        val result = LocalExecutor().run(orders.instance(Shop("corner")).create())
        assertEquals(JourneyId.of("corner-1"), result.journeyId)
    }
}

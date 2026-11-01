package org.itineradev.itinera.executor

import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.executor.samples.executors
import org.itineradev.itinera.executor.samples.refusals
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalExecutorTest {
    @Test
    fun `an executor refuses a journey while it runs one`() {
        val executor = LocalExecutor()
        val inner = packing().create()
        val refusals = mutableListOf<Refused>()
        val outer = packing().reporter(Nested(executor, inner, refusals)).create()
        assertTrue(executor.run(outer).isSucceeded)
        assertEquals(listOf(Refused.JourneyRunning), refusals)
        assertTrue(executor.run(inner).isSucceeded)
    }

    @Test
    fun `the samples run a journey and are refused the same instance twice`() {
        assertTrue(executors().isSucceeded)
        assertEquals(Refused.InstanceRan, refusals())
    }

    @Test
    fun `a refusal says why, and holds what refused it`() {
        val executor = LocalExecutor()
        val instance = packing().create()
        executor.run(instance)
        val refusal = assertThrows<Refusal> { executor.run(instance) }
        assertEquals(
            "run refused an instance handed to an executor before: create an instance for each journey",
            refusal.message,
        )
        assertEquals(Refused.InstanceRan, refusal.refused)
    }
}

/** Tries, on the first event, to run another journey on the executor running this one. */
private class Nested(
    private val executor: LocalExecutor,
    private val instance: WorkflowInstance<Sync>,
    private val refusals: MutableList<Refused>,
) : Reporter {
    override fun report(event: Event) {
        if (refusals.isEmpty()) refusals += assertThrows<Refusal> { executor.run(instance) }.refused
    }
}

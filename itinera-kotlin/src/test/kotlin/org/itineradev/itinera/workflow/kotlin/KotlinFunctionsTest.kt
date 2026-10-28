@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.describing
import org.itineradev.itinera.workflow.annotated.samples.shipping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinFunctionsTest {
    @Test
    fun `the samples' annotated Kotlin workflows are read and run`() {
        val result = shipping()
        assertTrue(result.isSucceeded)
        assertEquals(JourneyId.of("north-7"), result.journeyId)
        assertEquals(
            listOf(StepName.of("unpack")),
            describing().steps.map(StepDescriptor<Sync>::name),
        )
    }

    @Test
    fun `a suspend run in a synchronous workflow is refused with mode not accepted`() {
        assertEquals(
            listOf(Violation.ModeNotAccepted("step pack", Sync)),
            violationsOf(Suspending::class.java),
        )
    }

    @Test
    fun `return types that admit null are refused when described`() {
        assertEquals(
            listOf("returns Outcome?, which admits null", "returns String?, which admits null"),
            violationsOf(Lenient::class.java).map(::problem),
        )
    }

    @Test
    fun `a suspend ID generator is refused when described`() {
        assertEquals(
            listOf("is suspend, which an ID generator never is"),
            violationsOf(Waiting::class.java).map(::problem),
        )
    }

    private fun violationsOf(type: Class<out ItineraWorkflow>): List<Violation> =
        assertThrows<Violations> { WorkflowDescriptor.of(type) }.violations

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem
}

class Pausing : ItineraStep {
    suspend fun run(): Outcome = succeed()
}

class Nullable : ItineraStep {
    fun run(): Outcome? = succeed()
}

@Workflow(name = "suspending")
@WithStep(Pausing::class, name = "pack")
class Suspending : ItineraWorkflow

@Workflow(name = "lenient")
@WithStep(Nullable::class, name = "pack")
class Lenient : ItineraWorkflow {
    @IdGenerator fun id(data: DataBag): String? = data.toString()
}

@Workflow(name = "waiting")
class Waiting : ItineraWorkflow {
    @IdGenerator suspend fun id(): String = javaClass.simpleName
}

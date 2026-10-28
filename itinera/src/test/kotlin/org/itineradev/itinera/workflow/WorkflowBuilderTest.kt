package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.request.samples.requests
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.building
import org.itineradev.itinera.workflow.samples.ids
import org.itineradev.itinera.workflow.samples.modes
import org.itineradev.itinera.workflow.samples.steps
import org.itineradev.itinera.workflow.samples.violations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class WorkflowBuilderTest {
    @Test
    fun `two steps with one name are refused when the workflow is built`() {
        assertEquals(listOf(Violation.DuplicateStepName(StepName.of("charge"))), violations())
    }

    @Test
    fun `a step requesting a service no provider serves is refused when the workflow is built`() {
        val refused =
            assertThrows<Violations> {
                WorkflowDescriptor.builder(Orders::class.java, "orders").step(requests()).build()
            }
        assertEquals(
            listOf(Violation.ServiceNotProvided(StepName.of("charge"), "payments")),
            refused.violations,
        )
    }

    @Test
    fun `a step requesting one key twice among its services is refused once for the key`() {
        val charge =
            StepDescriptor.builder("charge") { error("never built") }
                .request(Requests.service("payments", PaymentGateway::class.java))
                .request(Requests.service("payments", String::class.java))
                .build()
        val refused =
            assertThrows<Violations> {
                WorkflowDescriptor.builder(Orders::class.java, "orders").step(charge).build()
            }
        assertEquals(
            listOf("service requested twice", "service not provided"),
            refused.violations.map(Violation::named),
        )
    }

    @Test
    fun `a step states its retry budget and whether it retries abnormal terminations`() {
        val charge = steps()
        assertEquals(2, charge.retryBudget)
        assertFalse(charge.retryOnAbnormalTermination)
    }

    @Test
    fun `a retry budget below 0 is refused when declared`() {
        assertThrows<IllegalArgumentException> {
            StepDescriptor.builder("charge") { error("never") }.retryBudget(-1)
        }
    }

    @Test
    fun `a built workflow keeps its name, mode, steps and ID generator`() {
        val orders = building()
        assertEquals(WorkflowName.of("orders"), orders.name)
        assertEquals(Sync, modes())
        assertEquals(listOf(StepName.of("charge")), orders.steps.map(StepDescriptor<Sync>::name))
        assertNull(orders.idGenerator)
        assertNotNull(ids().idGenerator)
    }

    @Test
    fun `violations say what refused the workflow`() {
        assertEquals(
            "The workflow is refused: DuplicateStepName(step=charge)",
            Violations(violations()).message,
        )
    }
}

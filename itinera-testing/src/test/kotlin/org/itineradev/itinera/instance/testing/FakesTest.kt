@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.instance.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.testing.samples.AcceptingGateway
import org.itineradev.itinera.instance.testing.samples.Gateway
import org.itineradev.itinera.instance.testing.samples.Pay
import org.itineradev.itinera.instance.testing.samples.Payments
import org.itineradev.itinera.instance.testing.samples.asyncFakes
import org.itineradev.itinera.instance.testing.samples.fakes
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.report.testing.complete
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FakesTest {
    @Test
    fun `a fake replaces its provider, and service_provided names the fake's implementation and data`() {
        assertTrue(fakes().isSucceeded)
    }

    @Test
    fun `an asynchronous fake provider replaces the provider's function`() {
        assertTrue(complete { asyncFakes() }.isSucceeded)
    }

    @Test
    fun `a fake named after a provider the workflow does not have is a mistake`() {
        val mistake =
            assertThrows<TestMistake> {
                payments().fakeProvider("courier", Gateway::class.java, ::accepting)
            }
        assertEquals(listOf(Mistake.UnknownProvider("courier")), mistake.problems)
    }

    @Test
    fun `a fake making another service than its provider is a mistake`() {
        val mistake =
            assertThrows<TestMistake> {
                payments().fakeProvider("gateway", String::class.java) {
                    Provided.Instance("label")
                }
            }
        assertEquals(listOf(Mistake.AnotherService("gateway")), mistake.problems)
    }

    @Test
    fun `a fake that gives no instance aborts the journey, as its provider would`() {
        val instance =
            payments()
                .fakeProvider("gateway", Gateway::class.java) { Provided.noInstance() }
                .create()
        val result = LocalExecutor().run(instance)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort?.reason)
    }

    @Test
    fun `a fake of attempt scope is called for every attempt`() {
        val references = ArrayDeque(listOf("declined", "accepted"))
        val instance =
            TestInstanceBuilder.of(Retried())
                .fakeProvider("gateway", Gateway::class.java) {
                    Provided.Instance(Gateway { references.removeFirst() })
                }
                .create()
        assertTrue(LocalExecutor().run(instance).isSucceeded)
        assertTrue(references.isEmpty())
    }

    @Test
    fun `a fake leaves the workflow's listing as it was`() {
        val instance = payments().fakeProvider("gateway", Gateway::class.java, ::accepting).create()
        assertEquals(
            WorkflowDescriptor.of(Payments::class.java).listing(),
            instance.descriptor.listing(),
        )
    }

    private fun payments(): TestInstanceBuilder = TestInstanceBuilder.of(Payments())

    private fun accepting(): Provided<Gateway> = Provided.Instance(AcceptingGateway())
}

/** Takes payments through a gateway lent for each attempt, retrying once. */
@Workflow(name = "retried payments")
@WithStep(Pay::class, name = "pay", retryBudget = 1)
class Retried : ItineraWorkflow {
    @Provider(perAttempt = true) fun gateway(): Provided<Gateway> = provide(AcceptingGateway())
}

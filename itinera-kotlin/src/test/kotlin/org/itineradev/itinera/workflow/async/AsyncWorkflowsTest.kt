@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.samples.AsyncReturns
import org.itineradev.itinera.workflow.async.samples.asyncBuilding
import org.itineradev.itinera.workflow.async.samples.asyncDescribing
import org.itineradev.itinera.workflow.async.samples.asyncShipping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AsyncWorkflowsTest {
    @Test
    fun `the samples' asynchronous workflows are built, read and run`() =
        runBlocking<Unit> {
            val result = asyncShipping()
            assertTrue(result.isSucceeded)
            assertEquals(JourneyId.of("north-1"), result.journeyId)
            assertEquals(Async, asyncBuilding().mode)
            assertEquals(
                listOf(StepName.of("unpack")),
                asyncDescribing().steps.map(StepDescriptor<Async>::name),
            )
        }

    @Test
    fun `an annotated class is read once, and its descriptor shared`() {
        assertSame(asyncDescribing(), asyncDescriptorOf(AsyncReturns::class))
    }

    @Test
    fun `a run without suspend in an asynchronous workflow is refused with mode not accepted`() {
        val violations = assertThrows<Violations> { asyncDescriptorOf(Blocking::class) }
        assertEquals(listOf(Violation.ModeNotAccepted("step wait", Async)), violations.violations)
    }

    @Test
    fun `a Java step in an asynchronous workflow is refused with mode not accepted`() {
        val violations = assertThrows<Violations> { asyncDescriptorOf(JavaParcels::class) }
        assertEquals(listOf(Violation.ModeNotAccepted("step pack", Async)), violations.violations)
    }

    @Test
    fun `each instance builds its own reporter of each class an annotated workflow lists`() {
        val first = Quietly().instance().create()
        val second = Quietly().instance().create()
        assertEquals(listOf(Quiet::class.java), first.reporters.map(Any::javaClass))
        assertNotSame(first.reporters.single(), second.reporters.single())
    }

    @Test
    fun `a synchronous reporter class in an asynchronous workflow is refused with mode not accepted`() {
        val violations = assertThrows<Violations> { asyncDescriptorOf(Loudly::class) }
        assertEquals(
            listOf(Violation.ModeNotAccepted("reporter ${Loud::class.java.name}", Async)),
            violations.violations,
        )
    }

    @ParameterizedTest
    @FieldSource("throwing")
    fun `what a suspend run throws, before or after it suspends, is its own error`(
        workflow: ItineraAsyncWorkflow,
        message: String,
    ) =
        runBlocking<Unit> {
            val failure = AsyncLocalExecutor().run(workflow.instance().create()).failure
            assertEquals(message, failure?.errorMessage)
            assertTrue(failure?.error is IllegalStateException)
        }

    companion object {
        @JvmField
        val throwing =
            listOf(
                argumentSet("before it suspends", Hasty(), "at once"),
                argumentSet("once it suspended", Tardy(), "later"),
            )
    }
}

/** Runs without `suspend`, as an asynchronous step must not. */
@Step
class Wait : ItineraStep {
    fun run(): Outcome = succeed()
}

@WithStep(Wait::class, name = "wait") class Blocking : ItineraAsyncWorkflow

/** Throws before it ever suspends. */
@Step
class AtOnce : ItineraStep {
    suspend fun run(): Outcome = throw IllegalStateException("at once")
}

@WithStep(AtOnce::class, name = "at once") class Hasty : ItineraAsyncWorkflow

/** Throws once it has suspended. */
@Step
class Later : ItineraStep {
    suspend fun run(): Outcome {
        delay(1)
        throw IllegalStateException("later")
    }
}

@WithStep(Later::class, name = "later") class Tardy : ItineraAsyncWorkflow

/** Reports nothing, as an asynchronous reporter. */
class Quiet : AsyncReporter {
    override suspend fun report(event: Event) = Unit
}

@Workflow(name = "quietly", reporters = [Quiet::class])
@WithStep(AtOnce::class, name = "at once")
class Quietly : ItineraAsyncWorkflow

/** Reports nothing, as a synchronous reporter, which an asynchronous workflow cannot list. */
class Loud : Reporter {
    override fun report(event: Event) = Unit
}

@Workflow(name = "loudly", reporters = [Loud::class])
@WithStep(AtOnce::class, name = "at once")
class Loudly : ItineraAsyncWorkflow

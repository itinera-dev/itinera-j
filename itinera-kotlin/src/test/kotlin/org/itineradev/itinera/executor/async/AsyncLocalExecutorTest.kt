package org.itineradev.itinera.executor.async

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.executor.async.AsyncJourneys.journeyOf
import org.itineradev.itinera.executor.async.AsyncJourneys.packing
import org.itineradev.itinera.executor.async.AsyncJourneys.running
import org.itineradev.itinera.executor.async.AsyncJourneys.succeeding
import org.itineradev.itinera.executor.async.samples.asyncExecutors
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class AsyncLocalExecutorTest {
    @Test
    fun `an asynchronous journey awaits each step and succeeds`() =
        runBlocking<Unit> {
            val log = AsyncLog()
            val waiting =
                running("pack") {
                    delay(1)
                    Outcome.success()
                }
            val result =
                AsyncLocalExecutor()
                    .run(journeyOf(waiting, succeeding("ship")).reporter(log).create())
            assertTrue(result.isSucceeded)
            assertEquals(
                listOf(
                    "journey_started",
                    "attempt_started",
                    "step_succeeded",
                    "attempt_started",
                    "step_succeeded",
                    "journey_succeeded",
                ),
                log.kinds,
            )
            assertTrue(asyncExecutors().isSucceeded)
        }

    @Test
    fun `an executor refuses a journey while it runs one`() =
        runBlocking<Unit> {
            val executor = AsyncLocalExecutor()
            val inner = packing().create()
            val refusals = mutableListOf<Refused>()
            val outer = packing().reporter(Nested(executor, inner, refusals)).create()
            assertTrue(executor.run(outer).isSucceeded)
            assertEquals(listOf(Refused.JourneyRunning), refusals)
            assertTrue(executor.run(inner).isSucceeded)
        }

    @Test
    fun `a cancelled coroutine abandons its journey with no further event`() =
        runBlocking<Unit> {
            val log = AsyncLog()
            val started = CompletableDeferred<Unit>()
            val waiting =
                running("pack") {
                    started.complete(Unit)
                    awaitCancellation()
                }
            val journey = async {
                AsyncLocalExecutor().run(journeyOf(waiting).reporter(log).create())
            }
            started.await()
            journey.cancel()
            assertThrows<CancellationException> { journey.await() }
            assertEquals(listOf("journey_started", "attempt_started"), log.kinds)
        }

    @Test
    fun `a step's own timeout is an abnormal termination`() =
        runBlocking<Unit> {
            val timing = running("pack") { withTimeout(1) { awaitCancellation() } }
            val result = AsyncLocalExecutor().run(journeyOf(timing).create())
            assertTrue(result.failure?.details is JourneyFailure.AbnormalTermination)
        }

    @Test
    fun `a journey whose coroutine is cancelled while journey_aborted is delivered is abandoned`() =
        runBlocking<Unit> {
            val journey = async {
                AsyncLocalExecutor()
                    .run(
                        packing()
                            .reporter(Throwing(EventKind.ATTEMPT_STARTED))
                            .reporter(CancellingOnAbort())
                            .create()
                    )
            }
            assertThrows<CancellationException> { journey.await() }
        }

    @Test
    fun `a CancellationException a reporter throws while its coroutine is active is a failure`() =
        runBlocking<Unit> {
            val result =
                AsyncLocalExecutor()
                    .run(packing().reporter(Throwing(EventKind.STEP_SUCCEEDED)).create())
            assertEquals(ReporterFailed(StepName.of("pack"), "timed out"), result.abort?.details)
        }

    @Test
    fun `a dispatcher factory written in Java is refused`() {
        val refused =
            assertThrows<IllegalArgumentException> {
                AsyncLocalExecutor(WrittenInJava.DispatcherFactory())
            }
        assertEquals(
            "The dispatcher factory ${WrittenInJava.DispatcherFactory::class.java.name} is " +
                "written in Java, which cannot write an asynchronous dispatcher factory's " +
                "suspend operation.",
            refused.message,
        )
    }

    @Test
    fun `an instance of another mode is refused with mode not accepted`() =
        runBlocking<Unit> {
            val synchronous =
                WorkflowDescriptor.builder(AsyncJourneys.Shop::class.java, "shop")
                    .step(
                        StepDescriptor.builder("pack") { SyncStepRun { Outcome.success() } }.build()
                    )
                    .build()
                    .instance(AsyncJourneys.Shop())
                    .reporter(Reporter {})
                    .create()
            val foreign: WorkflowInstance<Async> = returned(synchronous)
            val refusal = assertThrows<Refusal> { AsyncLocalExecutor().run(foreign) }
            assertEquals(
                Refused.Inadmissible(
                    listOf(
                        Violation.ModeNotAccepted("workflow shop", Async),
                        Violation.ModeNotAccepted("step pack", Async),
                        Violation.ModeNotAccepted("reporter 1", Async),
                    )
                ),
                refusal.refused,
            )
        }
}

/** Tries, on the first event, to run another journey on the executor running this one. */
@OptIn(ExperimentalItineraApi::class)
private class Nested(
    private val executor: AsyncLocalExecutor,
    private val instance: WorkflowInstance<Async>,
    private val refusals: MutableList<Refused>,
) : AsyncReporter {
    override suspend fun report(event: Event) {
        if (refusals.isEmpty()) refusals += refused()
    }

    private suspend fun refused(): Refused =
        try {
            executor.run(instance)
            error("A journey ran while another was running.")
        } catch (refusal: Refusal) {
            refusal.refused
        }
}

/** Throws a `CancellationException` of its own on the first event of [kind]. */
@OptIn(ExperimentalItineraApi::class)
private class Throwing(private val kind: EventKind) : AsyncReporter {
    override suspend fun report(event: Event) {
        if (event.kind == kind) throw CancellationException("timed out")
    }
}

/** Cancels the coroutine running the journey once it receives `journey_aborted`. */
@OptIn(ExperimentalItineraApi::class)
private class CancellingOnAbort : AsyncReporter {
    override suspend fun report(event: Event) {
        if (event.kind == EventKind.JOURNEY_ABORTED) currentCoroutineContext().job.cancel()
    }
}

package org.itineradev.itinera.executor.async

import java.util.concurrent.atomic.AtomicBoolean
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Engine
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.async.AsyncDefaultDispatcherFactory
import org.itineradev.itinera.report.async.AsyncDispatcherFactory
import org.itineradev.itinera.workflow.Async

/**
 * Runs the journeys of asynchronous workflows in this process, in the caller's coroutine, one at a
 * time, with no persistence: a journey lives only as long as the call to [run] that runs it.
 * Several executors run journeys in parallel.
 *
 * It never spawns, sleeps, sets timers or chooses a thread: the journey runs on whatever dispatcher
 * the caller's coroutine uses, and awaits only what the workflow's own parts do. A part that blocks
 * belongs in `withContext(Dispatchers.IO)`.
 *
 * For each journey it makes a new dispatcher with its dispatcher factory, and keeps nothing of the
 * journey once [run] returns.
 *
 * @sample org.itineradev.itinera.executor.async.samples.asyncExecutors
 */
@ExperimentalItineraApi
public class AsyncLocalExecutor
/** An executor whose journeys deliver their events through dispatchers [factory] makes. */
public constructor(private val factory: AsyncDispatcherFactory = AsyncDefaultDispatcherFactory()) {
    private val running = AtomicBoolean()

    /**
     * Runs the one journey of [instance] and returns its result: it succeeded or failed, with its
     * data bag, or it was aborted. Nothing that happens in the journey escapes, except a
     * [java.lang.Error], which itinera never catches, and the cancellation of the coroutine calling
     * it, which abandons the journey with no further event. A `CancellationException` that a part
     * throws while this coroutine is active, such as a step's own `withTimeout`, is that part
     * failing like any other.
     *
     * @throws Refusal before any journey and any event: when the executor is running a journey, the
     *   instance ran before, it is not admitted, or the dispatcher factory, the dispatcher or a
     *   reporter fails before the journey starts.
     * @sample org.itineradev.itinera.executor.async.samples.asyncExecutors
     */
    @JvmSynthetic
    public suspend fun run(instance: WorkflowInstance<Async>): JourneyResult {
        if (!running.compareAndSet(false, true)) throw Refusal(Refused.JourneyRunning)
        try {
            return Engine(AsyncParts(factory)).run(instance)
        } finally {
            running.set(false)
        }
    }
}

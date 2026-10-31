package org.itineradev.itinera.executor

import java.util.concurrent.atomic.AtomicBoolean
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Engine
import org.itineradev.itinera.engine.SyncParts
import org.itineradev.itinera.engine.driven
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.DefaultDispatcherFactory
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.workflow.Sync

/**
 * Runs the journeys of synchronous workflows in this process, on the caller's thread, one at a
 * time, with no persistence: a journey lives only as long as the call to [run] that runs it.
 * Several executors run journeys in parallel.
 *
 * For each journey it makes a new dispatcher with its dispatcher factory, and keeps nothing of the
 * journey once [run] returns.
 *
 * @sample org.itineradev.itinera.executor.samples.executors
 */
@ExperimentalItineraApi
public class LocalExecutor
/** An executor whose journeys deliver their events through dispatchers [factory] makes. */
@JvmOverloads
public constructor(private val factory: DispatcherFactory = DefaultDispatcherFactory()) {
    private val running = AtomicBoolean()

    /**
     * Runs the one journey of [instance] and returns its result: it succeeded or failed, with its
     * data bag, or it was aborted. Nothing that happens in the journey escapes, except a
     * [java.lang.Error], which itinera never catches.
     *
     * @throws Refusal before any journey and any event: when the executor is running a journey, the
     *   instance ran before, it is not admitted, or the dispatcher factory, the dispatcher or a
     *   reporter fails before the journey starts.
     * @sample org.itineradev.itinera.executor.samples.executors
     */
    @Throws(Refusal::class)
    public fun run(instance: WorkflowInstance<Sync>): JourneyResult {
        if (!running.compareAndSet(false, true)) throw Refusal(Refused.JourneyRunning)
        try {
            return driven { Engine(SyncParts(factory)).run(instance) }
        } finally {
            running.set(false)
        }
    }
}

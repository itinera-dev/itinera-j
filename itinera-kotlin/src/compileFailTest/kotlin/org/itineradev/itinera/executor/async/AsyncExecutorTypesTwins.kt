package org.itineradev.itinera.executor.async

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.async.AsyncDispatcherFactory
import org.itineradev.itinera.workflow.Async

/** What [AsyncExecutorTypesTest] refuses, written so that it compiles. */
internal object AsyncExecutorTypesTwins {
    suspend fun run(instance: WorkflowInstance<Async>) = AsyncLocalExecutor().run(instance)

    fun executor(factory: DispatcherFactory) = LocalExecutor(factory)

    fun asyncExecutor(factory: AsyncDispatcherFactory) = AsyncLocalExecutor(factory)
}

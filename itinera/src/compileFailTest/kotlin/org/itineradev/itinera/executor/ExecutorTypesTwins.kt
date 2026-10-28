package org.itineradev.itinera.executor

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.workflow.Sync

/** What [ExecutorTypesTest] refuses, written so that it compiles. */
internal object ExecutorTypesTwins {
    fun run(instance: WorkflowInstance<Sync>) = LocalExecutor().run(instance)
}

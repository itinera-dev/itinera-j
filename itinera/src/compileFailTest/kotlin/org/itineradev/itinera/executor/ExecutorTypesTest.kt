package org.itineradev.itinera.executor

import org.itineradev.itinera.compiling.assertCompiles
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

private const val RUN =
    """
    import org.itineradev.itinera.executor.LocalExecutor
    import org.itineradev.itinera.instance.WorkflowInstance
    import org.itineradev.itinera.workflow.*
    """

class ExecutorTypesTest {
    @Test
    fun `the local executor runs only a synchronous instance`() {
        assertDoesNotCompile(
            "$RUN fun run(instance: WorkflowInstance<Async>) = LocalExecutor().run(instance)",
            "'WorkflowInstance<Async>', but 'WorkflowInstance<Sync>' was expected",
        )
        assertCompiles(
            "$RUN fun run(instance: WorkflowInstance<Sync>) = LocalExecutor().run(instance)"
        )
    }
}

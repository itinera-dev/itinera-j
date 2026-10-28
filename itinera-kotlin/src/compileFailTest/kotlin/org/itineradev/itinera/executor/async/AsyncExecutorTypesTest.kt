package org.itineradev.itinera.executor.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AsyncExecutorTypesTest {
    @ParameterizedTest
    @FieldSource("otherModes")
    fun `each executor takes only the parts of its own mode`(refused: String, error: String) {
        assertDoesNotCompile("$MODES $refused", error)
    }

    companion object {
        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "a synchronous instance on the asynchronous executor",
                    "suspend fun run(instance: WorkflowInstance<Sync>) = " +
                        "AsyncLocalExecutor().run(instance)",
                    "'WorkflowInstance<Sync>', but 'WorkflowInstance<Async>' was expected",
                ),
                argumentSet(
                    "an asynchronous dispatcher factory for the local executor",
                    "fun executor(factory: AsyncDispatcherFactory) = LocalExecutor(factory)",
                    "'AsyncDispatcherFactory', but 'DispatcherFactory' was expected",
                ),
                argumentSet(
                    "a synchronous dispatcher factory for the asynchronous executor",
                    "fun executor(factory: DispatcherFactory) = AsyncLocalExecutor(factory)",
                    "'DispatcherFactory', but 'AsyncDispatcherFactory' was expected",
                ),
            )
    }
}

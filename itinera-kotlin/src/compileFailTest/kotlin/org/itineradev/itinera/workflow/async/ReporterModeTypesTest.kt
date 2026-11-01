package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val REPORTERS =
    """
    $MODES
    import org.itineradev.itinera.workflow.async.*

    class Shop

    val printer = ReporterFactory { Reporter {} }
    val asyncPrinter = AsyncReporterFactory { AsyncReporter {} }
    """

class ReporterModeTypesTest {
    @ParameterizedTest
    @FieldSource("reporters")
    fun `a workflow lists only reporters of its own mode`(workflow: String, refused: String) {
        assertDoesNotCompile(
            "$REPORTERS val shop = $workflow.reporter($refused)",
            "Argument type mismatch",
        )
    }

    companion object {
        @JvmField
        val reporters =
            listOf(
                argumentSet(
                    "a synchronous workflow",
                    "WorkflowDescriptor.builder(Shop::class.java, \"shop\")",
                    "asyncPrinter",
                ),
                argumentSet("an asynchronous workflow", "asyncBuilder<Shop>(\"shop\")", "printer"),
            )
    }
}

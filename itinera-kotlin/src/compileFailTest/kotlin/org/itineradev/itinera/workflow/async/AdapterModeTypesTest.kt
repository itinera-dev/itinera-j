package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val ADAPTERS =
    """
    $MODES
    import org.itineradev.itinera.workflow.async.*

    class Shop

    val pricing = InputAdapterDescriptor.builder<Shop>("pricing", "charge") { _, _ -> Supplied.FromDataBag }.build()
    val asyncPricing = asyncInputAdapter<Shop>("pricing", "charge") { _, _ -> Supplied.FromDataBag }.build()
    """

class AdapterModeTypesTest {
    @ParameterizedTest
    @FieldSource("adapters")
    fun `a workflow takes only input adapters of its own mode`(workflow: String, refused: String) {
        assertDoesNotCompile(
            "$ADAPTERS val shop = $workflow.inputAdapter($refused)",
            "Argument type mismatch",
        )
    }

    companion object {
        @JvmField
        val adapters =
            listOf(
                argumentSet(
                    "a synchronous workflow",
                    "WorkflowDescriptor.builder(Shop::class.java, \"shop\")",
                    "asyncPricing",
                ),
                argumentSet("an asynchronous workflow", "asyncBuilder<Shop>(\"shop\")", "pricing"),
            )
    }
}

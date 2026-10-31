package org.itineradev.itinera.service.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val PROVIDERS =
    """
    $MODES
    import org.itineradev.itinera.service.*
    import org.itineradev.itinera.service.async.*
    import org.itineradev.itinera.workflow.async.asyncBuilder

    class Shop

    val labels = ProviderDescriptor.builder<Shop, String>("labels", "labels", String::class.java) { _, _ -> Provided.NoInstance }.build()
    val asyncLabels = asyncProvider<Shop, String>("labels", "labels", String::class.java) { _, _ -> Provided.NoInstance }.build()
    """

class ProviderModeTypesTest {
    @ParameterizedTest
    @FieldSource("providers")
    fun `a workflow takes only service providers of its own mode`(
        workflow: String,
        refused: String,
    ) {
        assertDoesNotCompile(
            "$PROVIDERS val shop = $workflow.provider($refused)",
            "Argument type mismatch",
        )
    }

    companion object {
        @JvmField
        val providers =
            listOf(
                argumentSet(
                    "a synchronous workflow",
                    "WorkflowDescriptor.builder(Shop::class.java, \"shop\")",
                    "asyncLabels",
                ),
                argumentSet("an asynchronous workflow", "asyncBuilder<Shop>(\"shop\")", "labels"),
            )
    }
}

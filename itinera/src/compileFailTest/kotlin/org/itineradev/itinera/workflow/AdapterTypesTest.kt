package org.itineradev.itinera.workflow

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val ADAPTERS =
    """
    import org.itineradev.itinera.journey.DataBagAccess
    import org.itineradev.itinera.policy.Role
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.value.TypeToken
    import org.itineradev.itinera.workflow.*

    class Shop
    class Warehouse

    @Role
    fun interface Customers {
        fun tell(message: String)
    }
    """

class AdapterTypesTest {
    @ParameterizedTest
    @FieldSource("refused")
    fun `an input adapter requests only what its kind may`(refused: String) {
        assertDoesNotCompile(
            "$ADAPTERS fun read(got: Got<InputAdapterKind>) = got[Requests.$refused]",
            "Argument type mismatch",
        )
    }

    @Test
    fun `an input adapter always says what it supplies`() {
        assertDoesNotCompile(
            "$ADAPTERS val pricing = InputAdapterDescriptor.builder<Shop>(\"pricing\", \"charge\") " +
                "{ _, _ -> null }",
            "Null cannot be a value of a non-null type 'Supplied'",
        )
    }

    @Test
    fun `an adapter's access to the data bag has no method that writes`() {
        assertDoesNotCompile(
            "$ADAPTERS fun write(access: DataBagAccess) = access.put(\"amount\", 7L)",
            "Unresolved reference 'put' on receiver of type 'DataBagAccess'.",
        )
    }

    @Test
    fun `a workflow takes only input adapters of its own workflow objects`() {
        assertDoesNotCompile(
            "$ADAPTERS fun adapt(workflow: WorkflowBuilder<Shop, Sync>, " +
                "adapter: InputAdapterDescriptor<Warehouse, Sync>) = workflow.inputAdapter(adapter)",
            "actual type is 'InputAdapterDescriptor<Warehouse, Sync>', but " +
                "'InputAdapterDescriptor<Shop, Sync>' was expected",
        )
    }

    companion object {
        @JvmField
        val refused =
            listOf(
                argumentSet("a contributor", "contributor()"),
                argumentSet("a step reporter", "stepReporter()"),
                argumentSet("the attempt", "attempt()"),
                argumentSet("a role", "role(Customers::class.java)"),
                argumentSet(
                    "data from the step",
                    "fromStep(\"charged\", TypeToken.of(Long::class.javaObjectType))",
                ),
            )
    }
}

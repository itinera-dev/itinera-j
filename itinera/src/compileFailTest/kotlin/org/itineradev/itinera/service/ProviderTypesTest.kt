package org.itineradev.itinera.service

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val PROVIDERS =
    """
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.service.*
    import org.itineradev.itinera.workflow.*

    class Shop
    class Warehouse
    """

class ProviderTypesTest {
    @ParameterizedTest
    @FieldSource("attempted")
    fun `a provider of journey scope cannot request the attempt`(attempted: String) {
        assertDoesNotCompile("$PROVIDERS $attempted", "Argument type mismatch")
    }

    @ParameterizedTest
    @FieldSource("refused")
    fun `a service provider requests only what its kind may`(kind: String, refused: String) {
        assertDoesNotCompile(
            "$PROVIDERS fun read(got: Got<$kind>) = got[Requests.$refused]",
            "Argument type mismatch",
        )
    }

    @Test
    fun `a service provider always says what it provides`() {
        assertDoesNotCompile(
            "$PROVIDERS val warehouse = ProviderDescriptor.perAttempt<Shop, String>" +
                "(\"warehouse\", \"stock\", String::class.java) { _, _ -> null }",
            "Null cannot be a value of a non-null type 'Provided<String>'",
        )
    }

    @Test
    fun `a workflow takes only service providers of its own workflow objects`() {
        assertDoesNotCompile(
            "$PROVIDERS fun provide(workflow: WorkflowBuilder<Shop, Sync>, " +
                "provider: ProviderDescriptor<Warehouse, Sync>) = workflow.provider(provider)",
            "actual type is 'ProviderDescriptor<Warehouse, Sync>', but " +
                "'ProviderDescriptor<Shop, Sync>' was expected",
        )
    }

    companion object {
        @JvmField
        val attempted =
            listOf(
                argumentSet(
                    "declared on its builder",
                    "val p = ProviderDescriptor.builder<Shop, String>(\"p\", \"k\", " +
                        "String::class.java) { _, _ -> Provided.NoInstance }" +
                        ".request(Requests.attempt())",
                ),
                argumentSet(
                    "read from its answers",
                    "fun read(got: Got<JourneyProviderKind<Sync>>) = got[Requests.attempt()]",
                ),
            )

        @JvmField
        val refused =
            listOf(
                argumentSet(
                    "the step's name, in journey scope",
                    "JourneyProviderKind<Sync>",
                    "stepName()",
                ),
                argumentSet(
                    "a contributor, in attempt scope",
                    "AttemptProviderKind<Sync>",
                    "contributor()",
                ),
                argumentSet(
                    "a step reporter, in attempt scope",
                    "AttemptProviderKind<Sync>",
                    "stepReporter()",
                ),
                argumentSet("the key, in attempt scope", "AttemptProviderKind<Sync>", "key()"),
            )
    }
}

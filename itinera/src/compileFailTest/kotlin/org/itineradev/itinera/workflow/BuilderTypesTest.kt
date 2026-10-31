package org.itineradev.itinera.workflow

import org.itineradev.itinera.compiling.assertCompiles
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val SHOP =
    """
    import org.itineradev.itinera.step.*
    import org.itineradev.itinera.workflow.*

    class Shop

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop")
    val succeeding = SyncStepRun { Outcome.success() }

    suspend fun wait() {}
    """

class BuilderTypesTest {
    @Test
    fun `a step factory always builds a step`() {
        assertDoesNotCompile(
            "$SHOP val pack = StepDescriptor.builder(\"pack\") { null }",
            "Null cannot be a value of a non-null type 'SyncStepRun'",
        )
        assertCompiles("$SHOP val pack = StepDescriptor.builder(\"pack\") { succeeding }")
    }

    @Test
    fun `a step's run always gives an outcome`() {
        assertDoesNotCompile(
            "$SHOP val run = SyncStepRun { null }",
            "Null cannot be a value of a non-null type 'Outcome'",
        )
        assertCompiles("$SHOP val run = SyncStepRun { Outcome.success() }")
    }

    @ParameterizedTest
    @FieldSource("notStrings")
    fun `an ID generator gives the journey ID as a string`(generated: String, error: String) {
        assertDoesNotCompile("$SHOP val ids = shop.idGenerator { _, _ -> $generated }", error)
        assertCompiles("$SHOP val ids = shop.idGenerator { _, _ -> \"order-1\" }")
    }

    @Test
    fun `an ID generator never suspends`() {
        assertDoesNotCompile(
            "$SHOP val ids = shop.idGenerator { _, _ -> wait(); \"order-1\" }",
            "can only be called from a coroutine",
        )
        assertCompiles("$SHOP val ids = shop.idGenerator { _, _ -> \"order-1\" }")
    }

    @ParameterizedTest
    @FieldSource("otherModes")
    fun `a workflow takes only the steps of its own mode`(workflow: String, step: String) {
        assertDoesNotCompile(
            "$SHOP fun add(shop: WorkflowBuilder<Shop, $workflow>, step: StepDescriptor<$step>) = " +
                "shop.step(step)",
            "'StepDescriptor<$step>', but 'StepDescriptor<$workflow>' was expected",
        )
        assertCompiles(
            "$SHOP fun add(shop: WorkflowBuilder<Shop, $workflow>, " +
                "step: StepDescriptor<$workflow>) = shop.step(step)"
        )
    }

    companion object {
        @JvmField
        val notStrings =
            listOf(
                argumentSet("a number", "42", "expected 'String', actual 'Int'"),
                argumentSet(
                    "nothing",
                    "null",
                    "Null cannot be a value of a non-null type 'String'",
                ),
            )

        @JvmField
        val otherModes =
            listOf(
                argumentSet("an asynchronous step in a synchronous workflow", "Sync", "Async"),
                argumentSet("a synchronous step in an asynchronous workflow", "Async", "Sync"),
            )
    }
}

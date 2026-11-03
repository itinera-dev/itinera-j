package org.itineradev.itinera.workflow

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val RESCUES =
    """
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.step.*
    import org.itineradev.itinera.workflow.*
    """

class RescueTypesTest {
    @ParameterizedTest
    @FieldSource("skipping")
    fun `a rescue step never skips`(skipping: String, refused: String) {
        assertDoesNotCompile("$RESCUES $skipping", refused)
    }

    @ParameterizedTest
    @FieldSource("refused")
    fun `a rescue step requests only what its kind may`(refused: String) {
        assertDoesNotCompile(
            "$RESCUES fun read(got: Got<RescueKind<Sync>>) = got[Requests.$refused]",
            "Argument type mismatch",
        )
    }

    @Test
    fun `a rescue step's factory always builds a rescue step`() {
        assertDoesNotCompile(
            "$RESCUES val rescue = RescueStepDescriptor.builder { null }",
            "Null cannot be a value of a non-null type 'SyncRescueRun'",
        )
    }

    @Test
    fun `a rescue step's run always gives an outcome`() {
        assertDoesNotCompile(
            "$RESCUES val run = SyncRescueRun { null }",
            "Null cannot be a value of a non-null type 'RescueOutcome'",
        )
    }

    @Test
    fun `a step takes only a rescue step of its own mode`() {
        assertDoesNotCompile(
            "$RESCUES fun rescue(step: StepBuilder<Sync>, rescue: RescueStepDescriptor<Async>) = " +
                "step.rescue(rescue)",
            "actual type is 'RescueStepDescriptor<Async>', but " +
                "'RescueStepDescriptor<Sync>' was expected",
        )
    }

    companion object {
        @JvmField
        val skipping =
            listOf(
                argumentSet(
                    "a run returning a step's outcome",
                    "val r = RescueStepDescriptor.builder { SyncRescueRun { Outcome.success() } }",
                    "Return type mismatch: expected 'RescueOutcome', actual 'Outcome'.",
                ),
                argumentSet(
                    "an outcome of its own",
                    "object Skipped : RescueOutcome",
                    "Extending sealed classes or interfaces from a different module is prohibited.",
                ),
            )

        @JvmField
        val refused =
            listOf(
                argumentSet("the attempt", "attempt()"),
                argumentSet("the journey ID", "journeyId()"),
                argumentSet("the key", "key()"),
                argumentSet("a hook reporter", "hookReporter()"),
            )
    }
}

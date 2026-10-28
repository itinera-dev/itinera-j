package org.itineradev.itinera.workflow

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val ANNOTATED =
    """
    import org.itineradev.itinera.policy.*
    import org.itineradev.itinera.step.*
    import org.itineradev.itinera.workflow.*

    @Step
    class Charge : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @StepPolicy(name = "audit")
    class Audit : ItineraStepPolicy

    @WorkflowPolicy(name = "tell")
    class Tell : ItineraWorkflowPolicy
    """

class AnnotationTypesTest {
    @ParameterizedTest
    @FieldSource("misattached")
    fun `a step takes only step policies, and a workflow only workflow policies`(
        annotation: String,
        refused: String,
        expected: String,
    ) {
        assertDoesNotCompile(
            "$ANNOTATED $annotation class Shop : ItineraWorkflow",
            "actual type is 'Array<KClass<$refused>>', but 'Array<KClass<out $expected>>' was " +
                "expected",
        )
    }

    @Test
    fun `a step takes as its rescue step only a rescue step class`() {
        assertDoesNotCompile(
            "$ANNOTATED @Workflow(name = \"shop\") " +
                "@WithStep(Charge::class, name = \"charge\", rescue = Charge::class) " +
                "class Shop : ItineraWorkflow",
            "actual type is 'KClass<Charge>', but 'KClass<out ItineraRescueStep>' was expected",
        )
    }

    companion object {
        @JvmField
        val misattached =
            listOf(
                argumentSet(
                    "a workflow policy attached to a step",
                    "@Workflow(name = \"shop\") " +
                        "@WithStep(Charge::class, name = \"charge\", policies = [Tell::class])",
                    "Tell",
                    "ItineraStepPolicy",
                ),
                argumentSet(
                    "a step policy attached to a workflow",
                    "@Workflow(name = \"shop\", policies = [Audit::class]) " +
                        "@WithStep(Charge::class, name = \"charge\")",
                    "Audit",
                    "ItineraWorkflowPolicy",
                ),
            )
    }
}

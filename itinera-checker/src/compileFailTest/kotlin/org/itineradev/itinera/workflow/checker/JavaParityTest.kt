package org.itineradev.itinera.workflow.checker

import java.lang.reflect.Modifier
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.itineradev.itinera.workflow.checker.fixtures.Adapters
import org.itineradev.itinera.workflow.checker.fixtures.Assembly
import org.itineradev.itinera.workflow.checker.fixtures.Policies
import org.itineradev.itinera.workflow.checker.fixtures.Providers
import org.itineradev.itinera.workflow.checker.fixtures.Requests
import org.itineradev.itinera.workflow.checker.fixtures.Rescues
import org.itineradev.itinera.workflow.checker.fixtures.Steps
import org.itineradev.itinera.workflow.checker.fixtures.Values
import org.itineradev.itinera.workflow.checker.fixtures.Workflows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class JavaParityTest {
    @ParameterizedTest
    @MethodSource("mistaken")
    fun `the checker reports in Java every mistake that describing the workflow finds`(
        fixture: Class<*>
    ) {
        assertEquals(
            workflowsOf(fixture).flatMap(::violationsOf).map(::rendered).sorted(),
            javacErrors(javaSourceOf(fixture)).sorted(),
        )
    }

    @Test
    fun `the checker reports nothing in Java for a workflow describing finds no mistake in`() {
        val twins = JavaParityTwins::class.java
        assertEquals(emptyList<Violation>(), workflowsOf(twins).flatMap(::violationsOf))
        assertEquals(emptyList<String>(), javacErrors(javaSourceOf(twins)))
    }

    @Test
    fun `the checker reports nothing through KSP for the twins that name parts written in Kotlin`() {
        assertEquals(emptyList<String>(), kspErrors(javaSourceOf(JavaParityTwins::class.java)))
    }

    companion object {
        @JvmStatic
        fun mistaken(): List<Arguments> =
            listOf(
                argumentSet("workflow classes", Workflows::class.java),
                argumentSet("step classes", Steps::class.java),
                argumentSet("what steps request", Requests::class.java),
                argumentSet("rescue step classes", Rescues::class.java),
                argumentSet("policy classes", Policies::class.java),
                argumentSet("input adapters", Adapters::class.java),
                argumentSet("service providers", Providers::class.java),
                argumentSet("how a workflow is put together", Assembly::class.java),
                argumentSet("values", Values::class.java),
            )
    }
}

/** The workflow classes [fixture] declares, as its nested classes that are not abstract. */
internal fun workflowsOf(fixture: Class<*>): List<Class<*>> =
    fixture.declaredClasses.filter(::isWorkflow)

private fun isWorkflow(type: Class<*>): Boolean =
    !Modifier.isAbstract(type.modifiers) &&
        (ItineraWorkflow::class.java.isAssignableFrom(type) ||
            ItineraAsyncWorkflow::class.java.isAssignableFrom(type))

/** The violations describing the workflow [type] finds, in its mode. */
@Suppress("UNCHECKED_CAST")
internal fun violationsOf(type: Class<*>): List<Violation> =
    try {
        if (ItineraWorkflow::class.java.isAssignableFrom(type))
            WorkflowDescriptor.of(type as Class<ItineraWorkflow>)
        else asyncDescriptorOf((type as Class<ItineraAsyncWorkflow>).kotlin)
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }

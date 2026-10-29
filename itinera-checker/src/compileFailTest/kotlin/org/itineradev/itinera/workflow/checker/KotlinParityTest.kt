package org.itineradev.itinera.workflow.checker

import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.checker.fixtures.KotlinAdapters
import org.itineradev.itinera.workflow.checker.fixtures.KotlinPolicies
import org.itineradev.itinera.workflow.checker.fixtures.KotlinProviders
import org.itineradev.itinera.workflow.checker.fixtures.KotlinRequests
import org.itineradev.itinera.workflow.checker.fixtures.KotlinRescues
import org.itineradev.itinera.workflow.checker.fixtures.KotlinSteps
import org.itineradev.itinera.workflow.checker.fixtures.KotlinValues
import org.itineradev.itinera.workflow.checker.fixtures.KotlinWorkflows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class KotlinParityTest {
    @ParameterizedTest
    @MethodSource("mistaken")
    fun `the checker reports in Kotlin every mistake that describing the workflow finds`(
        fixture: Class<*>
    ) {
        assertEquals(
            workflowsOf(fixture).flatMap(::violationsOf).map(::rendered).sorted(),
            kspErrors(kotlinSourceOf(fixture)).sorted(),
        )
    }

    @Test
    fun `the checker reports nothing in Kotlin for a workflow describing finds no mistake in`() {
        val twins = KotlinParityTwins::class.java
        assertEquals(emptyList<Violation>(), workflowsOf(twins).flatMap(::violationsOf))
        assertEquals(emptyList<String>(), kspErrors(kotlinSourceOf(twins)))
    }

    companion object {
        @JvmStatic
        fun mistaken(): List<Arguments> =
            listOf(
                argumentSet("workflow classes", KotlinWorkflows::class.java),
                argumentSet("step classes", KotlinSteps::class.java),
                argumentSet("what steps request", KotlinRequests::class.java),
                argumentSet("rescue step classes", KotlinRescues::class.java),
                argumentSet("policy classes", KotlinPolicies::class.java),
                argumentSet("input adapters", KotlinAdapters::class.java),
                argumentSet("service providers", KotlinProviders::class.java),
                argumentSet("values", KotlinValues::class.java),
            )
    }
}

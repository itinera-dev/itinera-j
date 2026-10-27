package org.itineradev.itinera

import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.samples.names
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class NameTest {
    @ParameterizedTest
    @FieldSource("kinds")
    fun `a name is never empty`(name: (String) -> Name) {
        assertThrows<IllegalArgumentException> { name("") }
    }

    @ParameterizedTest
    @FieldSource("kinds")
    fun `names of one kind spelling the same text are equal`(name: (String) -> Name) {
        assertEquals(name("charge"), name("charge"))
        assertEquals(name("charge").hashCode(), name("charge").hashCode())
    }

    @Test
    fun `names of different kinds are never equal`() {
        assertNotEquals(StepName.of("charge"), PolicyName.of("charge"))
    }

    @Test
    fun `a name prints as its text`() {
        assertEquals("charge", StepName.of("charge").toString())
    }

    @Test
    fun `the sample names a journey and a step`() {
        assertEquals("journey order-42 ran charge", names())
    }

    companion object {
        @JvmField
        val kinds =
            listOf(
                argumentSet("a workflow's", WorkflowName::of),
                argumentSet("an adapter's", AdapterName::of),
                argumentSet("a step's", StepName::of),
                argumentSet("a policy's", PolicyName::of),
                argumentSet("a provider's", ProviderName::of),
            )
    }
}

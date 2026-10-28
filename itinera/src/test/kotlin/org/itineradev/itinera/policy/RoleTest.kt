@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.samples.Corner
import org.itineradev.itinera.policy.samples.Messages
import org.itineradev.itinera.policy.samples.Notify
import org.itineradev.itinera.policy.samples.notify
import org.itineradev.itinera.policy.samples.notifying
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class RoleTest {
    @Test
    fun `a workflow keeps the roles it provides`() {
        assertEquals(listOf(Messages::class.java), notifying().roles)
    }

    @Test
    fun `a hook calls the role it requests`() {
        val told = mutableListOf<String>()
        calledOnWorkflow(notify().onWorkflowSuccess, Notify(), Corner(told))
        assertEquals(listOf("corner: your order is paid"), told)
    }

    @ParameterizedTest
    @FieldSource("unmarked")
    fun `a role is an interface marked @Role, both to request and to provide`(use: () -> Unit) {
        assertEquals(
            "A role is an interface marked @Role, which ${Unmarked::class.java.name} is not.",
            assertThrows<IllegalArgumentException>(use).message,
        )
    }

    private fun interface Unmarked {
        fun tell(message: String)
    }

    private class Shouting : Unmarked {
        override fun tell(message: String) = Unit
    }

    companion object {
        @JvmField
        val unmarked =
            listOf(
                argumentSet("requested", using { Requests.role(Unmarked::class.java) }),
                argumentSet(
                    "provided",
                    using {
                        WorkflowDescriptor.builder(Shouting::class.java, "orders")
                            .role(Unmarked::class.java)
                    },
                ),
            )

        private fun using(use: () -> Any) = named("use", use)
    }
}

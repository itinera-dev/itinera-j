package org.itineradev.itinera.request.java

import java.util.Optional
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.java.samples.Notes
import org.itineradev.itinera.request.java.samples.javaHookRequests
import org.itineradev.itinera.request.java.samples.javaRequests
import org.itineradev.itinera.step.Reason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JavaRequestsTest {
    @Test
    fun `an Optional input is an optional input`() {
        assertEquals(
            listOf("optional input discount: Long", "contributor"),
            javaRequests().requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `an Optional hook request is an optional request of what it holds`() {
        assertEquals(
            listOf(
                "optional data from the step charged: Long",
                "optional data from the workflow limit: Long",
                "optional reason",
                "optional error",
                "contributor",
            ),
            javaHookRequests().onStepRetry?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a hook reads each Optional answer as its own type`() {
        val hook = checkNotNull(javaHookRequests().onStepRetry)
        val contributed = mutableListOf<Pair<String, Any?>>()
        val answers =
            listOf(
                Optional.of(40L),
                Optional.empty<Long>(),
                Optional.of(Reason("declined")),
                Optional.empty<Throwable>(),
                Contributor { key, value -> contributed += key to value },
            )
        val decided =
            (hook.call as SyncHook).call(Notes(), Answers(hook.requests.zip(answers).toMap()))
        assertEquals(KeepDefault, decided)
        assertEquals(listOf("note" to "charged 40 of 0: declined"), contributed)
    }
}

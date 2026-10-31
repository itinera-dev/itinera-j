package org.itineradev.itinera.request.kotlin

import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.kotlin.samples.Shelves
import org.itineradev.itinera.request.kotlin.samples.Shelving
import org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
import org.itineradev.itinera.request.kotlin.samples.reifiedRequests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinRequestsTest {
    @Test
    fun `reified requests keep Kotlin's types`() {
        assertEquals(
            listOf(
                "input parcels: List<String>",
                "optional input note: String",
                "service store: Store",
            ),
            reifiedRequests().map(Request<*, *>::toString),
        )
    }

    @Test
    fun `reified hook requests keep Kotlin's types`() {
        assertEquals(
            listOf(
                "data from the step parcels: List<String>",
                "optional data from the step label: String",
                "data from the workflow aisle: String",
                "optional data from the workflow note: String",
                "role Shelves",
            ),
            reifiedHookRequests().onStepSuccess?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a hook reads each reified request's answer as its own type`() {
        val hook = checkNotNull(reifiedHookRequests().onStepSuccess)
        val shelved = mutableListOf<String>()
        val answers =
            hook.requests.zip(
                listOf(listOf("books", "lamps"), null, "B", "fragile", Shelves(shelved::add))
            )
        val decided = (hook.call as SyncHook).call(Shelving(), Answers(answers.toMap()))
        assertEquals(KeepDefault, decided)
        assertEquals(
            listOf("books in B: unlabelled, fragile", "lamps in B: unlabelled, fragile"),
            shelved,
        )
    }
}

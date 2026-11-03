package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.model.Ending
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

// Cucumber reads every class of this glue package, and the class Kotlin writes for an inlined
// assertThrows<T> names this one as its outer class without being listed in it, which makes the
// read fail; the Java form needs no class of its own.
class TablesTest {
    @Test
    fun `a sentence lists names, each quoted, separated by commas`() {
        assertEquals(listOf("audit", "metrics", ""), names(""""audit", "metrics", """""))
    }

    @Test
    fun `a row of attempts gives its number, outcome, reason and contributions`() {
        assertEquals(
            ScriptedAttempt(
                number = 2,
                ending = Ending.FAILURE,
                code = "declined",
                message = "The card was declined.",
                details = mapOf("amount" to 10L),
                contributes = mapOf("tries" to 2L, "card" to "visa"),
            ),
            attemptOf(
                mapOf(
                    "attempt" to "2",
                    "outcome" to "failure",
                    "code" to "declined",
                    "message" to "The card was declined.",
                    "details" to """{"amount": 10}""",
                    "contributes" to """{"tries": 2, "card": "visa"}""",
                )
            ),
        )
    }

    @Test
    fun `a row of attempts leaves out the columns it does not fill`() {
        assertEquals(
            ScriptedAttempt(1, Ending.SUCCESS),
            attemptOf(mapOf("attempt" to "1", "outcome" to "success")),
        )
    }

    @Test
    fun `contributions that are not a JSON object are an error in the case`() {
        assertThrows(ModelException::class.java) {
            attemptOf(mapOf("attempt" to "1", "outcome" to "success", "contributes" to "[1]"))
        }
    }

    @Test
    fun `a row of attempts without a number is an error in the case`() {
        assertThrows(ModelException::class.java) {
            attemptOf(mapOf("attempt" to "first", "outcome" to "success"))
        }
    }
}

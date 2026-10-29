package org.itineradev.itinera.journey

import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.journey.samples.outputs
import org.itineradev.itinera.journey.samples.results
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class JourneyResultTest {
    private val id = JourneyId.of("order-42")

    @Test
    fun `a failed journey reads as failed, with its failure and no abort`() {
        val failure =
            Failure(JourneyFailure.AbnormalTermination("down"), IllegalStateException("down"))
        val result = JourneyResult(id, JourneyStatus.Failed(failure, DataBags.of()))
        assertTrue(result.isFailed)
        assertFalse(result.isSucceeded || result.isAborted)
        assertEquals(failure, result.failure)
        assertNull(result.abort)
        assertEquals(FailureCause.ABNORMAL_TERMINATION, failure.cause)
        assertEquals("order-42 failed: abnormal termination, down", results(result))
    }

    @Test
    fun `an aborted journey reads as aborted, with its abort and no failure`() {
        val abort = Abort(ReporterFailed(null, "full"), null)
        val result = JourneyResult(id, JourneyStatus.Aborted(abort))
        assertTrue(result.isAborted)
        assertEquals(abort, result.abort)
        assertNull(result.failure)
        assertEquals("order-42 aborted: reporter failed at null", results(result))
    }

    @Test
    fun `a succeeded journey reads as succeeded`() {
        val result = JourneyResult(id, JourneyStatus.Succeeded(DataBags.of()))
        assertTrue(result.isSucceeded)
        assertEquals("order-42 succeeded with []", results(result))
    }

    @Test
    fun `a succeeded journey's output reads its data bag as a record`() {
        val bag = DataBags.of("order" to "42", "amount" to 40L)
        assertEquals(
            "order 42 charged 40",
            outputs(JourneyResult(id, JourneyStatus.Succeeded(bag))),
        )
    }

    @Test
    fun `a failed journey's output reads the data bag it left`() {
        val failure =
            Failure(JourneyFailure.AbnormalTermination("down"), IllegalStateException("down"))
        val bag = DataBags.of("order" to "42", "amount" to 40L)
        val failed = JourneyResult(id, JourneyStatus.Failed(failure, bag))
        assertEquals("order 42 charged 40", outputs(failed))
    }

    @ParameterizedTest
    @FieldSource("unread")
    fun `an output that cannot be read says why`(status: JourneyStatus, why: String) {
        assertEquals(why, outputs(JourneyResult(id, status)))
    }

    companion object {
        @JvmField
        val unread =
            listOf(
                argumentSet(
                    "a required key that holds nothing",
                    JourneyStatus.Succeeded(DataBags.of("order" to "42")),
                    "nothing under amount",
                ),
                argumentSet(
                    "a required key of a Kotlin record read without itinera-kotlin",
                    JourneyStatus.Succeeded(DataBags.of("amount" to 40L)),
                    "nothing under order",
                ),
                argumentSet(
                    "a key holding a value of another type",
                    JourneyStatus.Succeeded(DataBags.of("order" to "42", "amount" to 40)),
                    "another type under amount",
                ),
                argumentSet(
                    "an aborted journey",
                    JourneyStatus.Aborted(Abort(ReporterFailed(null, "full"), null)),
                    "aborted",
                ),
            )
    }
}

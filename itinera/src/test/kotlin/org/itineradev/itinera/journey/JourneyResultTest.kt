package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.journey.samples.results
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
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
}

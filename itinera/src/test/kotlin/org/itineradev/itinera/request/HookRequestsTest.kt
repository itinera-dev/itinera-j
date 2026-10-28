@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.called
import org.itineradev.itinera.policy.samples.Messages
import org.itineradev.itinera.request.samples.Watch
import org.itineradev.itinera.request.samples.hookRequests
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class HookRequestsTest {
    private val watch = hookRequests()

    @Test
    fun `each hook request says what it asks`() {
        assertEquals(
            listOf(
                "data from the step charged: Long",
                "optional data from the step note: String",
                "data from the workflow limit: Long",
                "optional data from the workflow discount: Long",
                "journey ID",
                "step name",
                "attempt",
                "optional reason",
                "optional error",
                "retry cause",
                "hook reporter",
                "contributor",
                "role Messages",
            ),
            watch.onStepRetry?.requests?.map(Request<*, *>::toString),
        )
        assertEquals(
            listOf("reason", "failure cause"),
            watch.onStepFailure?.requests?.map(Request<*, *>::toString),
        )
        assertEquals(
            listOf("error"),
            watch.onStepAbnormalTermination?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a hook reads each answer as its request's own type`() {
        val reported = mutableListOf<Pair<String, Any?>>()
        val contributed = mutableListOf<Pair<String, Any?>>()
        val told = mutableListOf<String>()
        val decided =
            called(
                watch.onStepRetry,
                Watch(),
                40L,
                "fragile",
                100L,
                null,
                JourneyId.of("order-1"),
                StepName.of("charge"),
                Attempt.of(2),
                Reason("declined"),
                null,
                RetryCause.FAILURE,
                Reporting(reported),
                Contributor { key, value -> contributed += key to value },
                Messages(told::add),
            )
        assertEquals(KeepDefault, decided)
        assertEquals(
            listOf(
                "retrying" to
                    "order-1 charge attempt 2 after failure: charged 40 of 100, less 0, declined, fragile"
            ),
            reported,
        )
        assertEquals(listOf("retried" to 2), contributed)
        assertEquals(listOf("trying again"), told)
    }

    @ParameterizedTest
    @FieldSource("failing")
    fun `the failure hooks read the reason, the failure's cause and the error`(
        call: () -> OnFailure,
        decided: OnFailure,
    ) {
        assertEquals(decided, call())
    }

    private class Reporting(private val reported: MutableList<Pair<String, Any?>>) : HookReporter {
        override fun info(message: String) {
            reported += message to null
        }

        override fun info(message: String, data: Any?) {
            reported += message to data
        }

        override fun warning(message: String) = Unit

        override fun warning(message: String, data: Any?) = Unit

        override fun error(message: String) = Unit

        override fun error(message: String, data: Any?) = Unit

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    companion object {
        @JvmField
        val failing =
            listOf(
                argumentSet(
                    "on step failure",
                    calling {
                        called(
                            hookRequests().onStepFailure,
                            Watch(),
                            Reason("declined"),
                            FailureCause.RETRIES_EXHAUSTED,
                        )
                    },
                    Lifecycle.FailWorkflow(Reason("declined after retries exhausted")),
                ),
                argumentSet(
                    "on step abnormal termination",
                    calling {
                        called(
                            hookRequests().onStepAbnormalTermination,
                            Watch(),
                            IllegalStateException(),
                        )
                    },
                    Lifecycle.FailWorkflow(Reason("IllegalStateException")),
                ),
            )

        private fun calling(call: () -> OnFailure) = named("call", call)
    }
}

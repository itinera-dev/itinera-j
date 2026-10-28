@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.executor.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.executor.async.AsyncJourneys.journeyOf
import org.itineradev.itinera.executor.async.AsyncJourneys.succeeding
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.async.asyncStepPolicy
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Violation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncPartsTest {
    @Test
    fun `a hook or a reporter written in Java is a part of the other mode`() =
        runBlocking<Unit> {
            val audit =
                asyncStepPolicy("audit", PolicyFactory(::Any)).onStepSuccess(WrittenInJava.Hook())
            val instance =
                journeyOf(succeeding("pack").policy(audit.build()))
                    .reporter(WrittenInJava.Reporter())
                    .create()
            assertEquals(
                Refused.Inadmissible(
                    listOf(
                        Violation.ModeNotAccepted("hook on step success of policy audit", Async),
                        Violation.ModeNotAccepted("reporter 1", Async),
                    )
                ),
                refused { AsyncLocalExecutor().run(instance) },
            )
        }

    @Test
    fun `a synchronous hook is a part of the other mode`() =
        runBlocking<Unit> {
            val audit =
                StepPolicyDescriptor.builder("audit", PolicyFactory(::Any))
                    .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
                    .build()
            val foreign: StepPolicyDescriptor<Any, Async> = returned(audit)
            val instance = journeyOf(succeeding("pack").policy(foreign)).create()
            assertEquals(
                Refused.Inadmissible(
                    listOf(Violation.ModeNotAccepted("hook on step success of policy audit", Async))
                ),
                refused { AsyncLocalExecutor().run(instance) },
            )
        }

    private suspend fun refused(run: suspend () -> Unit): Refused =
        try {
            run()
            error("The instance ran.")
        } catch (refusal: Refusal) {
            refusal.refused
        }
}

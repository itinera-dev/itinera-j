@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.executor.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.executor.async.AsyncJourneys.Shop
import org.itineradev.itinera.executor.async.AsyncJourneys.journeyOf
import org.itineradev.itinera.executor.async.AsyncJourneys.succeeding
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.async.asyncStepPolicy
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.async.asyncProvider
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncInputAdapter
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

    @Test
    fun `a synchronous input adapter, or one written in Java, is not of the executor's mode`() =
        runBlocking<Unit> {
            val synchronous: InputAdapterDescriptor<Any?, Async> =
                returned(
                    InputAdapterDescriptor.builder<Any?>("pricing", "pack") { _, _ ->
                            Supplied.FromDataBag
                        }
                        .build()
                )
            val java = asyncInputAdapter("discounts", "ship", WrittenInJava.Adapter()).build()
            val instance =
                asyncBuilder<Shop>("shop")
                    .step(succeeding("pack").build())
                    .step(succeeding("ship").build())
                    .inputAdapter(synchronous)
                    .inputAdapter(java)
                    .build()
                    .instance(Shop())
                    .create()
            assertEquals(
                Refused.Inadmissible(listOf(notOfTheMode("pricing"), notOfTheMode("discounts"))),
                refused { AsyncLocalExecutor().run(instance) },
            )
        }

    @Test
    fun `a synchronous service provider, or one written in Java, is a part of the other mode`() =
        runBlocking<Unit> {
            val synchronous: ProviderDescriptor<Any?, Async> =
                returned(
                    ProviderDescriptor.builder<Any?, String>(
                            "labels",
                            "labels",
                            String::class.java,
                        ) { _, _ ->
                            Provided.NoInstance
                        }
                        .build()
                )
            val java =
                asyncProvider("gateway", "payments", String::class.java, WrittenInJava.Provider())
                    .build()
            val instance =
                asyncBuilder<Shop>("shop")
                    .step(succeeding("pack").build())
                    .provider(synchronous)
                    .provider(java)
                    .build()
                    .instance(Shop())
                    .create()
            assertEquals(
                Refused.Inadmissible(
                    listOf(
                        Violation.ModeNotAccepted("service provider labels", Async),
                        Violation.ModeNotAccepted("service provider gateway", Async),
                    )
                ),
                refused { AsyncLocalExecutor().run(instance) },
            )
        }

    @Test
    fun `a synchronous rescue step is a part of the other mode`() =
        runBlocking<Unit> {
            val renewal =
                RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }.build()
            val foreign: RescueStepDescriptor<Async> = returned(renewal)
            val instance = journeyOf(succeeding("pack").rescue(foreign)).create()
            assertEquals(
                Refused.Inadmissible(
                    listOf(Violation.ModeNotAccepted("rescue step pack_rescue", Async))
                ),
                refused { AsyncLocalExecutor().run(instance) },
            )
        }

    private fun notOfTheMode(adapter: String): Violation =
        Violation.Misdeclared(
            "input adapter $adapter",
            "is not of the mode Async, which the executor runs",
        )

    private suspend fun refused(run: suspend () -> Unit): Refused =
        try {
            run()
            error("The instance ran.")
        } catch (refusal: Refusal) {
            refusal.refused
        }
}

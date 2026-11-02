@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.executor.async

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.event.Event
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
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.async.AsyncWorkflowHook
import org.itineradev.itinera.policy.async.asyncStepPolicy
import org.itineradev.itinera.policy.async.asyncWorkflowPolicy
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncHookReporter
import org.itineradev.itinera.request.async.asyncProviderReporter
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.async.asyncProvider
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncInputAdapter
import org.itineradev.itinera.workflow.async.asyncStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `a synchronous input adapter, or one written in Java, is a part of the other mode`() =
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

    @Test
    fun `an asynchronous input adapter and service provider are awaited while a step is built`() =
        runBlocking<Unit> {
            val order = Requests.input("order", TypeToken.of(String::class.java))
            val labels = Requests.service("labels", String::class.java)
            val built = mutableListOf<String>()
            val ship =
                asyncStep("ship") { got ->
                        built += "${got[order]} ${got[labels]}"
                        AsyncStepRun { Outcome.success() }
                    }
                    .request(order)
                    .request(labels)
            val instance =
                asyncBuilder<Shop>("shop")
                    .step(ship.build())
                    .inputAdapter(
                        asyncInputAdapter<Shop>("pricing", "ship") { _, _ ->
                                Supplied.Value(later("42"))
                            }
                            .build()
                    )
                    .provider(
                        asyncProvider<Shop, String>("printer", "labels", String::class.java) { _, _
                                ->
                                Provided.Instance(later("label"))
                            }
                            .build()
                    )
                    .build()
                    .instance(Shop())
                    .create()
            assertTrue(AsyncLocalExecutor().run(instance).isSucceeded)
            assertEquals(listOf("42 label"), built)
        }

    @Test
    fun `an asynchronous step's reporter emits and sets global metadata, awaiting every reporter`() =
        runBlocking<Unit> {
            val reporter = Requests.asyncStepReporter()
            val ship =
                asyncStep("ship") { got ->
                        val handle = got[reporter]
                        AsyncStepRun {
                            handle.info("packed", mapOf("parcels" to 2L))
                            handle.setGlobalMetadata("carrier", "north")
                            Outcome.success()
                        }
                    }
                    .request(reporter)
            val log = Yielding()
            val instance = journeyOf(ship).reporter(log).create()
            assertTrue(AsyncLocalExecutor().run(instance).isSucceeded)
            assertEquals(
                listOf("journey_started", "attempt_started", "step_info", "global_metadata_set"),
                log.kinds.take(4),
            )
            assertEquals(listOf("journey_id", "carrier"), log.keys)
        }

    @Test
    fun `an asynchronous provider's reporter emits and sets global metadata, awaiting every reporter`() =
        runBlocking<Unit> {
            val reporter = Requests.asyncProviderReporter()
            val labels = Requests.service("labels", String::class.java)
            val ship = asyncStep("ship") { _ -> AsyncStepRun { Outcome.success() } }.request(labels)
            val printer =
                asyncProvider<Shop, String>("printer", "labels", String::class.java) { _, got ->
                        got[reporter].warning("paper low")
                        got[reporter].setGlobalMetadata("printer", "north")
                        Provided.Instance("label")
                    }
                    .request(reporter)
            val log = Yielding()
            val instance =
                asyncBuilder<Shop>("shop")
                    .step(ship.build())
                    .provider(printer.build())
                    .build()
                    .instance(Shop())
                    .reporter(log)
                    .create()
            assertTrue(AsyncLocalExecutor().run(instance).isSucceeded)
            assertEquals(
                listOf("journey_warning", "global_metadata_set", "service_provided"),
                log.kinds.subList(2, 5),
            )
            assertEquals(listOf("journey_id", "printer"), log.keys)
        }

    @Test
    fun `asynchronous hooks are awaited, with their hook reporter, and decide with what they return`() =
        runBlocking<Unit> {
            val reporter = Requests.asyncHookReporter()
            val contributor = Requests.contributor()
            val audit =
                asyncStepPolicy("audit", PolicyFactory(::Any))
                    .onStepSuccess(
                        listOf(reporter),
                        AsyncHook { _, got -> audited(got[reporter]) },
                    )
                    .build()
            val ledger =
                asyncWorkflowPolicy("ledger", PolicyFactory(::Any))
                    .onWorkflowFailure(
                        listOf(contributor),
                        AsyncWorkflowHook { _, got ->
                            yield()
                            got[contributor].contribute("ledger", "failed")
                        },
                    )
                    .build()
            val log = Yielding()
            val instance =
                asyncBuilder<Shop>("shop")
                    .step(succeeding("pack").policy(audit).build())
                    .policy(ledger)
                    .build()
                    .instance(Shop())
                    .reporter(log)
                    .create()
            val result = AsyncLocalExecutor().run(instance)
            assertEquals("fraud", result.failure?.reason?.code)
            assertEquals(
                listOf(
                    "journey_info",
                    "hook_called",
                    "hook_called",
                    "contribution_committed",
                    "journey_failed",
                ),
                log.kinds.takeLast(5),
            )
        }

    private fun notOfTheMode(adapter: String): Violation =
        Violation.ModeNotAccepted("input adapter $adapter", Async)

    private suspend fun audited(reporter: AsyncHookReporter): OnSuccess {
        reporter.info("audited")
        return OnSuccess.failWorkflow("fraud")
    }

    private suspend fun later(value: String): String {
        yield()
        return value
    }

    private suspend fun refused(run: suspend () -> Unit): Refused =
        try {
            run()
            error("The instance ran.")
        } catch (refusal: Refusal) {
            refusal.refused
        }
}

/** Keeps the kind of every event and the key of every entry it receives, yielding first. */
private class Yielding : AsyncReporter {
    val kinds = mutableListOf<String>()
    val keys = mutableListOf<String>()

    override suspend fun report(event: Event) {
        yield()
        kinds += event.kind.toString()
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        yield()
        keys += key
    }
}

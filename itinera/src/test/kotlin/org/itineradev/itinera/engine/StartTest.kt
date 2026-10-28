package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.samples.Messages
import org.itineradev.itinera.report.Dispatcher
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.Fragile
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.JourneyIdGenerator
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.SyncInputAdapter
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class StartTest {
    @Test
    fun `the journey ID, then the initial global metadata, reach the reporters before journey_started`() {
        val log = Log()
        val instance =
            packing()
                .reportersGlobalMetadata("tenant", "acme")
                .reportersGlobalMetadata("region", "eu")
                .reporter(log)
                .create()
        ran(instance)
        assertEquals(
            listOf(
                "journey_id" to instance.journeyId.toString(),
                "tenant" to "acme",
                "region" to "eu",
            ),
            log.metadata,
        )
    }

    @Test
    fun `an instance runs once`() {
        val instance = packing().create()
        ran(instance)
        assertEquals(Refused.InstanceRan, refused { ran(instance) })
    }

    @Test
    fun `initial values that are not values, and journey_id in the metadata, are refused together with no event`() {
        val log = Log()
        val refused = refused {
            ran(
                packing()
                    .data("basket", StringBuilder("book"))
                    .reportersGlobalMetadata("journey_id", "mine")
                    .reportersGlobalMetadata("cart", Any())
                    .reporter(log)
                    .create()
            )
        }
        assertEquals(
            Refused.Inadmissible(
                listOf(
                    Violation.NotAValue(Violation.Held.INITIAL_DATA, "basket", null),
                    Violation.NotAValue(Violation.Held.INITIAL_GLOBAL_METADATA, "cart", null),
                    Violation.ReservedMetadataKey,
                )
            ),
            refused,
        )
        assertEquals(emptyList<String>(), log.kinds)
    }

    @Test
    fun `initial data whose copy fails is refused, listing the key and carrying the error`() {
        val refused = refused {
            ran(packing().data("vases", setOf(Fragile("vase"))).create())
        }
        val notAValue = (refused as Refused.Inadmissible).violations.single() as Violation.NotAValue
        assertEquals("vases", notAValue.key)
        assertEquals("cannot hash vase", notAValue.error?.message)
    }

    @Test
    fun `a hand-written descriptor is checked for every violation before the journey`() {
        val step = succeeding("pack").build()
        val refused = refused { ran(HandWritten(Written(listOf(step, step)), emptyList())) }
        assertEquals(Refused.Inadmissible(listOf(Violation.DuplicateStepName(step.name))), refused)
    }

    @Test
    fun `a hand-written step with a retry budget below 0 is refused`() {
        val step = succeeding("pack").build()
        val overdrawn =
            object : StepDescriptor<Sync> by step {
                override val retryBudget: Int = -1
            }
        val refused = refused { ran(HandWritten(Written(listOf(overdrawn)), emptyList())) }
        assertEquals(
            Refused.Inadmissible(
                listOf(Violation.Misdeclared("step pack", "has a retry budget below 0"))
            ),
            refused,
        )
    }

    @Test
    fun `a hook of no mode the executor accepts is refused with mode not accepted`() {
        val audit =
            object : StepPolicyDescriptor<Any?, Sync> {
                override val name: PolicyName = PolicyName.of("audit")
                override val factory: PolicyFactory<Any?> = PolicyFactory { null }
                override val onStepSuccess:
                    HookDefinition<Sync, Any?, StepHookKind<Sync>, OnSuccess> =
                    object : HookDefinition<Sync, Any?, StepHookKind<Sync>, OnSuccess> {
                        override val requests: List<Request<StepHookKind<Sync>, *>> = emptyList()
                        override val call: HookCall<Sync, Any?, StepHookKind<Sync>, OnSuccess> =
                            object : HookCall<Sync, Any?, StepHookKind<Sync>, OnSuccess> {}
                    }
                override val onStepFailure = null
                override val onStepRetry = null
                override val onStepAbnormalTermination = null
            }
        val step = succeeding("pack").policy(audit).build()
        val refused = refused { ran(HandWritten(Written(listOf(step)), emptyList())) }
        assertEquals(
            Refused.Inadmissible(
                listOf(Violation.ModeNotAccepted("hook on step success of policy audit", Sync))
            ),
            refused,
        )
    }

    @Test
    fun `a workflow object that does not implement a role its descriptor provides is refused`() {
        val step = succeeding("pack").build()
        val refused = refused {
            ran(HandWritten(Written(listOf(step), listOf(unimplemented())), emptyList()))
        }
        assertEquals(
            Refused.Inadmissible(
                listOf(
                    Violation.Misdeclared(
                        "workflow written",
                        "provides role ${Messages::class.java.name}, which its workflow object " +
                            "does not implement",
                    )
                )
            ),
            refused,
        )
    }

    @Test
    fun `a part of another mode is refused with mode not accepted`() {
        val step = StepDescriptor.builder("pack") { error("never built") }.build()
        val foreign =
            object : StepDescriptor<Sync> by step {
                override val factory: StepFactory<Sync> = object : StepFactory<Sync> {}
            }
        val refused = refused {
            ran(HandWritten(Written(listOf(foreign)), listOf(object : ReporterOf<Sync> {})))
        }
        assertEquals(
            Refused.Inadmissible(
                listOf(
                    Violation.ModeNotAccepted("step pack", Sync),
                    Violation.ModeNotAccepted("reporter 1", Sync),
                )
            ),
            refused,
        )
    }

    @Test
    fun `a hand-written descriptor's input adapters are checked before the journey`() {
        val pricing =
            object : InputAdapterDescriptor<Shop, Sync> {
                override val name: AdapterName = AdapterName.of("pricing")
                override val steps: List<StepName> = listOf(StepName.of("ship"))
                override val requests: List<Request<InputAdapterKind, *>> = emptyList()
                override val call: InputAdapterCall<Shop, Sync> =
                    object : InputAdapterCall<Shop, Sync> {}
            }
        val discounts =
            object : InputAdapterDescriptor<Shop, Sync> {
                override val name: AdapterName = AdapterName.of("discounts")
                override val steps: List<StepName> = emptyList()
                override val requests: List<Request<InputAdapterKind, *>> = emptyList()
                override val call: InputAdapterCall<Shop, Sync> = SyncInputAdapter { _, _ ->
                    Supplied.FromDataBag
                }
            }
        val pack = succeeding("pack").build()
        val adapters = listOf(pricing, discounts)
        val refused = refused {
            ran(HandWritten(Written(listOf(pack), inputAdapters = adapters), emptyList()))
        }
        assertEquals(
            Refused.Inadmissible(
                listOf(
                    Violation.Misdeclared("input adapter discounts", "is attached to no step"),
                    Violation.InputAdapterForUnknownStep(pricing.name, StepName.of("ship")),
                    Violation.Misdeclared(
                        "input adapter pricing",
                        "is not of the mode Sync, which the executor runs",
                    ),
                )
            ),
            refused,
        )
    }

    @Test
    fun `a hand-written descriptor's service providers are checked before the journey`() {
        val gateway =
            object : AttemptProviderDescriptor<Shop, Sync> {
                override val name: ProviderName = ProviderName.of("gateway")
                override val key: String = "payments"
                override val type: Class<*> = String::class.java
                override val steps: List<StepName> = listOf(StepName.of("ship"))
                override val requests: List<Request<AttemptProviderKind<Sync>, *>> = emptyList()
                override val call: ProviderCall<Sync, Shop, AttemptProviderKind<Sync>, *> =
                    object : ProviderCall<Sync, Shop, AttemptProviderKind<Sync>, String> {}
            }
        val pack = succeeding("pack").build()
        val refused = refused {
            ran(HandWritten(Written(listOf(pack), providers = listOf(gateway)), emptyList()))
        }
        assertEquals(
            Refused.Inadmissible(
                listOf(
                    Violation.ServiceProviderForUnknownStep(gateway.name, StepName.of("ship")),
                    Violation.ModeNotAccepted("service provider gateway", Sync),
                )
            ),
            refused,
        )
    }

    @Test
    fun `a workflow of another mode is refused with mode not accepted`() {
        val asynchronous: WorkflowDescriptor<Shop, Sync> = returned(Asynchronous())
        val refused = refused { ran(HandWritten(asynchronous, emptyList())) }
        assertEquals(
            Refused.Inadmissible(listOf(Violation.ModeNotAccepted("workflow written", Sync))),
            refused,
        )
    }

    @Test
    fun `a dispatcher factory that throws refuses the instance, carrying the error`() {
        val error = IllegalStateException("no dispatcher")
        val refused = refused {
            ran(packing().create(), DispatcherFactory { throw error })
        }
        assertEquals(Refused.DispatcherFactoryFailed(error, "no dispatcher"), refused)
    }

    @Test
    fun `a dispatcher that throws while the reporters are added refuses the instance`() {
        val error = IllegalStateException("full")
        val refused = refused {
            ran(
                packing().reporter(Log()).create(),
                DispatcherFactory { Adding(error) },
            )
        }
        assertEquals(Refused.DispatcherFailed(error, "full"), refused)
    }

    @Test
    fun `a reporter that throws on the initial global metadata refuses the instance with no event`() {
        val error = IllegalStateException("no tenants")
        val log = Log()
        val refusal =
            assertThrows<Refusal> {
                ran(packing().reporter(log).reporter(Rejecting(error)).create())
            }
        assertEquals(Refused.GlobalMetadataNotSet(error, "no tenants"), refusal.refused)
        assertSame(error, refusal.cause)
        assertEquals(emptyList<String>(), log.kinds)
    }

    private fun refused(run: () -> Unit): Refused = assertThrows<Refusal>(run).refused

    /** A role the shop does not implement, as only an unchecked cast hands over. */
    private fun unimplemented(): Class<in Shop> = returned(Messages::class.java)
}

@OptIn(ExperimentalItineraApi::class)
private class Written(
    override val steps: List<StepDescriptor<Sync>>,
    override val roles: List<Class<in Shop>> = emptyList(),
    override val inputAdapters: List<InputAdapterDescriptor<Shop, Sync>> = emptyList(),
    override val providers: List<ProviderDescriptor<Shop, Sync>> = emptyList(),
) : WorkflowDescriptor<Shop, Sync> {
    override val name: WorkflowName = WorkflowName.of("written")
    override val mode: Sync = Sync
    override val policies: List<WorkflowPolicyDescriptor<*, Sync>> = emptyList()
    override val idGenerator: JourneyIdGenerator<Shop>? = null
}

/** A hand-written asynchronous workflow of no step, which only an unchecked cast hands over. */
@OptIn(ExperimentalItineraApi::class)
private class Asynchronous : WorkflowDescriptor<Shop, Async> {
    override val name: WorkflowName = WorkflowName.of("written")
    override val mode: Async = Async
    override val steps: List<StepDescriptor<Async>> = emptyList()
    override val policies: List<WorkflowPolicyDescriptor<*, Async>> = emptyList()
    override val inputAdapters: List<InputAdapterDescriptor<Shop, Async>> = emptyList()
    override val providers: List<ProviderDescriptor<Shop, Async>> = emptyList()
    override val roles: List<Class<in Shop>> = emptyList()
    override val idGenerator: JourneyIdGenerator<Shop>? = null
}

@OptIn(ExperimentalItineraApi::class)
private class HandWritten(
    override val descriptor: WorkflowDescriptor<*, Sync>,
    override val reporters: List<ReporterOf<Sync>>,
) : WorkflowInstance<Sync> {
    override val workflow: Any = Shop()
    override val journeyId: JourneyId = JourneyId.of("by-hand")
    override val initialData: Map<String, Any?> = emptyMap()
    override val initialGlobalMetadata: Map<String, Any?> = emptyMap()

    override fun claim(): Boolean = true
}

@OptIn(ExperimentalItineraApi::class)
private class Adding(private val thrown: Throwable) : Dispatcher {
    override fun add(reporter: Reporter): Unit = throw thrown

    override fun dispatch(event: Event) = Unit

    override fun setGlobalMetadata(key: String, value: Any?) = Unit
}

@OptIn(ExperimentalItineraApi::class)
private class Rejecting(private val thrown: Throwable) : Reporter {
    override fun report(event: Event) = Unit

    override fun setGlobalMetadata(key: String, value: Any?): Unit = throw thrown
}

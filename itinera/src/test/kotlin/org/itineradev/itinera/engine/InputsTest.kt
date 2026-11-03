package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.event.AdapterSource
import org.itineradev.itinera.event.ByAdapter
import org.itineradev.itinera.event.ByAdapterForKey
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.InputAdapterSupplied
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.event.WrongData
import org.itineradev.itinera.event.WrongType
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.SyncInputAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class InputsTest {
    private val order = Requests.input("order", TypeToken.of(String::class.java))
    private val note = Requests.optionalInput("note", TypeToken.of(String::class.java))
    private val read = mutableListOf<Any?>()

    @Test
    fun `a step reads its inputs from the data bag`() {
        ran(journeyOf(reading(order)).data("order", "42").create())
        assertEquals(listOf("42"), read)
    }

    @Test
    fun `an optional input with no value is absent, and optional_input_absent names the step`() {
        val log = Log()
        ran(journeyOf(reading(note)).reporter(log).create())
        assertEquals(listOf(null), read)
        val absent = log.events.filterIsInstance<OptionalInputAbsent>().single()
        assertEquals("note", absent.key)
        assertEquals(StepSource(ship, Attempt.of(1)), absent.requester)
    }

    @Test
    fun `a required input with no value aborts the journey before the step is built`() {
        val result = ran(journeyOf(reading(order)).create())
        assertEquals(
            RequiredDataMissing(ship, MissingData.Key("order", ByStep)),
            result.abort?.details,
        )
        assertEquals(emptyList<Any?>(), read)
    }

    @Test
    fun `an input of another type aborts the journey with wrong type, naming the step`() {
        val result = ran(journeyOf(reading(order)).data("order", 42L).create())
        assertEquals(WrongType(ship, WrongData.Key("order", ByStep), null), result.abort?.details)
    }

    @Test
    fun `an input adapter supplies the input, told the step and the key, and it is announced`() {
        val step = Requests.stepName()
        val key = Requests.key()
        val adapter = adapting { _, got -> Supplied.Value("${got[step]}/${got[key]}") }
        val log = Log()
        ran(adapted(adapter.request(step).request(key)).reporter(log).create())
        assertEquals(listOf("ship/order"), read)
        val supplied = log.events.filterIsInstance<InputAdapterSupplied>().single()
        assertEquals(
            listOf(ship, "order", pricing),
            listOf(supplied.step, supplied.key, supplied.adapter),
        )
    }

    @Test
    fun `an input adapter that leaves the input to the data bag reads it from there`() {
        val adapter = adapting { _, _ -> Supplied.FromDataBag }
        ran(adapted(adapter).data("order", "42").create())
        assertEquals(listOf("42"), read)
    }

    @Test
    fun `an input adapter reads the data bag through its access`() {
        val access = Requests.dataBagAccess()
        val price = TypeToken.of(Long::class.javaObjectType)
        val adapter = adapting { _, got ->
            Supplied.Value("${got[access].read("price", price).orNull()}")
        }
        ran(adapted(adapter.request(access)).data("price", 40L).create())
        assertEquals(listOf("40"), read)
    }

    @Test
    fun `an input adapter's access reads nothing once its call has returned`() {
        val access = Requests.dataBagAccess()
        val price = TypeToken.of(Long::class.javaObjectType)
        val kept = mutableListOf<DataBagAccess>()
        val adapter = adapting { _, got -> Supplied.FromDataBag.also { kept += got[access] } }
        ran(adapted(adapter.request(access)).data("order", "42").data("price", 40L).create())
        assertEquals(Read.Absent, kept.single().read("price", price))
    }

    @Test
    fun `an input adapter that throws fails, and the step could not be built`() {
        val error = IllegalStateException("no prices")
        val log = Log()
        val result = ran(adapted(adapting { _, _ -> throw error }).reporter(log).create())
        assertEquals(
            listOf("journey_started", "attempt_started", "input_adapter_failed", "journey_aborted"),
            log.kinds,
        )
        assertEquals(
            StepCouldNotBeBuilt(ship, ByAdapterForKey(pricing, "order"), "no prices"),
            result.abort?.details,
        )
        assertSame(error, result.abort?.error)
    }

    @Test
    fun `an input adapter that supplies what is not a value fails`() {
        val log = Log()
        val result = ran(adapted(adapting { _, _ -> Supplied.Value(Any()) }).reporter(log).create())
        assertEquals("input_adapter_failed", log.kinds[2])
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort?.reason)
    }

    @Test
    fun `an input adapter that returns null is an unexpected result`() {
        val result = ran(adapted(adapting { _, _ -> returned<Supplied>(null) }).create())
        assertEquals(
            UnexpectedResult(
                ship,
                ByAdapterForKey(pricing, "order"),
                "the input adapter returned null, not what it supplies",
            ),
            result.abort?.details,
        )
    }

    @Test
    fun `what an input adapter supplies of another type is wrong type, naming the adapter`() {
        val result = ran(adapted(adapting { _, _ -> Supplied.Value(42L) }).create())
        val wrong = result.abort?.details as WrongType
        assertEquals(ByAdapter(pricing), (wrong.wrong as WrongData.Key).requester)
    }

    @Test
    fun `data an input adapter requires from the workflow is read first, naming the adapter`() {
        val currency = Requests.fromWorkflow("currency", TypeToken.of(String::class.java))
        val called = mutableListOf<String>()
        val adapter = adapting { _, got ->
            Supplied.Value(got[currency]).also { called += "called" }
        }
        val result = ran(adapted(adapter.request(currency)).create())
        assertEquals(
            RequiredDataMissing(ship, MissingData.Key("currency", ByAdapter(pricing))),
            result.abort?.details,
        )
        assertEquals(emptyList<String>(), called)
    }

    @Test
    fun `optional data an input adapter finds absent names the adapter`() {
        val currency = Requests.optionalFromWorkflow("currency", TypeToken.of(String::class.java))
        val log = Log()
        ran(
            adapted(adapting { _, got -> Supplied.Value("${got[currency]}") }.request(currency))
                .reporter(log)
                .create()
        )
        assertEquals(
            AdapterSource(pricing, ship, Attempt.of(1)),
            log.events.filterIsInstance<OptionalInputAbsent>().single().requester,
        )
    }

    private fun reading(input: Request<StepKind<Sync>, *>): StepBuilder<Sync> =
        building("ship") { got ->
                read += got[input]
                SyncStepRun { Outcome.success() }
            }
            .request(input)

    private fun adapting(adapter: SyncInputAdapter<Shop>): InputAdapterBuilder<Shop, Sync> =
        InputAdapterDescriptor.builder("pricing", "ship", adapter)

    private fun adapted(adapter: InputAdapterBuilder<Shop, Sync>) =
        instanceOf(shop().step(reading(order).build()).inputAdapter(adapter.build()).build())

    private companion object {
        val ship: StepName = StepName.of("ship")
        val pricing: AdapterName = AdapterName.of("pricing")
    }
}

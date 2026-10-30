package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.samples.keys
import org.itineradev.itinera.journey.samples.reads
import org.itineradev.itinera.value.Rebuilds
import org.itineradev.itinera.value.Ticket
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class DataBagTest {
    private val amount = TypeToken.of(Long::class.java)

    @AfterEach
    fun rebuildAgain() {
        Rebuilds.fail = false
    }

    @Test
    fun `a value of the type read is present`() {
        assertEquals(Read.Present(40L), DataBags.of("amount", 40L).read("amount", amount))
    }

    @Test
    fun `a key that holds nothing is absent`() {
        assertEquals(Read.Absent, DataBags.of().read("amount", amount))
    }

    @Test
    fun `numbers do not widen`() {
        assertEquals(Read.OtherType, DataBags.of("amount", 40).read("amount", amount))
    }

    @ParameterizedTest
    @FieldSource("nullReads")
    fun `a stored null is of a type only when null is of it`(type: TypeToken<*>, read: Read<*>) {
        assertEquals(read, DataBags.of("note", null).read("note", type))
    }

    @ParameterizedTest
    @FieldSource("holdings")
    fun `a key holding null is held`(data: DataBag, held: Boolean) {
        assertEquals(held, "note" in data)
    }

    @Test
    fun `keys keep the order they were first set`() {
        val data = DataBags.of()
        data.put("b", DataBags.captured(1L))
        data.put("a", DataBags.captured(2L))
        data.put("b", DataBags.captured(3L))
        assertEquals(listOf("b", "a"), data.keys.toList())
        assertEquals(Read.Present(3L), data.read("b", amount))
    }

    @Test
    fun `a reader may change its copy without changing the data bag`() {
        val data = DataBags.of("items", listOf("book"))
        val items = TypeToken.of(MutableList::class.java)
        data.read("items", items).orNull()?.clear()
        assertEquals(Read.Present(listOf("book")), data.read("items", items))
    }

    @Test
    fun `a copy that throws while the data bag is read gives another type`() {
        val data = DataBags.of("ticket", Ticket("A1"))
        Rebuilds.fail = true
        assertEquals(Read.OtherType, data.read("ticket", TypeToken.of(Ticket::class.java)))
    }

    @ParameterizedTest
    @FieldSource("orNulls")
    fun `orNull gives the value only when one of the type was found`(
        read: Read<*>,
        value: Any?,
    ) {
        assertEquals(value, read.orNull())
    }

    @ParameterizedTest
    @FieldSource("readSamples")
    fun `the sample reads each kind of answer`(data: DataBag, answer: String) {
        assertEquals(answer, reads(data))
    }

    @ParameterizedTest
    @FieldSource("keySamples")
    fun `the sample lists the keys when an amount is held`(data: DataBag, answer: String) {
        assertEquals(answer, keys(data))
    }

    companion object {
        @JvmField
        val nullReads =
            listOf(
                argumentSet("refusing null", TypeToken.of(String::class.java), Read.OtherType),
                argumentSet(
                    "admitting null",
                    TypeToken.ofNullable(String::class.java),
                    Read.Present(null),
                ),
            )

        @JvmField
        val holdings =
            listOf(
                argumentSet("holding null", DataBags.of("note", null), true),
                argumentSet("holding nothing", DataBags.of(), false),
            )

        @JvmField
        val orNulls =
            listOf(
                argumentSet("present", Read.Present(40L), 40L),
                argumentSet("absent", Read.Absent, null),
                argumentSet("of another type", Read.OtherType, null),
            )

        @JvmField
        val readSamples =
            listOf(
                argumentSet("a long", DataBags.of("amount", 40L), "charging 40"),
                argumentSet("nothing", DataBags.of(), "nothing to charge"),
                argumentSet(
                    "an int",
                    DataBags.of("amount", 40),
                    "the amount is not a Long: was it written 40 instead of 40L?",
                ),
            )

        @JvmField
        val keySamples =
            listOf(
                argumentSet(
                    "an amount",
                    DataBags.of("amount", 40L),
                    "keys [amount] in {amount=40}",
                ),
                argumentSet("no amount", DataBags.of(), "no amount"),
            )
    }
}

package org.itineradev.itinera.value.kotlin

import org.itineradev.itinera.engine.NotAValue
import org.itineradev.itinera.engine.Received
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.engine.WrongType
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.journey.kotlin.read
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinDataClassesTest {
    @AfterEach
    fun rebuildAgain() {
        Rebuilds.fail = false
    }

    @Test
    fun `a data class is copied through its primary constructor`() {
        val parcel = Parcel(1200L, mutableListOf("book"))
        val copy = captured(parcel).value as Parcel
        parcel.items += "lamp"
        assertEquals(Parcel(1200L, mutableListOf("book")), copy)
    }

    @ParameterizedTest
    @FieldSource("dataObjects")
    fun `a data object holding only immutable values is shared`(value: Any) {
        assertSame(value, captured(value).value)
    }

    @Test
    fun `a pair is a data class`() {
        assertEquals(1L to "one", captured(1L to "one").value)
    }

    @ParameterizedTest
    @FieldSource("notValues")
    fun `a Kotlin class that is not data is not a value`(candidate: Any) {
        assertInstanceOf(NotAValue::class.java, Values.capture(candidate))
    }

    @Test
    fun `a copy that throws while a data class is captured makes it not a value, carrying the error`() {
        val ticket = Ticket("A1")
        Rebuilds.fail = true
        assertEquals("no more tickets", (Values.capture(ticket) as NotAValue).error?.message)
    }

    @Test
    fun `a copy that throws while a set is captured makes it not a value, carrying the error`() {
        assertEquals(
            "cannot hash vase",
            (Values.capture(setOf(Fragile("vase"))) as NotAValue).error?.message,
        )
    }

    @Test
    fun `a copy that throws while a data class is read makes it of the wrong type, carrying the error`() {
        val stored = captured(Ticket("A1"))
        Rebuilds.fail = true
        assertEquals(
            "no more tickets",
            (Values.receive(stored, typeToken<Ticket>()) as WrongType).error?.message,
        )
    }

    @Test
    fun `a copy that throws while the data bag is read gives another type`() {
        val data = bagOf("ticket", Ticket("A1"))
        Rebuilds.fail = true
        assertEquals(Read.OtherType, data.read<Ticket>("ticket"))
    }

    @Test
    fun `each receiver gets a data class of its own`() {
        val stored = captured(Parcel(1200L, mutableListOf("book")))
        (Values.receive(stored, typeToken<Parcel>()) as Received).value.items += "lamp"
        assertEquals(
            Parcel(1200L, mutableListOf("book")),
            (Values.receive(stored, typeToken<Parcel>()) as Received).value,
        )
    }

    @ParameterizedTest
    @FieldSource("ofTheType")
    fun `a data class is read as a type when its properties are of it`(
        value: Any,
        type: TypeToken<*>,
    ) {
        assertEquals(Read.Present(value), bagOf("key", value).read("key", type))
    }

    @ParameterizedTest
    @FieldSource("ofAnotherType")
    fun `a data class whose properties are of another type is of another type`(
        value: Any,
        type: TypeToken<*>,
    ) {
        assertEquals(Read.OtherType, bagOf("key", value).read("key", type))
    }

    companion object {
        @JvmField
        val dataObjects =
            listOf(
                argumentSet("holding nothing", Empty),
                argumentSet(
                    "holding a data class, a data object and a JDK enum constant",
                    Defaults,
                ),
            )

        @JvmField
        val notValues =
            listOf(
                argumentSet("a data class with a var property", Counter(1L)),
                argumentSet("a data class with a field of its own", Tagged("a")),
                argumentSet("a data class holding a value class", Order(OrderId("A1"))),
                argumentSet("a data object with a var property", Mutable),
                argumentSet("a data object holding a list", Registry),
                argumentSet("a data object holding what is not a value", Workshop),
                argumentSet("an ordinary class", Plain("a")),
                argumentSet("a value class", OrderId("A1")),
                argumentSet("a lambda", { 1 }),
                argumentSet("a function reference", ::bagOf),
            )

        @JvmField
        val ofTheType =
            listOf(
                argumentSet(
                    "a property marked nullable holding null",
                    Note("hi", null),
                    typeToken<Note>(),
                ),
                argumentSet("a generic property", Box(4L), typeToken<Box<Long>>()),
                argumentSet(
                    "a generic property holding null, bound to a nullable type",
                    Box<Long?>(null),
                    typeToken<Box<Long?>>(),
                ),
                argumentSet(
                    "a nullable generic property holding null",
                    Maybe<Long>(null),
                    typeToken<Maybe<Long>>(),
                ),
                argumentSet(
                    "a sealed interface's member holding its type argument",
                    Ok(4L),
                    typeToken<Outcome<Long>>(),
                ),
                argumentSet(
                    "a sealed interface's member not using its type argument",
                    Refused("no"),
                    typeToken<Outcome<Long>>(),
                ),
            )

        @JvmField
        val ofAnotherType =
            listOf(
                argumentSet(
                    "a generic property of another type",
                    Box(4L),
                    typeToken<Box<String>>(),
                ),
                argumentSet(
                    "a generic property holding null, bound to a type",
                    Box<Long?>(null),
                    typeToken<Box<Long>>(),
                ),
                argumentSet(
                    "a sealed interface's member holding another type",
                    Ok("four"),
                    typeToken<Outcome<Long>>(),
                ),
            )
    }
}

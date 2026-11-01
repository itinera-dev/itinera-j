package org.itineradev.itinera.value

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.NotAValue
import org.itineradev.itinera.engine.Values
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class CopyTest {
    @ParameterizedTest
    @FieldSource("values")
    fun `a value is captured as an equal copy`(value: Any?) {
        assertEquals(value, captured(value))
    }

    @ParameterizedTest
    @FieldSource("notValues")
    fun `what is not a value is refused when captured`(candidate: Any?) {
        assertInstanceOf(NotAValue::class.java, Values.capture(candidate))
    }

    @Test
    fun `a copy shares nothing that can change with the original`() {
        val parcel = Parcel(1200L, mutableListOf("book"))
        val copy = captured(parcel) as Parcel
        parcel.items += "lamp"
        assertEquals(listOf("book"), copy.items)
        assertNotSame(parcel.items, copy.items)
    }

    @Test
    fun `a list keeps its order and its nulls`() {
        assertEquals(listOf("b", null, "a"), captured(listOf("b", null, "a")))
    }

    @Test
    fun `a map keeps the order of its keys`() {
        val copy = captured(linkedMapOf("z" to 1L, "a" to 2L)) as Map<*, *>
        assertEquals(listOf("z", "a"), copy.keys.toList())
    }

    @Test
    fun `a container that holds itself is not a value`() {
        val items = mutableListOf<Any>()
        items.add(items)
        assertInstanceOf(NotAValue::class.java, Values.capture(items))
    }

    private fun captured(value: Any?): Any? = (Values.capture(value) as Captured).value

    companion object {
        @JvmField
        val values =
            listOf(
                argumentSet("null", null),
                argumentSet("a boolean", true),
                argumentSet("a string", "text"),
                argumentSet("a character", 'c'),
                argumentSet("a byte", 1.toByte()),
                argumentSet("a short", 2.toShort()),
                argumentSet("an int", 3),
                argumentSet("a long", 4L),
                argumentSet("a finite float", 1.5f),
                argumentSet("a finite double", 2.5),
                argumentSet("a big integer", BigInteger.TEN),
                argumentSet("a big decimal", BigDecimal("1.50")),
                argumentSet("a UUID", UUID(1L, 2L)),
                argumentSet("an instant", Instant.EPOCH),
                argumentSet("a date", LocalDate.of(2026, 10, 27)),
                argumentSet("a duration", Duration.ofMinutes(5)),
                argumentSet("a zone", ZoneId.of("Europe/Lisbon")),
                argumentSet("a zone offset", ZoneOffset.UTC),
                argumentSet("an enum constant", Size.LARGE),
                argumentSet("an enum constant holding immutable values", Coin.PENNY),
                argumentSet("an enum constant with a body of its own", Tone.LOUD),
                argumentSet("a JDK enum constant holding fields", ChronoUnit.DAYS),
                argumentSet("a JDK enum constant of java.util.concurrent", TimeUnit.SECONDS),
                argumentSet("a JDK enum constant of java.math", RoundingMode.HALF_UP),
                argumentSet("an enum constant of Kotlin's standard library", DurationUnit.SECONDS),
                argumentSet("a list", listOf(1L, "two", null)),
                argumentSet("a set", setOf("a", "b")),
                argumentSet("a map with string keys", mapOf("a" to 1L, "b" to listOf(2L))),
                argumentSet("a record", Parcel(1200L, mutableListOf("book"))),
            )

        @JvmField
        val notValues =
            listOf(
                argumentSet("not a number", Double.NaN),
                argumentSet("an infinity", Float.POSITIVE_INFINITY),
                argumentSet("an array", arrayOf("a")),
                argumentSet("a lambda", { 1 }),
                argumentSet("a thread", Thread.currentThread()),
                argumentSet("a map with a key that is not a string", mapOf(1 to "a")),
                argumentSet("an object of an ordinary class", Basket(listOf("a"))),
                argumentSet("a Kotlin data class without itinera-kotlin", Order(40L)),
                argumentSet("a list holding what is not a value", listOf(Basket(listOf()))),
                argumentSet("an enum constant with a field that can change", Dial.LOW),
                argumentSet("an enum constant holding a list", Shelf.TOP),
            )
    }
}

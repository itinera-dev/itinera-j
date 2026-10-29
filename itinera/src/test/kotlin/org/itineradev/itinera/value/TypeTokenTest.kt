package org.itineradev.itinera.value

import java.util.LinkedList
import org.itineradev.itinera.value.samples.genericTypeTokens
import org.itineradev.itinera.value.samples.typeTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class TypeTokenTest {
    @Test
    fun `a primitive class gives its wrapper's token`() {
        assertEquals(TypeToken.of(Long::class.javaObjectType), TypeToken.of(java.lang.Long.TYPE))
    }

    @Test
    fun `a token admitting null differs from one refusing it`() {
        assertNotEquals(TypeToken.of(String::class.java), TypeToken.ofNullable(String::class.java))
    }

    @ParameterizedTest
    @FieldSource("refused")
    fun `a token of a class no value can be is refused, saying why`(type: Class<*>, why: String) {
        val refusal = assertThrows<IllegalArgumentException> { TypeToken.of(type) }
        assertEquals(why, refusal.message)
    }

    @ParameterizedTest
    @FieldSource("accepted")
    fun `a token may name any class a value can be an instance of`(type: Class<*>) {
        assertDoesNotThrow { TypeToken.of(type) }
    }

    @Test
    fun `the samples run`() {
        typeTokens()
        genericTypeTokens()
    }

    companion object {
        @JvmField
        val refused =
            listOf(
                argumentSet(
                    "an array",
                    Array<String>::class.java,
                    "No value is an array, such as String[]: a value holds a list instead.",
                ),
                argumentSet(
                    "a linked list",
                    LinkedList::class.java,
                    "No value is a java.util.LinkedList: a value's lists, sets and maps are read as " +
                        "List, Set and Map, or as ArrayList, LinkedHashSet and LinkedHashMap.",
                ),
                argumentSet(
                    "an ordinary class",
                    Thread::class.java,
                    "No value is a java.lang.Thread: values are records, data classes, " +
                        "enum constants, lists, sets, maps with string keys, and the JDK's immutable values.",
                ),
                argumentSet(
                    "an enum with a field that can change",
                    Dial::class.java,
                    "No value is a org.itineradev.itinera.value.Dial: a record or an enum constant " +
                        "is a value only when its module opens its package to org.itineradev.itinera, " +
                        "and an enum constant only when its fields are final.",
                ),
                argumentSet(
                    "an abstract class with fields",
                    java.io.Reader::class.java,
                    "No value is a java.io.Reader: values are records, data classes, " +
                        "enum constants, lists, sets, maps with string keys, and the JDK's immutable values.",
                ),
                argumentSet(
                    "a Kotlin data class without itinera-kotlin",
                    Order::class.java,
                    "org.itineradev.itinera.value.Order is a Kotlin class, which itinera reads only with " +
                        "the artifact org.itinera-dev:itinera-kotlin: add it to the application's dependencies.",
                ),
            )

        @JvmField
        val accepted =
            listOf(
                argumentSet("any value", Any::class.java),
                argumentSet("a number", Number::class.java),
                argumentSet("a list", List::class.java),
                argumentSet("an array list", ArrayList::class.java),
                argumentSet("a linked hash map", LinkedHashMap::class.java),
                argumentSet("a record", Parcel::class.java),
                argumentSet("an enum", Size::class.java),
                argumentSet("a JDK enum holding fields", java.time.temporal.ChronoUnit::class.java),
                argumentSet("an interface", Comparable::class.java),
                argumentSet("a big decimal", java.math.BigDecimal::class.java),
            )
    }
}

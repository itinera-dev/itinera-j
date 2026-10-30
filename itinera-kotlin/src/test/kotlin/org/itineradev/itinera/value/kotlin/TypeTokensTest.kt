package org.itineradev.itinera.value.kotlin

import java.util.LinkedList
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.value.kotlin.samples.typeTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class TypeTokensTest {
    @ParameterizedTest
    @FieldSource("ofTheType")
    fun `a value is read as a Kotlin type it is of`(value: Any?, type: TypeToken<*>) {
        assertEquals(Read.Present(value), bagOf("key", value).read("key", type))
    }

    @ParameterizedTest
    @FieldSource("ofAnotherType")
    fun `a value is of another type than a Kotlin type it is not of`(
        value: Any?,
        type: TypeToken<*>,
    ) {
        assertEquals(Read.OtherType, bagOf("key", value).read("key", type))
    }

    @Test
    fun `the reified token equals the class's token`() {
        assertEquals(TypeToken.of(Long::class.java), typeToken<Long>())
    }

    @ParameterizedTest
    @FieldSource("refused")
    fun `a type no value can be is refused`(make: () -> TypeToken<*>) {
        assertThrows<IllegalArgumentException> { make() }
    }

    @Test
    fun `the sample runs`() {
        typeTokens()
    }

    companion object {
        @JvmField
        val ofTheType =
            listOf(
                argumentSet(
                    "a list holding null as a list of a nullable type",
                    listOf(1L, null),
                    typeToken<List<Long?>>(),
                ),
                argumentSet("null as a nullable type", null, typeToken<String?>()),
                argumentSet(
                    "a list of anything as a star projection",
                    listOf("a", 1L),
                    typeToken<List<*>>(),
                ),
                argumentSet(
                    "a list of anything as an in projection",
                    listOf("a", 1L),
                    typeToken<MutableList<in Long>>(),
                ),
            )

        @JvmField
        val ofAnotherType =
            listOf(
                argumentSet(
                    "a list holding null as a list of a type",
                    listOf(1L, null),
                    typeToken<List<Long>>(),
                ),
                argumentSet("null as a type", null, typeToken<String>()),
                argumentSet("an Int as a Long", 40, typeToken<Long>()),
            )

        @JvmField
        val refused =
            listOf(
                argumentSet("a linked list", { typeToken<LinkedList<Long>>() }),
                argumentSet("a map whose keys are not strings", { typeToken<Map<Int, Long>>() }),
                argumentSet("an array", { typeToken<Array<Long>>() }),
                argumentSet("an ordinary class", { typeToken<Plain>() }),
            )
    }
}

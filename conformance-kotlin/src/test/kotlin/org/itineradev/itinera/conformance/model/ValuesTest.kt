package org.itineradev.itinera.conformance.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class ValuesTest {
    @ParameterizedTest
    @FieldSource("written")
    fun `a value written as JSON becomes a Long, Double, String, Boolean, List or Map`(
        written: String,
        expected: Any?,
    ) {
        assertEquals(expected, value(written))
    }

    @Test
    fun `an object keeps its keys in the order written`() {
        assertEquals(listOf("b", "a"), (value("""{"b": 1, "a": 2}""") as Map<*, *>).keys.toList())
    }

    @Test
    fun `text that is not JSON is an error in the case`() {
        assertThrows<ModelException> { value("{") }
    }

    @ParameterizedTest
    @FieldSource("types")
    fun `each type of the cases has the class its values have`(
        written: String,
        expected: Class<*>,
    ) {
        assertEquals(expected, ValueType.named(written).type)
    }

    @Test
    fun `a type the cases do not name is an error in the case`() {
        assertThrows<ModelException> { ValueType.named("decimal") }
    }

    private companion object {
        @JvmStatic
        val written =
            listOf(
                argumentSet("an integer", "42", 42L),
                argumentSet("a number", "4.5", 4.5),
                argumentSet("a string", "\"eu\"", "eu"),
                argumentSet("a boolean", "true", true),
                argumentSet("null", "null", null),
                argumentSet("a list", "[1, \"a\"]", listOf(1L, "a")),
                argumentSet("an object", """{"amount": 10}""", mapOf("amount" to 10L)),
            )

        @JvmStatic
        val types =
            listOf(
                argumentSet("integer", "integer", Long::class.javaObjectType),
                argumentSet("number", "number", Double::class.javaObjectType),
                argumentSet("string", "string", String::class.java),
                argumentSet("boolean", "boolean", Boolean::class.javaObjectType),
                argumentSet("list", "list", List::class.java),
                argumentSet("object", "object", Map::class.java),
            )
    }
}

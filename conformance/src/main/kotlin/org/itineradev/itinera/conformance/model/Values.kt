package org.itineradev.itinera.conformance.model

import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

private val json = JsonMapper.builder().enable(DeserializationFeature.USE_LONG_FOR_INTS).build()

/**
 * The value a case writes as JSON: an integer becomes a `Long`, any other number a `Double`, an
 * object a `LinkedHashMap<String, Any?>` keeping its keys' order, an array a `List<Any?>`, and
 * `null` the value null.
 */
internal fun value(written: String): Any? =
    try {
        json.readValue(written, Any::class.java)
    } catch (error: JacksonException) {
        throw ModelException("$written is not JSON: ${error.originalMessage}", error)
    }

/** A type of the cases' neutral vocabulary, and the class a value of it has here. */
internal enum class ValueType(val written: String, val type: Class<*>) {
    INTEGER("integer", Long::class.javaObjectType),
    NUMBER("number", Double::class.javaObjectType),
    STRING("string", String::class.java),
    BOOLEAN("boolean", Boolean::class.javaObjectType),
    LIST("list", List::class.java),
    OBJECT("object", Map::class.java);

    companion object {
        private val byName = entries.associateBy(ValueType::written)

        /** The type a case names. */
        fun named(written: String): ValueType =
            byName[written] ?: throw ModelException("$written is not a type of the cases")
    }
}

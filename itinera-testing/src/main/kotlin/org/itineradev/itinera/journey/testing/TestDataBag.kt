package org.itineradev.itinera.journey.testing

import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.engine.NotAValue
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.journey.DataBagAccess

/**
 * A data bag built from values outside any journey, so that a test calls an input adapter itself
 * and lends it read access to the data bag, as a journey does. It is named apart from the journey's
 * own `DataBag`, which only itinera makes. Each [with] gives a new data bag, so one can be shared.
 *
 * @sample org.itineradev.itinera.journey.testing.samples.testDataBag
 */
public class TestDataBag private constructor(private val values: Map<String, Captured>) {
    /**
     * An empty data bag.
     *
     * @sample org.itineradev.itinera.journey.testing.samples.testDataBag
     */
    public constructor() : this(emptyMap())

    /**
     * A data bag holding what this one holds, and a captured copy of [value] under [key], replacing
     * what this one holds under it.
     *
     * @throws IllegalArgumentException when [value] is not a value.
     * @sample org.itineradev.itinera.journey.testing.samples.testDataBag
     */
    public fun with(key: String, value: Any?): TestDataBag =
        TestDataBag(values + (key to captured(key, value)))

    /**
     * Read access to the data bag, as an input adapter requests it. Each read is new access.
     *
     * @sample org.itineradev.itinera.journey.testing.samples.testDataBag
     */
    public val access: DataBagAccess
        get() = DataBags.access(DataBags.of(values))
}

private fun captured(key: String, value: Any?): Captured =
    when (val capture = Values.capture(value)) {
        is Captured -> capture
        is NotAValue ->
            throw IllegalArgumentException(
                "The value given under $key is not a value.",
                capture.error,
            )
    }

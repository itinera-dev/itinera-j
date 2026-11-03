package org.itineradev.itinera.journey.testing.samples

import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.testing.Contributions
import org.itineradev.itinera.journey.testing.TestDataBag
import org.itineradev.itinera.value.TypeToken

private val LONG = TypeToken.of(Long::class.javaObjectType)

fun contributions(): Set<String> {
    val contributions = Contributions()
    contributions.contributor.contribute("charged", 35L)
    contributions.contributor.contribute("charged", 40L)
    check(contributions.read("charged", LONG).orNull() == 40L)
    check(contributions.read("refunded", LONG).orNull() == null)
    return contributions.keys
}

/** The amount an input adapter reads from the data bag it is lent, or nothing. */
private fun amountOf(data: DataBagAccess): Long = data.read("amount", LONG).orNull() ?: 0L

fun testDataBag(): Long {
    val bag = TestDataBag().with("amount", 35L).with("currency", "EUR")
    check(amountOf(TestDataBag().access) == 0L)
    return amountOf(bag.access)
}

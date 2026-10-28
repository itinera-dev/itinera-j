package org.itineradev.itinera.request

import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync

/** What [RequestTypesTest] refuses, written so that it compiles. */
internal object RequestTypesTwins {
    val amount = Requests.input("amount", TypeToken.of(Long::class.javaObjectType))
    val discount = Requests.optionalInput("discount", TypeToken.of(Long::class.javaObjectType))

    fun read(got: Got<StepKind<Sync>>): Long = got[amount]

    fun readOptional(got: Got<StepKind<Sync>>): Long? = got[discount]
}

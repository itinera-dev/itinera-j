package org.itineradev.itinera.testing.samples

import org.itineradev.itinera.instance.testing.TestInstanceBuilder
import org.itineradev.itinera.instance.testing.samples.AcceptingGateway
import org.itineradev.itinera.instance.testing.samples.Gateway
import org.itineradev.itinera.instance.testing.samples.Payments
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.MistakeKind
import org.itineradev.itinera.testing.TestMistake

fun mistakes(): List<Mistake> {
    val builder = TestInstanceBuilder.of(Payments())
    val mistake =
        runCatching {
            builder.fakeProvider("courier", Gateway::class.java) {
                Provided.Instance(AcceptingGateway())
            }
        }
            .exceptionOrNull() as TestMistake
    check(mistake.message == "The test is mistaken: the workflow has no service provider courier.")
    check(mistake.problems.single().kind == MistakeKind.UNKNOWN_PROVIDER)
    return mistake.problems
}

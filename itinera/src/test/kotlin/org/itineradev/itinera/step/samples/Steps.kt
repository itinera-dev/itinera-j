@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason

fun attempts(attempt: Attempt): String =
    if (attempt.isFirst) "first try" else "try ${attempt.number}"

fun reasons(): Reason {
    val declined = Reason("declined", "The card was declined.", mapOf("retryAfter" to 30L))
    check(declined.code == Reason("declined").code)
    return declined
}

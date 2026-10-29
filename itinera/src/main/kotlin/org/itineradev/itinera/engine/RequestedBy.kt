package org.itineradev.itinera.engine

import org.itineradev.itinera.event.RequestSource
import org.itineradev.itinera.event.Requester
import org.itineradev.itinera.step.StepName

/**
 * Who requests data during [step], when there is one: the [requester] an abort names, and the
 * [source] `optional_input_absent` names.
 */
internal class RequestedBy(
    val step: StepName?,
    val requester: Requester,
    val source: RequestSource,
)

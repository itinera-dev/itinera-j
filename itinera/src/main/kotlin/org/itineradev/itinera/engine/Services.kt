package org.itineradev.itinera.engine

import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation

/**
 * The violations of the services [step] requests: each key it requests twice, then each key no
 * provider serves for it.
 */
internal fun serviceViolations(step: StepDescriptor<*>): List<Violation> {
    val keys = step.requests.map(::askedBy).filterIsInstance<ServiceAsked>().map(ServiceAsked::key)
    return shared(keys).map { key -> requestedTwice(step.name, key) } +
        keys.distinct().map { key -> notProvided(step.name, key) }
}

private fun requestedTwice(step: StepName, key: String): Violation =
    Violation.ServiceRequestedTwice(step, key)

private fun notProvided(step: StepName, key: String): Violation =
    Violation.ServiceNotProvided(step, key)

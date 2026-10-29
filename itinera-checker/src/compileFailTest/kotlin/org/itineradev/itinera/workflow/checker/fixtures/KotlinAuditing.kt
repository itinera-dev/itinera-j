package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess

/** A step policy written in Kotlin, which a Java policy extends. */
abstract class KotlinAuditing : ItineraStepPolicy {
    @OnStepSuccess fun charged(): OnSuccess = keepDefault()
}

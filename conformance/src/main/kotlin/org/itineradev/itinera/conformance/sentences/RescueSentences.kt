package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.RescuedStepRequest
import org.itineradev.itinera.conformance.model.ValueType

/** The sentences that script what a rescue step requests of the step it rescues. */
internal class RescueSentences(private val world: World) {
    @Given("step {string} requests rescued step data {string} of type {word}")
    fun requestsRescuedStepData(step: String, key: String, type: String) {
        requests(step, HookRequest.StepData(key, ValueType.named(type), optional = false))
    }

    @Given("step {string} requests optional rescued step data {string} of type {word}")
    fun requestsOptionalRescuedStepData(step: String, key: String, type: String) {
        requests(step, HookRequest.StepData(key, ValueType.named(type), optional = true))
    }

    @Given("step {string} requests the failure reason")
    fun requestsTheFailureReason(step: String) {
        requests(step, HookRequest.FailureReason(optional = false))
    }

    @Given("step {string} requests the error")
    fun requestsTheError(step: String) {
        requests(step, HookRequest.Error)
    }

    private fun requests(step: String, request: HookRequest) {
        val script = world.model.step(step)
        script.ofRescuedStep += RescuedStepRequest(request, script.inputs.size)
    }
}

package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World

/**
 * The sentences that declare the mode of a step, an input adapter or a hook, and which modes the
 * executor accepts.
 */
internal class ExecutorSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("step {string} is asynchronous")
    fun isAsynchronous(step: String) {
        model.step(step).isAsynchronous = true
    }

    @Given("the input adapter {string} is asynchronous")
    fun adapterIsAsynchronous(adapter: String) {
        model.workflow.adapter(adapter).isAsynchronous = true
    }

    @Given("the hook {string} of policy {string} is asynchronous")
    fun hookIsAsynchronous(hook: String, policy: String) {
        world.scriptOf(hook, policy).isAsynchronous = true
    }

    @Given("the executor accepts only synchronous steps, hooks and reporters")
    fun acceptsOnlySynchronous() {
        model.acceptOnlySynchronous()
    }
}

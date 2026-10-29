package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookReturn

/** The sentences that script how a hook ends. */
internal class HookEndingSentences(private val world: World) {
    @Given("the hook {string} of policy {string} returns FinishWorkflow")
    fun returnsFinishWorkflow(hook: String, policy: String) {
        world.ends(hook, policy, HookReturn.FinishWorkflow)
    }

    @Given("the hook {string} of policy {string} returns FailWorkflow with code {string}")
    fun returnsFailWorkflow(hook: String, policy: String, code: String) {
        world.ends(hook, policy, HookReturn.FailWorkflow(code))
    }

    @Given("the hook {string} of policy {string} throws")
    fun throws(hook: String, policy: String) {
        world.ends(hook, policy, HookReturn.Fails(null))
    }

    @Given("the hook {string} of policy {string} fails with {string}")
    fun failsWith(hook: String, policy: String, message: String) {
        world.ends(hook, policy, HookReturn.Fails(message))
    }

    @Given("the hook {string} of policy {string} returns a value")
    fun returnsAValue(hook: String, policy: String) {
        world.ends(hook, policy, HookReturn.Value)
    }
}

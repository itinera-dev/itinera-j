package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookAction
import org.itineradev.itinera.conformance.model.JourneyEmitted
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.value

/** The sentences that script what a hook does before it ends. */
internal class HookActionSentences(private val world: World) {
    @Given("""^the hook "([^"]*)" of policy "([^"]*)" contributes "([^"]*)" = (.+)$""")
    fun contributes(hook: String, policy: String, key: String, written: String) {
        world.does(hook, policy, HookAction.Contributes(key, value(written)))
    }

    @Given(
        "the hook {string} of policy {string} counts its calls and contributes the count as {string}"
    )
    fun countsItsCalls(hook: String, policy: String, key: String) {
        world.does(hook, policy, HookAction.CountsCalls(key))
    }

    @Given(
        """^the hook "([^"]*)" of policy "([^"]*)" changes the step data "([^"]*)" it received to (.+)$"""
    )
    fun changesStepData(hook: String, policy: String, key: String, written: String) {
        world.does(hook, policy, HookAction.ChangesStepData(key, value(written)))
    }

    @Given("the hook {string} of policy {string} emits {word} {string}")
    fun emits(hook: String, policy: String, kind: String, message: String) {
        world.does(hook, policy, HookAction.Emits(JourneyEmitted.named(kind), message))
    }

    @Given(
        """^the hook "([^"]*)" of policy "([^"]*)" sets the global metadata "([^"]*)" to (.+)$"""
    )
    fun setsGlobalMetadata(hook: String, policy: String, key: String, written: String) {
        world.does(hook, policy, HookAction.SetsGlobalMetadata(key, value(written)))
    }

    @Given(
        """^the hook "([^"]*)" of policy "([^"]*)" uses the kept contributor and step reporter of """ +
            """step "([^"]*)" to contribute "([^"]*)" = (.+) and emit step_info "([^"]*)"$"""
    )
    fun usesKeptHandles(
        hook: String,
        policy: String,
        step: String,
        key: String,
        written: String,
        message: String,
    ) {
        if (!world.model.step(step).keepsHandles) {
            throw ModelException("\"$step\" does not keep its contributor and step reporter")
        }
        world.does(hook, policy, HookAction.UsesKeptHandles(step, key, value(written), message))
    }
}

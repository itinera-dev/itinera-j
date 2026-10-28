package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.NotAValue

/**
 * The sentences that script what a step does on every attempt: it contributes, and changes inputs.
 */
internal class StepActionSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("""^step "([^"]*)" contributes "([^"]*)" = (?!.* and then changes it to )(.+)$""")
    fun contributes(step: String, key: String, written: String) {
        model.step(step).contributions += key to value(written)
    }

    @Given("step {string} contributes a non-value as {string}")
    fun contributesANonValue(step: String, key: String) {
        model.step(step).contributions += key to NotAValue
    }

    @Given("""^step "([^"]*)" changes its input "([^"]*)" to (.+)$""")
    fun changesItsInput(step: String, key: String, written: String) {
        model.step(step).inputChanges[key] = value(written)
    }
}

package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.When
import org.itineradev.itinera.conformance.World

/** The sentences that admit the workflow and run its journeys. */
internal class ActionSentences(private val world: World) {
    @When("the workflow is admitted")
    fun isAdmitted() {
        world.admit()
    }

    @When("the workflow runs")
    fun runs() {
        world.run(1)
    }

    @When("the same executor runs the workflow twice, with a new instance each time")
    fun runsTwice() {
        world.run(2)
    }
}

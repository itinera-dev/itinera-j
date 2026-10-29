package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.FakeModel
import org.itineradev.itinera.conformance.model.Implementation
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.testing.Mistake

/**
 * The sentences of test support's recording reporter, its fakes, and the mistakes it stops a test
 * for.
 */
internal class TestSupportSentences(private val world: World) {
    @Given("the instance has the recording reporter of test support")
    fun hasTheRecordingReporter() {
        world.model.testing.record()
    }

    @Then("the path is:")
    fun pathIs(table: DataTable) {
        val path = world.tested.kept.path
        val expected = table.asMaps().map { row -> cell(row, "line") }
        holds(path == expected, "the path $expected", "$path")
    }

    @Then("the path is empty")
    fun pathIsEmpty() {
        val path = world.tested.kept.path
        holds(path.isEmpty(), "an empty path", "$path")
    }

    @Then("the recording reporter of test support kept the same events as the reporter {string}")
    fun keptTheSameEventsAs(reporter: String) {
        val kept = world.tested.kept.events
        val received =
            world.recorders.made(reporter).lastOrNull()?.events
                ?: throw ModelException("no reporter \"$reporter\" was made")
        holds(kept == received, "the events of \"$reporter\", $received", "$kept")
    }

    @Then("the recording reporter of test support kept the global metadata:")
    fun keptTheGlobalMetadata(table: DataTable) {
        val kept = world.tested.kept.globalMetadata.toList()
        val expected = table.asMaps().map(::entryOf)
        holds(kept == expected, "the global metadata $expected", "$kept")
    }

    @Given(
        "the test replaces the service provider {string} with a fake that makes instances of {implementation}"
    )
    fun fakeMakes(provider: String, implementation: Implementation) {
        world.model.testing.fake(provider, FakeModel.Makes(implementation, null))
    }

    @Given(
        """^the test replaces the service provider "([^"]*)" with a fake that makes instances of """ +
            """(first labeller|second labeller) and supplies the data (.+)$"""
    )
    fun fakeMakesWithData(provider: String, implementation: String, data: String) {
        world.model.testing.fake(
            provider,
            FakeModel.Makes(Implementation.named(implementation), value(data)),
        )
    }

    @Given("the test replaces the service provider {string} with a fake that fails with {string}")
    fun fakeFails(provider: String, message: String) {
        world.model.testing.fake(provider, FakeModel.Fails(message))
    }

    @Given("the test replaces the service provider {string} with a fake that makes meters")
    fun fakeMakesMeters(provider: String) {
        world.model.testing.fake(provider, FakeModel.MakesMeters)
    }

    @Then("the fake of {string} was called {int} times")
    fun fakeWasCalled(provider: String, times: Int) {
        val calls =
            world.tested.fakes[provider]?.calls
                ?: throw ModelException("no fake replaces the provider \"$provider\"")
        holds(calls == times, "the fake of \"$provider\" called $times times", "$calls calls")
    }

    @Then("the test stopped before the journey, naming:")
    fun stoppedBeforeTheJourney(table: DataTable) {
        val ended = world.journeys.lastOrNull()?.ended
        val mistake =
            (ended as? Ended.Stopped)?.mistake
                ?: throw Unmet("expected the test to stop before the journey; it ended $ended")
        val found = mistake.problems.map(::namedBy).sortedBy(Named::toString)
        val expected = table.asMaps().map(::namedIn).sortedBy(Named::toString)
        holds(found == expected, "the mistakes $expected", "${mistake.message}")
    }
}

/** What a row of mistakes names: the mistake, and the step, attempt and provider it names. */
private data class Named(
    val mistake: String,
    val step: String? = null,
    val attempt: String? = null,
    val provider: String? = null,
)

private fun namedIn(row: Map<String?, String?>): Named =
    Named(cell(row, "mistake"), row["step"], row["attempt"], row["provider"])

/** What [mistake] names, its kind as test support words it. */
private fun namedBy(mistake: Mistake): Named {
    val kind = mistake.kind.toString()
    return when (mistake) {
        is Mistake.UnknownStep -> Named(kind, step = mistake.step)
        is Mistake.StepScriptedTwice -> Named(kind, step = mistake.step)
        is Mistake.GapInTheAttempts -> Named(kind, mistake.step, "${mistake.attempt}")
        is Mistake.AttemptScriptedTwice -> Named(kind, mistake.step, "${mistake.attempt}")
        is Mistake.NoSuchAttempt -> Named(kind, mistake.step, "${mistake.attempt}")
        is Mistake.RescueStepSkips -> Named(kind, step = mistake.step)
        is Mistake.NoContributor -> Named(kind, step = mistake.step)
        is Mistake.NoReporter -> Named(kind, step = mistake.step)
        is Mistake.AttemptLeftOut -> Named(kind, mistake.step, "${mistake.attempt}")
        is Mistake.RescueStepLeftOut -> Named(kind, step = mistake.step)
        is Mistake.StepLeftOut -> Named(kind, step = mistake.step)
        is Mistake.UnknownProvider -> Named(kind, provider = mistake.provider)
        is Mistake.AnotherService -> Named(kind, provider = mistake.provider)
    }
}

/** The key and value a row gives, its value written as JSON. */
internal fun entryOf(row: Map<String?, String?>): Pair<String, Any?> =
    cell(row, "key") to value(cell(row, "value"))

package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ReporterFailure
import org.itineradev.itinera.event.EventKind

/**
 * The sentences that declare the workflow's reporters, what the executor is given to dispatch
 * events, and how scripted reporters fail.
 */
internal class ReporterSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("""^the workflow lists the reporters ("[^"]*"(?:, "[^"]*")*)$""")
    fun listsTheReporters(reporters: String) {
        model.listReporters(names(reporters))
    }

    @Given("the executor uses its default dispatcher")
    fun usesItsDefaultDispatcher() {
        model.dispatchWith(Dispatching.Default)
    }

    @Given("the executor is given a dispatcher factory that fails")
    fun givenAFactoryThatFails() {
        model.dispatchWith(Dispatching.FactoryFails)
    }

    @Given("the executor is given a dispatcher holding the reporter {string}")
    fun givenADispatcherHolding(reporter: String) {
        model.dispatchWith(Dispatching.Holding(reporter, DispatcherScript.Delivers))
    }

    @Given(
        "the executor is given a dispatcher holding the reporter {string} that ignores added reporters"
    )
    fun thatIgnoresAddedReporters(reporter: String) {
        model.dispatchWith(Dispatching.Holding(reporter, DispatcherScript.IgnoresAddedReporters))
    }

    @Given(
        "the executor is given a dispatcher holding the reporter {string} that throws when a reporter is added"
    )
    fun thatThrowsWhenAReporterIsAdded(reporter: String) {
        model.dispatchWith(Dispatching.Holding(reporter, DispatcherScript.ThrowsWhenAdding))
    }

    @Given(
        "the executor is given a dispatcher holding the reporter {string} that throws when dispatching {string}"
    )
    fun thatThrowsWhenDispatching(reporter: String, event: String) {
        val script = DispatcherScript.ThrowsWhenDispatching(kind(event))
        model.dispatchWith(Dispatching.Holding(reporter, script))
    }

    @Given("the reporter {string} throws")
    fun throwsOnTheFirstEvent(reporter: String) {
        model.fails(reporter, ReporterFailure.OnFirstEvent)
    }

    @Given("the reporter {string} throws on {string}")
    fun throwsOn(reporter: String, event: String) {
        model.fails(reporter, ReporterFailure.OnEvent(kind(event)))
    }

    @Given("the reporter {string} fails with {string} on {string}")
    fun failsWithOn(reporter: String, message: String, event: String) {
        model.fails(reporter, ReporterFailure.OnEvent(kind(event), message))
    }

    @Given("the reporter {string} fails when it receives the global metadata {string}")
    fun failsOnGlobalMetadata(reporter: String, key: String) {
        model.fails(reporter, ReporterFailure.OnGlobalMetadata(key))
    }
}

/** The kind of event a case names, by its snake_case name, such as `journey_started`. */
private fun kind(written: String): EventKind =
    EventKind.parse(written) ?: throw ModelException("$written is not a kind of event")

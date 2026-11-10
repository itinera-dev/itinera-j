package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.trace.Status

private val endedColumns = setOf("step", "status", "attempts")

private val servicesColumns = setOf("step", "service", "label", "attempt")

private val builtColumns = setOf("step", "attempt", "input", "value")

private val reportersColumns = setOf("reporter", "event", "received")

/**
 * The tables that check how the steps ended, what they were built with and given, and what the
 * reporters received, each row standing for the sentence of its kind.
 */
internal class TableCheckSentences(
    private val steps: StepSentences,
    private val built: BuiltSentences,
    private val received: ReceivedSentences,
) {
    @Then("the steps ended as:")
    fun theStepsEndedAs(table: DataTable) {
        table.asMaps().forEach(::ended)
    }

    @Then("the steps were given the services:")
    fun theStepsWereGivenTheServices(table: DataTable) {
        table.asMaps().forEach(::given)
    }

    @Then("the steps were built with:")
    fun theStepsWereBuiltWith(table: DataTable) {
        table.asMaps().forEach(::builtWith)
    }

    @Then("the reporters received:")
    fun theReportersReceived(table: DataTable) {
        table.asMaps().forEach(::reporterReceived)
    }

    private fun ended(row: Map<String?, String?>) {
        only(endedColumns, row)
        val step = cell(row, "step")
        val status = Status.named(cell(row, "status"))
        val attempts = row["attempts"]
        when {
            attempts != null -> steps.endedAsAfter(step, status, count(attempts))
            status == Status.ABORTED -> steps.endedAsAborted(step)
            else ->
                throw ModelException(
                    "the step \"$step\" ended as ${status.written} with no attempts"
                )
        }
    }

    private fun given(row: Map<String?, String?>) {
        only(servicesColumns, row)
        built.wasGivenTheService(
            cell(row, "step"),
            cell(row, "service"),
            cell(row, "label"),
            count(cell(row, "attempt")),
        )
    }

    private fun builtWith(row: Map<String?, String?>) {
        only(builtColumns, row)
        val step = cell(row, "step")
        val input = cell(row, "input")
        val value = cell(row, "value")
        val attempt = row["attempt"]
        when {
            value == "absent" && attempt == null -> built.wasBuiltWithInputAbsent(step, input)
            value == "absent" ->
                throw ModelException("an input absent is checked for attempt 1 only")
            attempt == null -> built.wasBuiltWithInput(step, input, value)
            else -> built.wasBuiltForAttemptWithInput(step, count(attempt), input, value)
        }
    }

    private fun reporterReceived(row: Map<String?, String?>) {
        only(reportersColumns, row)
        val reporter = cell(row, "reporter")
        val event = cell(row, "event")
        when (val written = cell(row, "received")) {
            "yes" -> received.receivedTheEvent(reporter, event)
            "no" -> received.didNotReceive(reporter, event)
            else -> received.receivedTheEventTimes(reporter, event, count(written))
        }
    }
}
/** A cell holding a whole number. */

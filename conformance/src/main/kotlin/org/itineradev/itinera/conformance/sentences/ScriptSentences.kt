package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Emitted
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptRow
import org.itineradev.itinera.conformance.model.ScriptedEnding
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.executor.testing.AttemptRecord
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.value.TypeToken

/** The sentences of scripted runs: the script, the run, and the run's record. */
internal class ScriptSentences(private val world: World) {
    @Given("the script says:")
    fun scriptSays(table: DataTable) {
        world.model.testing.script(table.asMaps().map(::rowOf))
    }

    @Given("the script ensures every step is scripted")
    fun ensuresEveryStep() {
        world.model.testing.ensureAllScripted()
    }

    @Given("step {string} declares no contributor and no reporter")
    fun declaresNoHandles(step: String) {
        world.model.step(step).declaresNoHandles = true
    }

    @When("the workflow runs with the script")
    fun runsWithTheScript() {
        world.runScripted()
    }

    @Then("""^the scripted run's record of step "([^"]*)" holds the attempts (\d+(?:, \d+)*)$""")
    fun recordHolds(step: String, attempts: String) {
        val held = attemptsOf(step)
        val expected = attempts.split(", ").map(String::toInt)
        holds(held == expected, "the attempts $expected of \"$step\"", "$held")
    }

    @Then("the scripted run's record of step {string} holds no attempt")
    fun recordHoldsNone(step: String) {
        val held = attemptsOf(step)
        holds(held.isEmpty(), "no attempt of \"$step\"", "$held")
    }

    @Then(
        "reading the scripted run's record of step {string} stops the test as the mistake {string}"
    )
    fun readingStops(step: String, mistake: String) {
        val stopped = runCatching { world.scriptedRun.step(step) }.exceptionOrNull()
        val problems = (stopped as? TestMistake)?.problems
        holds(
            problems?.singleOrNull()?.let { problem -> isNaming(problem, mistake, step) } == true,
            "reading \"$step\" to stop the test as \"$mistake\"",
            "$stopped",
        )
    }

    @Then(
        """^attempt (-?\d+) of step "([^"]*)" in the scripted run's record read the """ +
            """(?:input|rescued step data) "([^"]*)" = (.+)$"""
    )
    fun attemptRead(attempt: Int, step: String, key: String, written: String) {
        val record =
            world.scriptedRun.step(step).attempt(attempt)
                ?: throw Unmet(
                    "expected attempt $attempt of \"$step\" in the record; it holds none"
                )
        val read = record.input(key, ANY).orNull()
        holds(read == value(written), "\"$key\" = $written", "$read")
    }

    @Then(
        """^reading attempt (-?\d+) of step "([^"]*)" in the scripted run's record reads nothing$"""
    )
    fun readingReadsNothing(attempt: Int, step: String) {
        val record = world.scriptedRun.step(step).attempt(attempt)
        holds(record == null, "no attempt $attempt of \"$step\"", "$record")
    }

    private fun attemptsOf(step: String): List<Int> =
        world.scriptedRun.step(step).attempts.map(::numberOf)
}

private val ANY = TypeToken.of(Any::class.java)

private fun numberOf(record: AttemptRecord): Int = record.attempt.number

private fun isNaming(problem: Mistake, mistake: String, step: String): Boolean =
    "${problem.kind}" == mistake && problem is Mistake.UnknownStep && problem.step == step

private fun rowOf(row: Map<String?, String?>): ScriptRow =
    ScriptRow(
        step = cell(row, "step"),
        attempt = row["attempt"],
        contributes = objectOf(row["contributes"]),
        emits = row["emits"]?.let(Emitted::named),
        emitMessage = row["emit message"],
        setsGlobalMetadata = objectOf(row["sets global metadata"]),
        ending = ScriptedEnding.named(cell(row, "ending")),
        code = row["code"],
        message = row["message"],
    )

private fun objectOf(written: String?): Map<String, Any?> =
    written?.let(::value)?.let(::entriesOf).orEmpty()

private fun entriesOf(written: Any?): Map<String, Any?> =
    (written as? Map<*, *>)?.mapKeys(::keyOf) ?: throw ModelException("$written is not an object")

private fun keyOf(entry: Map.Entry<Any?, Any?>): String = entry.key as String

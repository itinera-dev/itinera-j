package org.itineradev.itinera.conformance

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class RunTest {
    @TempDir lateinit var directory: Path

    private val report
        get() = directory.resolve("reports/cucumber.json")

    private fun runs(tags: String): Boolean = run(Environment(cases, tags, report))

    private fun scenarios(): List<String> = features().flatMap(::scenariosOf)

    private fun errors(): List<String> =
        features().flatMap(::errorsOf).ifEmpty { listOf("no error") }

    private fun features(): List<JsonNode> =
        JsonMapper.builder().build().readTree(report.toFile()).values().toList()

    @Test
    fun `every selected scenario runs once in each column that accepts it`() {
        assertTrue(runs("@declaring"))
        assertEquals(
            listOf(
                "A workflow of one step that succeeds @executor-local",
                "A scenario that needs synchronous steps @executor-local",
                "Every sentence that declares a workflow @executor-local",
                "Every sentence that declares the instance @executor-local",
                "A workflow with no ID generator and no initial data @executor-local",
                "Every sentence that declares reporters @executor-local",
                "A dispatcher that ignores added reporters @executor-local",
                "A dispatcher that throws when a reporter is added @executor-local",
                "A dispatcher that throws on one kind of event @executor-local",
                "The executor's own dispatchers @executor-local",
                "A dispatcher factory that fails @executor-local",
                "A workflow of one step that succeeds @executor-async",
                "A scenario that needs asynchronous steps @executor-async",
                "Every sentence that declares a workflow @executor-async",
                "Every sentence that declares the instance @executor-async",
                "A workflow with no ID generator and no initial data @executor-async",
                "Every sentence that declares reporters @executor-async",
                "A dispatcher that ignores added reporters @executor-async",
                "A dispatcher that throws when a reporter is added @executor-async",
                "A dispatcher that throws on one kind of event @executor-async",
                "The executor's own dispatchers @executor-async",
                "A dispatcher factory that fails @executor-async",
            ),
            scenarios(),
        )
    }

    @Test
    fun `a scenario that uses a sentence the runner does not define fails`() {
        assertFalse(runs("@undefined"))
        assertEquals(
            listOf(
                "A sentence the runner does not define @executor-local",
                "A sentence the runner does not define @executor-async",
            ),
            scenarios(),
        )
    }

    @Test
    fun `every scenario that runs journeys passes in both columns`() {
        assertTrue(runs("@journeys"))
        assertEquals(24, scenarios().size)
    }

    @Test
    fun `every scenario that declares adapters, services and providers or lists passes in both columns`() {
        assertTrue(runs("@admission and not @not-run-yet"))
        assertEquals(8, scenarios().size)
    }

    @Test
    fun `every scenario that builds steps from inputs and services passes in both columns`() {
        assertTrue(runs("@building"), errors().toString())
        assertEquals(18, scenarios().size)
    }

    @Test
    fun `every scenario whose steps fail, skip and contribute passes in both columns`() {
        assertTrue(runs("@outcomes"), errors().toString())
        assertEquals(12, scenarios().size)
    }

    @Test
    fun `every scenario whose steps emit and set global metadata passes in both columns`() {
        assertTrue(runs("@reporting"), errors().toString())
        assertEquals(6, scenarios().size)
    }

    @Test
    fun `a journey that did not do what the case expects fails as unmet`() {
        assertFalse(runs("@unmet"))
        assertTrue(errors().all { "Unmet" in it }, errors().toString())
    }

    @Test
    fun `a script the runner cannot run yet fails saying so`() {
        assertFalse(runs("@not-run-yet"))
        assertEquals(
            listOf(
                "the runner cannot run rescue steps yet",
                "the runner cannot run rescue steps yet",
            ),
            errors().map(::notRunYet),
            errors().toString(),
        )
    }

    @Test
    fun `a scenario whose sentences do not make a model fails`() {
        assertFalse(runs("@wrong-case"))
    }

    @Test
    fun `selecting no scenario passes with an empty report`() {
        assertTrue(runs("@proposal-0000"))
        assertEquals(emptyList<String>(), scenarios())
    }

    private companion object {
        val cases: Path = Path.of("src/test/resources/features")
    }
}

/** Each scenario of a feature in the report, written as its name and its column's tag. */
private fun scenariosOf(feature: JsonNode): List<String> =
    feature.path("elements").values().map(::nameAndColumn)

private fun nameAndColumn(scenario: JsonNode): String =
    "${scenario.path("name").asString()} ${scenario.path("tags").last().path("name").asString()}"

/** What the runner said it cannot run yet, from an error's first line. */
private fun notRunYet(error: String): String = error.lineSequence().first().substringAfter(": ")

/** The error of each failed step of a feature in the report. */
private fun errorsOf(feature: JsonNode): List<String> =
    feature.path("elements").values().flatMap(::stepsOf).mapNotNull(::errorOf)

private fun stepsOf(scenario: JsonNode): Iterable<JsonNode> = scenario.path("steps").values()

private fun errorOf(step: JsonNode): String? =
    step.path("result").path("error_message").asString().ifEmpty { null }

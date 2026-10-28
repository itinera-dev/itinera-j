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

    private fun scenarios(): List<String> =
        JsonMapper.builder().build().readTree(report.toFile()).values().flatMap(::scenariosOf)

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

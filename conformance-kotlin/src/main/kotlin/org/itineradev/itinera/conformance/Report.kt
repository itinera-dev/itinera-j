package org.itineradev.itinera.conformance

import java.nio.file.Path
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

private val json = JsonMapper.builder().build()

/**
 * The Cucumber JSON report of a run: each column's report merged into one, every scenario tagged
 * with its column. The features keep the order in which they first ran, and each feature lists the
 * first column's scenarios before the second's.
 *
 * A scenario that ran in both columns appears twice, as the two rows of an Examples table would,
 * since the report tells scenarios apart by name and line: the first copy keeps the scenario's
 * line, and the second is moved past every scenario of the feature sharing its name, such as the
 * rows of a Scenario Outline.
 */
internal fun merged(reports: Map<Column, Path>): JsonNode =
    json
        .createArrayNode()
        .addAll(reports.flatMap(::featuresOf).groupBy(FeatureRun::uri).values.map(::mergedFeature))

/** Writes a report, replacing what [file] held. */
internal fun write(report: JsonNode, file: Path) {
    json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), report)
}

/** One feature as one column's run reported it. */
private class FeatureRun(val column: Column, val feature: JsonNode) {
    val uri: String = feature.path("uri").asString()

    fun scenarios(): List<ScenarioRun> = feature.path("elements").values().map(::run)

    private fun run(element: JsonNode): ScenarioRun = ScenarioRun(column, element)
}

/** One scenario as one column's run reported it. */
private class ScenarioRun(val column: Column, val element: JsonNode) {
    val name: String = element.path("name").asString()
    val line: Int = element.path("line").asInt()

    fun isNamed(other: String): Boolean = name == other

    /** Whether this is the same scenario, run in a column before [other]'s. */
    fun isEarlierCopyOf(other: ScenarioRun): Boolean =
        column < other.column && name == other.name && line == other.line
}

/** The scenarios of one feature from every column, each copy given its column's tag and line. */
private class Copies(private val scenarios: List<ScenarioRun>) {
    fun of(scenario: ScenarioRun): ObjectNode {
        val row = scenarios.count(scenario::follows)
        return (scenario.element.deepCopy() as ObjectNode).apply {
            withArray("tags").addObject().put("name", scenario.column.tag)
            put("line", scenario.line + row * span(scenario.name))
        }
    }

    /** The number of lines the scenarios with this name cover, from the first to the last. */
    private fun span(name: String): Int {
        val lines = scenarios.filter { it.isNamed(name) }.map(ScenarioRun::line)
        return lines.max() - lines.min() + 1
    }
}

private fun ScenarioRun.follows(earlier: ScenarioRun): Boolean = earlier.isEarlierCopyOf(this)

private fun featuresOf(report: Map.Entry<Column, Path>): List<FeatureRun> =
    json.readTree(report.value.toFile()).values().map { FeatureRun(report.key, it) }

/** A feature that ran in one or more columns, with each column's scenarios in turn. */
private fun mergedFeature(runs: List<FeatureRun>): ObjectNode {
    val scenarios = runs.flatMap(FeatureRun::scenarios)
    val elements = json.createArrayNode().addAll(scenarios.map(Copies(scenarios)::of))
    return (runs.first().feature.deepCopy() as ObjectNode).apply { set("elements", elements) }
}

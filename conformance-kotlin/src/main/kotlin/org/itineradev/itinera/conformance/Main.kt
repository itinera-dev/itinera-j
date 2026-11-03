package org.itineradev.itinera.conformance

import io.cucumber.core.cli.Main as Cucumber
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/** The package of the sentences, which both columns share. */
private const val SENTENCES = "org.itineradev.itinera.conformance.sentences"

/** Runs the selected scenarios in both columns and writes the report, failing when any failed. */
fun main() {
    val environment =
        try {
            Environment.fromProcess()
        } catch (error: EnvironmentException) {
            System.err.println(error.message)
            exitProcess(2)
        }
    if (!run(environment)) exitProcess(1)
}

/**
 * Runs the scenarios [environment] selects, once in each column that accepts them, and writes the
 * merged report, telling whether every scenario passed. A scenario that uses a sentence the runner
 * does not define fails.
 */
internal fun run(environment: Environment): Boolean {
    val directory = Files.createTempDirectory("cucumber")
    try {
        val reports = Column.entries.associateWith { reportOf(it, directory) }
        val passed =
            reports.map { (column, report) -> runs(column, environment, report) }.all { it }
        Files.createDirectories(environment.report.toAbsolutePath().parent)
        write(merged(reports), environment.report)
        return passed
    } finally {
        directory.toFile().deleteRecursively()
    }
}

/** Where a column's own run writes its report, before the two are merged. */
private fun reportOf(column: Column, directory: Path): Path =
    directory.resolve("${column.name}.json")

private fun runs(column: Column, environment: Environment, report: Path): Boolean {
    val arguments =
        arrayOf(
            "--glue",
            SENTENCES,
            "--glue",
            column.glue,
            "--tags",
            column.tags(environment.tags),
            "--plugin",
            "json:$report",
            "--plugin",
            "summary",
            "--monochrome",
            environment.cases.toString(),
        )
    return Cucumber.run(arguments, Thread.currentThread().contextClassLoader) == 0.toByte()
}

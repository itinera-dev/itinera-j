package org.itineradev.itinera.conformance

import io.cucumber.tagexpressions.TagExpressionException
import io.cucumber.tagexpressions.TagExpressionParser
import java.nio.file.Path

private const val CASES = "ITINERA_CONFORMANCE_CASES"
private const val TAGS = "ITINERA_CONFORMANCE_TAGS"
private const val REPORT = "ITINERA_CONFORMANCE_REPORT"

/** What one run of the runner reads from its environment: what to run, and where to report. */
internal data class Environment(
    /** The directory holding the feature files, searched recursively. */
    val cases: Path,
    /** The Cucumber tag expression selecting the scenarios that run. */
    val tags: String,
    /** The file the Cucumber JSON report is written to, replacing what it held. */
    val report: Path,
) {
    companion object {
        /** Reads the three variables, each required, from [variable]. */
        fun read(variable: (String) -> String?): Environment {
            val tags = required(variable, TAGS)
            try {
                TagExpressionParser.parse(tags)
            } catch (error: TagExpressionException) {
                throw EnvironmentException("$TAGS is not a tag expression: $tags", error)
            }
            return Environment(
                Path.of(required(variable, CASES)),
                tags,
                Path.of(required(variable, REPORT)),
            )
        }

        /** Reads the process's environment. */
        fun fromProcess(): Environment = read(System::getenv)
    }
}

/** The value of a variable that must be set; an empty one counts as missing. */
private fun required(variable: (String) -> String?, name: String): String =
    variable(name)?.takeIf(String::isNotEmpty) ?: throw EnvironmentException("$name is not set")

/** The environment does not say what to run. */
internal class EnvironmentException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

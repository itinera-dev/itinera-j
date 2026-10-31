package org.itineradev.itinera.conformance;

import io.cucumber.tagexpressions.TagExpressionException;
import io.cucumber.tagexpressions.TagExpressionParser;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * What one run of the runner reads from its environment: what to run, and where to report.
 *
 * @param cases the directory holding the feature files, searched recursively
 * @param tags the Cucumber tag expression selecting the scenarios that run
 * @param report the file the Cucumber JSON report is written to, replacing what it held
 */
record Environment(Path cases, String tags, Path report) {
  private static final String CASES = "ITINERA_CONFORMANCE_CASES";
  private static final String TAGS = "ITINERA_CONFORMANCE_TAGS";
  private static final String REPORT = "ITINERA_CONFORMANCE_REPORT";

  /** Reads the three variables, each required, from {@code variable}. */
  static Environment read(Function<String, String> variable) throws EnvironmentException {
    var tags = required(variable, TAGS);
    try {
      TagExpressionParser.parse(tags);
    } catch (TagExpressionException error) {
      throw new EnvironmentException(TAGS + " is not a tag expression: " + tags, error);
    }
    return new Environment(
        Path.of(required(variable, CASES)), tags, Path.of(required(variable, REPORT)));
  }

  /** Reads the process's environment. */
  static Environment fromProcess() throws EnvironmentException {
    return read(System::getenv);
  }

  /** The value of a variable that must be set; an empty one counts as missing. */
  private static String required(Function<String, String> variable, String name)
      throws EnvironmentException {
    var value = variable.apply(name);
    if (value == null || value.isEmpty()) {
      throw new EnvironmentException(name + " is not set", null);
    }
    return value;
  }
}

package org.itineradev.itinera.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class EnvironmentTest {
  private static final Map<String, String> COMPLETE =
      Map.of(
          "ITINERA_CONFORMANCE_CASES", "cases",
          "ITINERA_CONFORMANCE_TAGS", "@proposal-0121 and not @non-value",
          "ITINERA_CONFORMANCE_REPORT", "reports/cucumber.json");

  private static Map<String, String> completeWith(String variable, String value) {
    var written = new HashMap<>(COMPLETE);
    written.put(variable, value);
    return written;
  }

  @Test
  void theEnvironmentSaysWhatToRunAndWhereToReport() throws EnvironmentException {
    assertEquals(
        new Environment(
            Path.of("cases"),
            "@proposal-0121 and not @non-value",
            Path.of("reports/cucumber.json")),
        Environment.read(COMPLETE::get));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ITINERA_CONFORMANCE_CASES",
        "ITINERA_CONFORMANCE_TAGS",
        "ITINERA_CONFORMANCE_REPORT"
      })
  void aVariableThatIsNotSetStopsTheRunner(String variable) {
    var written = completeWith(variable, null);
    var error = assertThrows(EnvironmentException.class, () -> Environment.read(written::get));
    assertEquals(variable + " is not set", error.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ITINERA_CONFORMANCE_CASES",
        "ITINERA_CONFORMANCE_TAGS",
        "ITINERA_CONFORMANCE_REPORT"
      })
  void anEmptyVariableCountsAsNotSet(String variable) {
    var written = completeWith(variable, "");
    var error = assertThrows(EnvironmentException.class, () -> Environment.read(written::get));
    assertEquals(variable + " is not set", error.getMessage());
  }

  @Test
  void tagsThatAreNotATagExpressionStopTheRunner() {
    var written = completeWith("ITINERA_CONFORMANCE_TAGS", "@proposal-0121 and (");
    var error = assertThrows(EnvironmentException.class, () -> Environment.read(written::get));
    assertEquals(
        "ITINERA_CONFORMANCE_TAGS is not a tag expression: @proposal-0121 and (",
        error.getMessage());
  }
}

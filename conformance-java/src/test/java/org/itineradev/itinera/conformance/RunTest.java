package org.itineradev.itinera.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class RunTest {
  private static final Path CASES = Path.of("src/test/resources/features");

  @TempDir Path directory;

  private Path report() {
    return directory.resolve("reports/cucumber.json");
  }

  private boolean runs(String tags) {
    return Main.run(new Environment(CASES, tags, report()));
  }

  private List<String> scenarios() {
    return JsonMapper.builder().build().readTree(report().toFile()).values().stream()
        .flatMap(RunTest::scenariosOf)
        .toList();
  }

  @Test
  void everySelectedScenarioRunsOnce() {
    assertTrue(runs("@declaring and not @capability-async"));
    assertEquals(
        List.of(
            "A workflow of one step that succeeds",
            "A scenario that needs synchronous steps",
            "Every sentence that declares a workflow",
            "Every sentence that declares the instance",
            "A workflow with no ID generator and no initial data",
            "Every sentence that declares reporters",
            "A dispatcher that ignores added reporters",
            "A dispatcher that throws when a reporter is added",
            "A dispatcher that throws on one kind of event",
            "The executor's own dispatchers",
            "A dispatcher factory that fails"),
        scenarios());
  }

  @Test
  void theRunnerDefinesTheSentencesOfAsynchronousScenariosToo() {
    assertTrue(runs("@declaring and @capability-async"));
    assertEquals(List.of("A scenario that needs asynchronous steps"), scenarios());
  }

  @Test
  void aScenarioThatUsesASentenceTheRunnerDoesNotDefineFails() {
    assertFalse(runs("@undefined"));
    assertEquals(List.of("A sentence the runner does not define"), scenarios());
  }

  @Test
  void aScenarioWhoseSentencesDoNotMakeAModelFails() {
    assertFalse(runs("@wrong-case"));
  }

  @Test
  void selectingNoScenarioPassesWithAnEmptyReport() {
    assertTrue(runs("@proposal-0000"));
    assertEquals(List.of(), scenarios());
  }

  /** Each scenario of a feature in the report, by its name. */
  private static Stream<String> scenariosOf(JsonNode feature) {
    return feature.path("elements").values().stream().map(RunTest::nameOf);
  }

  private static String nameOf(JsonNode scenario) {
    return scenario.path("name").asString();
  }
}

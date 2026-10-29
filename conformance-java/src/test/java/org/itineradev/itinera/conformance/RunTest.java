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
    return features().flatMap(RunTest::scenariosOf).toList();
  }

  private List<String> errors() {
    var errors = features().flatMap(RunTest::errorsOf).toList();
    return errors.isEmpty() ? List.of("no error") : errors;
  }

  private Stream<JsonNode> features() {
    return JsonMapper.builder().build().readTree(report().toFile()).values().stream();
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
  void everyScenarioThatRunsJourneysPasses() {
    assertTrue(runs("@journeys"));
    assertEquals(12, scenarios().size());
  }

  @Test
  void everyScenarioThatDeclaresAdaptersServicesAndProvidersOrListsPasses() {
    assertTrue(runs("@admission"));
    assertEquals(4, scenarios().size());
  }

  @Test
  void everyScenarioThatBuildsStepsFromInputsAndServicesPasses() {
    assertTrue(runs("@building"), errors().toString());
    assertEquals(9, scenarios().size());
  }

  @Test
  void everyScenarioWhoseStepsFailSkipAndContributePasses() {
    assertTrue(runs("@outcomes"), errors().toString());
    assertEquals(6, scenarios().size());
  }

  @Test
  void everyScenarioWhoseStepsEmitAndSetGlobalMetadataPasses() {
    assertTrue(runs("@reporting"), errors().toString());
    assertEquals(3, scenarios().size());
  }

  @Test
  void everyScenarioWhosePoliciesDefineHooksPasses() {
    assertTrue(runs("@hooks"), errors().toString());
    assertEquals(3, scenarios().size());
  }

  @Test
  void everyScenarioThatUsesTestSupportPasses() {
    assertTrue(runs("@testing"), errors().toString());
    assertEquals(12, scenarios().size());
  }

  @Test
  void aJourneyThatDidNotDoWhatTheCaseExpectsFailsAsUnmet() {
    assertFalse(runs("@unmet"));
    assertTrue(errors().stream().allMatch(RunTest::isUnmet), errors().toString());
  }

  @Test
  void everyScenarioWhoseStepsAreRescuedPasses() {
    assertTrue(runs("@rescues"), errors().toString());
    assertEquals(5, scenarios().size());
  }

  @Test
  void everyScenarioWhoseCustomCodeReturnsAnUnexpectedResultPasses() {
    assertTrue(runs("@unexpected-results"), errors().toString());
    assertEquals(10, scenarios().size());
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

  /** The error of each failed step of a feature in the report. */
  private static Stream<String> errorsOf(JsonNode feature) {
    return feature.path("elements").values().stream()
        .flatMap(RunTest::stepsOf)
        .map(RunTest::errorOf)
        .filter(RunTest::isStated);
  }

  private static Stream<JsonNode> stepsOf(JsonNode scenario) {
    return scenario.path("steps").values().stream();
  }

  private static String errorOf(JsonNode step) {
    return step.path("result").path("error_message").asString();
  }

  private static boolean isStated(String error) {
    return !error.isEmpty();
  }

  private static boolean isUnmet(String error) {
    return error.contains("Unmet");
  }
}

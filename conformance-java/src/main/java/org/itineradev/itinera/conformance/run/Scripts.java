package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Objects;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.itineradev.itinera.conformance.model.StepScript;
import org.itineradev.itinera.conformance.model.WorkflowModel;
import org.itineradev.itinera.step.testing.Ending;
import org.itineradev.itinera.step.testing.StepMock;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.Sync;

/** The step mocks that play each step's script. */
public final class Scripts {
  /** The message of an abnormal termination the case scripts with none. */
  private static final String UNWRITTEN_MESSAGE = "the scripted step terminated abnormally";

  private Scripts() {}

  /** The step mock playing the step's script, one the runner can run. */
  static StepMock mockOf(StepScript script) {
    if (script.buildFailure() != null) {
      return StepMock.failingToBuild(script.buildFailure());
    }
    var endings = endingsOf(script);
    return StepMock.playing(
        endings.get(0), endings.subList(1, endings.size()).toArray(Ending[]::new));
  }

  /** The step's retry budget and retry on abnormal termination, as its script says. */
  static StepBuilder<Sync> scriptedBy(StepBuilder<Sync> builder, StepScript script) {
    if (script.retries() != null) {
      builder.retryBudget(script.retries());
    }
    if (!script.retryOnAbnormalTermination()) {
      builder.noRetryOnAbnormalTermination();
    }
    return builder;
  }

  /** How each attempt of the step ends, a success for every attempt when the case does not say. */
  private static List<Ending> endingsOf(StepScript script) {
    if (script.attempts() == null) {
      return List.of(Ending.success());
    }
    return script.attempts().stream().map(Scripts::endingOf).toList();
  }

  /**
   * What {@code model} scripts that the runner cannot run yet, or {@code null} when it can run all
   * of it. A workflow that holds any of it is still built, so its admission and its listing are
   * checked, with each such part a call that throws {@link NotRunYet}, which no journey reaches.
   */
  public static String notRunYetIn(WorkflowModel model) {
    return model.scripts().values().stream()
        .map(Scripts::notRunYetIn)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /** What {@code script} asks for that the runner cannot run yet, or {@code null} when it can. */
  static String notRunYetIn(StepScript script) {
    if (!script.contributions().isEmpty()) {
      return "contributions";
    }
    if (script.hasRescueStep() || script.isRescueStep()) {
      return "rescue steps";
    }
    var attempts = script.attempts() == null ? List.<ScriptedAttempt>of() : script.attempts();
    if (attempts.stream().anyMatch(Scripts::contributes)) {
      return "contributions";
    }
    if (attempts.stream().anyMatch(Scripts::failsOrSkips)) {
      return "an attempt that fails or is skipped";
    }
    return null;
  }

  private static boolean contributes(ScriptedAttempt attempt) {
    return !attempt.contributes().isEmpty();
  }

  private static boolean failsOrSkips(ScriptedAttempt attempt) {
    return switch (attempt.ending()) {
      case SUCCESS, ERROR -> false;
      case FAILURE, SKIPPED -> true;
    };
  }

  private static Ending endingOf(ScriptedAttempt attempt) {
    return switch (attempt.ending()) {
      case SUCCESS -> Ending.success();
      case ERROR ->
          Ending.abnormalTermination(
              attempt.message() == null ? UNWRITTEN_MESSAGE : attempt.message());
      case FAILURE, SKIPPED -> throw new NotRunYet("an attempt that fails or is skipped");
    };
  }
}

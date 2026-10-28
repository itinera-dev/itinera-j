package org.itineradev.itinera.conformance.run;

import java.util.List;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.itineradev.itinera.conformance.model.StepScript;
import org.itineradev.itinera.step.testing.Ending;
import org.itineradev.itinera.step.testing.StepMock;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.Sync;

/** The step mocks that play each step's script. */
final class Scripts {
  /** The message of an abnormal termination the case scripts with none. */
  private static final String UNWRITTEN_MESSAGE = "the scripted step terminated abnormally";

  private Scripts() {}

  /**
   * The step mock playing the step's script.
   *
   * @throws NotRunYet when the script asks for what the runner cannot run yet
   */
  static StepMock mockOf(StepScript script) {
    var what = notRunYetIn(script);
    if (what != null) {
      throw new NotRunYet(what);
    }
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

  private static String notRunYetIn(StepScript script) {
    if (!script.inputs().isEmpty()) {
      return "inputs";
    }
    if (!script.contributions().isEmpty()) {
      return "contributions";
    }
    if (script.hasRescueStep() || script.isRescueStep()) {
      return "rescue steps";
    }
    return null;
  }

  private static Ending endingOf(ScriptedAttempt attempt) {
    if (!attempt.contributes().isEmpty()) {
      throw new NotRunYet("contributions");
    }
    return switch (attempt.ending()) {
      case SUCCESS -> Ending.success();
      case ERROR ->
          Ending.abnormalTermination(
              attempt.message() == null ? UNWRITTEN_MESSAGE : attempt.message());
      case FAILURE, SKIPPED -> throw new NotRunYet("an attempt that fails or is skipped");
    };
  }
}

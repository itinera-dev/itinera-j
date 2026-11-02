package org.itineradev.itinera.conformance.run;

import java.util.List;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.itineradev.itinera.conformance.model.StepScript;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.testing.Ending;
import org.itineradev.itinera.step.testing.RescueEnding;
import org.itineradev.itinera.step.testing.RescueStepMock;
import org.itineradev.itinera.step.testing.StepMock;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.Sync;

/** The step mocks and rescue step mocks that play each script. */
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

  /** The rescue step mock playing the rescue step's script. */
  static RescueStepMock rescueMockOf(StepScript script) {
    if (script.buildFailure() != null) {
      return RescueStepMock.failingToBuild(script.buildFailure());
    }
    var endings = rescueEndingsOf(script);
    return RescueStepMock.playing(
        endings.get(0), endings.subList(1, endings.size()).toArray(RescueEnding[]::new));
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

  /** How the rescue step's attempt ends, a success when the case does not say. */
  private static List<RescueEnding> rescueEndingsOf(StepScript script) {
    if (script.attempts() == null) {
      return List.of(RescueEnding.success());
    }
    return script.attempts().stream().map(Scripts::rescueEndingOf).toList();
  }

  private static RescueEnding rescueEndingOf(ScriptedAttempt attempt) {
    return switch (attempt.ending()) {
      case SUCCESS -> RescueEnding.success();
      case FAILURE -> RescueEnding.failure(failureReasonOf(attempt));
      case SKIPPED ->
          throw new ModelException("Java's types refuse a rescue step that skips: it never does");
      case ERROR -> RescueEnding.abnormalTermination(messageOf(attempt));
    };
  }

  private static Ending endingOf(ScriptedAttempt attempt) {
    return switch (attempt.ending()) {
      case SUCCESS -> Ending.success();
      case FAILURE -> Ending.failure(failureReasonOf(attempt));
      case SKIPPED ->
          new Ending.Skipped(attempt.code() == null ? null : reasonOf(attempt, attempt.code()));
      case ERROR -> Ending.abnormalTermination(messageOf(attempt));
    };
  }

  private static String messageOf(ScriptedAttempt attempt) {
    return attempt.message() == null ? UNWRITTEN_MESSAGE : attempt.message();
  }

  private static Reason failureReasonOf(ScriptedAttempt attempt) {
    if (attempt.code() == null) {
      throw new ModelException("attempt " + attempt.number() + " fails with no code");
    }
    return reasonOf(attempt, attempt.code());
  }

  private static Reason reasonOf(ScriptedAttempt attempt, String code) {
    return new Reason(code, attempt.message(), attempt.details());
  }
}

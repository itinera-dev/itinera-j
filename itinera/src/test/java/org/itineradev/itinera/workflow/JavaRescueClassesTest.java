package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.RescueKind;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.FromRescuedStep;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.SyncRescueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaRescueClassesTest {
  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    private final String card;
    private final String answer;
    private final Reason reason;
    private final Throwable error;

    public Renew(
        @Input("card") String card,
        @FromRescuedStep("answer") String answer,
        Reason reason,
        Throwable error) {
      this.card = card;
      this.answer = answer;
      this.reason = reason;
      this.error = error;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute(
          "card",
          card + " renewed after " + reason.code() + ", " + answer + ", " + error.getMessage());
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = Renew.class)
  public static final class Cards implements ItineraWorkflow {}

  @Test
  void aJavaRescueStepRequestsItsConstructorsParametersThenItsRuns() {
    assertEquals(
        List.of(
            "input card: String?",
            "data from the step answer: String?",
            "reason",
            "error",
            "contributor"),
        WorkflowDescriptor.of(Cards.class).getSteps().get(0).getRescue().getRequests().stream()
            .map(Request::toString)
            .toList());
  }

  @Test
  void aJavaRescueStepIsBuiltFromItsAnswersAndRun() {
    var renewal = WorkflowDescriptor.of(Cards.class).getSteps().get(0).getRescue();
    var requests = renewal.getRequests();
    var renewed = new ArrayList<String>();
    var got =
        new Answers<RescueKind<Sync>>(
            Map.of(
                requests.get(0),
                "visa",
                requests.get(1),
                "declined",
                requests.get(2),
                new Reason("expired"),
                requests.get(3),
                new IllegalStateException("bank down"),
                requests.get(4),
                (Contributor) (key, value) -> renewed.add(key + ": " + value)));
    var run = ((SyncRescueFactory) renewal.getFactory()).build(got);
    assertEquals(RescueOutcome.success(), run.run());
    assertEquals(List.of("card: visa renewed after expired, declined, bank down"), renewed);
  }

  @RescueStep
  public static final class Refuse implements ItineraRescueStep {
    private final Reason reason;

    public Refuse(Reason reason) {
      this.reason = reason;
    }

    public RescueOutcome run() {
      return reason.code().equals("stolen")
          ? fail(new Reason("stolen card", "Call the bank."))
          : fail("no renewal");
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = Refuse.class)
  public static final class Refusals implements ItineraWorkflow {}

  @Test
  void aJavaRescueStepFailsThroughItsInterfacesHelpers() {
    var refusal = WorkflowDescriptor.of(Refusals.class).getSteps().get(0).getRescue();
    var factory = (SyncRescueFactory) refusal.getFactory();
    var reason = refusal.getRequests().get(0);
    assertEquals(
        RescueOutcome.failure(new Reason("stolen card", "Call the bank.")),
        factory.build(new Answers<>(Map.of(reason, new Reason("stolen")))).run());
    assertEquals(
        RescueOutcome.failure("no renewal"),
        factory.build(new Answers<>(Map.of(reason, new Reason("expired")))).run());
  }

  @RescueStep
  public static final class Renewal implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", retryBudget = 1, rescue = Renewal.class)
  public static final class Retried implements ItineraWorkflow {}

  @Test
  void aJavaStepWithARescueStepAndRetriesIsRefusedWhenItsWorkflowIsDescribed() {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(Retried.class));
    assertEquals(
        List.of(new Violation.RescuedStepRetried(StepName.of("charge"))), refused.getViolations());
  }

  public static final class UnmarkedRenewal implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = UnmarkedRenewal.class)
  public static final class Unmarked implements ItineraWorkflow {}

  @RescueStep
  public static final class SkippingRenewal implements ItineraRescueStep {
    public Outcome run() {
      return Outcome.success();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = SkippingRenewal.class)
  public static final class Skipping implements ItineraWorkflow {}

  @RescueStep
  public static final class UnaskedRenewal implements ItineraRescueStep {
    private final String note;

    public UnaskedRenewal(String note) {
      this.note = note;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute("note", note);
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = UnaskedRenewal.class)
  public static final class Unasked implements ItineraWorkflow {}

  @RescueStep
  public static final class OptionalRenewal implements ItineraRescueStep {
    private final Optional<Reason> reason;

    public OptionalRenewal(Optional<Reason> reason) {
      this.reason = reason;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute("reason", reason.map(Reason::code).orElse("unknown"));
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = OptionalRenewal.class)
  public static final class Optionally implements ItineraWorkflow {}

  @RescueStep
  public static final class AttemptedRenewal implements ItineraRescueStep {
    public RescueOutcome run(Attempt attempt, Contributor contributor) {
      contributor.contribute("attempt", attempt.getNumber());
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = AttemptedRenewal.class)
  public static final class Attempted implements ItineraWorkflow {}

  @RescueStep
  public static final class TwiceBuiltRenewal implements ItineraRescueStep {
    private final String note;

    public TwiceBuiltRenewal() {
      this("renewed");
    }

    public TwiceBuiltRenewal(@Input("note") String note) {
      this.note = note;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute("note", note);
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = TwiceBuiltRenewal.class)
  public static final class TwiceBuilt implements ItineraWorkflow {}

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInAJavaRescueStepIsRefusedWhenItsWorkflowIsDescribed(
      Class<? extends ItineraWorkflow> workflow, String problem) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        List.of(problem),
        refused.getViolations().stream().map(JavaRescueClassesTest::problem).toList());
  }

  private static String problem(Violation violation) {
    return ((Violation.Misdeclared) violation).problem();
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a rescue step not marked @RescueStep", Unmarked.class, "is not marked @RescueStep"),
        Arguments.argumentSet(
            "a rescue step whose run returns a step's outcome",
            Skipping.class,
            "returns Outcome, a step's outcome, though a rescue step never skips: return "
                + "RescueOutcome"),
        Arguments.argumentSet(
            "a constructor parameter that requests nothing",
            Unasked.class,
            "has a parameter that requests nothing, arg0: a rescue step's constructor takes "
                + "inputs, marked @Input, data from the step it rescues, marked @FromRescuedStep, "
                + "and the failure's Reason and error by their types"),
        Arguments.argumentSet(
            "a run requesting the attempt",
            Attempted.class,
            "has a parameter that requests nothing, arg0: mark a service @Service, and request "
                + "a handle by its type"),
        Arguments.argumentSet(
            "a rescue step with several constructors",
            TwiceBuilt.class,
            "has several constructors, and a rescue step has one"),
        Arguments.argumentSet(
            "an Optional without itinera-java",
            Optionally.class,
            "requests an Optional, which itinera reads only with the artifact "
                + "org.itinera-dev:itinera-java: add it to the application's dependencies"));
  }
}

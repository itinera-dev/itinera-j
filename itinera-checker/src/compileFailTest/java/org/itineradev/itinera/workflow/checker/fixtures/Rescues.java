package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
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
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;

/** Mistakes in rescue step classes, each in a workflow of its own. */
public final class Rescues {
  private Rescues() {}

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

  @RescueStep
  public static final class Renewal implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", retryBudget = 1, rescue = Renewal.class)
  public static final class Retried implements ItineraWorkflow {}

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
}

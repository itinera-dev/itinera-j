package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepReporter;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Mistakes in what steps request, each in a workflow of its own. */
public final class Requests {
  private Requests() {}

  interface PaymentGateway {
    String take(long amount);
  }

  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;
    private final @Nullable String note;

    Charge(@Input("amount") long amount, @Input("note") @Nullable String note) {
      this.amount = amount;
      this.note = note;
    }

    public Outcome run(Contributor contributor, StepReporter reporter) {
      reporter.info("charging", note);
      contributor.contribute("charged", amount);
      return succeed();
    }
  }

  @Step
  public static final class Note implements ItineraStep {
    @NullMarked
    Note(@Input("note") String note) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "noting")
  @WithStep(step = Note.class, name = "note")
  public static final class Noting implements ItineraWorkflow {}

  @Workflow(name = "charging")
  @WithStep(step = Charge.class, name = "charge")
  public static final class Charging implements ItineraWorkflow {}

  private static final class RecordingReporter implements StepReporter {
    final List<String> infos = new ArrayList<>();

    @Override
    public void info(String message) {
      infos.add(message);
    }

    @Override
    public void info(String message, @Nullable Object data) {
      infos.add(message + " " + data);
    }

    @Override
    public void warning(String message) {}

    @Override
    public void warning(String message, @Nullable Object data) {}

    @Override
    public void error(String message) {}

    @Override
    public void error(String message, @Nullable Object data) {}

    @Override
    public void setGlobalMetadata(String key, @Nullable Object value) {}
  }

  public static final class UnmarkedStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class TwoConstructorsStep implements ItineraStep {
    TwoConstructorsStep() {}

    TwoConstructorsStep(@Input("amount") long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public final class InnerStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class UnkeyedStep implements ItineraStep {
    UnkeyedStep(@Input long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class OptionalStep implements ItineraStep {
    OptionalStep(@Input("discount") Optional<Long> discount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class LinkedStep implements ItineraStep {
    LinkedStep(@Input("lines") LinkedList<String> lines) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class InputInRunStep implements ItineraStep {
    public Outcome run(@Input("amount") long amount) {
      return succeed();
    }
  }

  @Step
  public static final class PayingStep implements ItineraStep {
    public Outcome run(@Service("payments") PaymentGateway payments) {
      return succeed();
    }
  }

  @Step
  public static final class PayingTwiceStep implements ItineraStep {
    public Outcome run(
        @Service("payments") PaymentGateway payments, @Service("payments") PaymentGateway again) {
      return succeed();
    }
  }

  @WithStep(step = UnmarkedStep.class, name = "charge")
  public static final class Unmarked implements ItineraWorkflow {}

  @WithStep(step = TwoConstructorsStep.class, name = "charge")
  public static final class TwoConstructors implements ItineraWorkflow {}

  @WithStep(step = InnerStep.class, name = "charge")
  public static final class Inner implements ItineraWorkflow {}

  @WithStep(step = UnkeyedStep.class, name = "charge")
  public static final class Unkeyed implements ItineraWorkflow {}

  @WithStep(step = OptionalStep.class, name = "charge")
  public static final class Optionally implements ItineraWorkflow {}

  @WithStep(step = LinkedStep.class, name = "charge")
  public static final class Linked implements ItineraWorkflow {}

  @WithStep(step = InputInRunStep.class, name = "charge")
  public static final class InputInRun implements ItineraWorkflow {}

  @WithStep(step = PayingStep.class, name = "charge")
  public static final class Paying implements ItineraWorkflow {}

  @WithStep(step = PayingTwiceStep.class, name = "charge")
  public static final class PayingTwice implements ItineraWorkflow {}
}

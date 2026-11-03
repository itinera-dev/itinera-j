package org.itineradev.itinera.workflow.checker.fixtures;

import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;

/** Mistakes in parts that no workflow names, each read alone. */
public final class Alone {
  private Alone() {}

  @Step
  public static final class Runless implements ItineraStep {}

  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;

    public Charge(@Input("amount") long amount) {
      this.amount = amount;
    }

    public Charge() {
      this(0);
    }

    public Outcome run() {
      return amount > 0 ? succeed() : skip();
    }
  }

  @RescueStep
  public static final class Skipping implements ItineraRescueStep {
    public Outcome run() {
      return Outcome.success();
    }
  }

  @StepPolicy(name = "silent")
  public static final class Silent implements ItineraStepPolicy {}

  @WorkflowPolicy(name = "ledger")
  public static final class Ledger implements ItineraWorkflowPolicy {
    private final String book;

    public Ledger(String book) {
      this.book = book;
    }

    @OnWorkflowSuccess
    public void paid() {
      System.out.println(book);
    }
  }

  @Step
  public static final class Pack implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }
}

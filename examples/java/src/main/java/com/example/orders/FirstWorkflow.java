package com.example.orders;

import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

public final class FirstWorkflow {
  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;

    public Charge(@Input("amount") long amount) {
      this.amount = amount;
    }

    public Outcome run(Contributor contributor) {
      if (amount <= 0) return fail("nothing to charge");
      contributor.contribute("receipt", "charged " + amount);
      return succeed();
    }
  }

  @Step
  public static final class Ship implements ItineraStep {
    private final String receipt;

    public Ship(@Input("receipt") String receipt) {
      this.receipt = receipt;
    }

    public Outcome run(Contributor contributor) {
      contributor.contribute("shipped", "after " + receipt);
      return succeed();
    }
  }

  @Workflow(name = "orders")
  @WithStep(step = Charge.class, name = "charge", retryBudget = 2)
  @WithStep(step = Ship.class, name = "ship")
  public static final class Orders implements ItineraWorkflow {}

  public record Shipment(String shipped) {}

  public static void main(String[] args) throws Refusal {
    var executor = new LocalExecutor();

    var paid = executor.run(new Orders().instance().data("amount", 40L).create());
    check(paid.isSucceeded());
    check(paid.output(TypeToken.of(Shipment.class)).equals(new Shipment("after charged 40")));

    var free = executor.run(new Orders().instance().data("amount", 0L).create());
    check(free.isFailed());
    check(free.failure().cause() == FailureCause.RETRIES_EXHAUSTED);
    check(free.failure().reason().code().equals("nothing to charge"));
  }

  private static void check(boolean holds) {
    if (!holds) throw new IllegalStateException("The orders workflow did not run as expected.");
  }
}

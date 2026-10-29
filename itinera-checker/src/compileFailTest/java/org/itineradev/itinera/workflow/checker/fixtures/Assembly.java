package org.itineradev.itinera.workflow.checker.fixtures;

import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.policy.async.AsyncHookReporter;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.service.async.AsyncProviderReporter;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.InputAdapter;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.Supplied;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

/** Mistakes in how a workflow puts its parts together, each in a workflow of its own. */
public final class Assembly {
  private Assembly() {}

  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run(@Service("payments") Runnable payments) {
      return succeed();
    }
  }

  @Step
  public static final class Print implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  @WithStep(step = Print.class, name = "print", rescue = Renew.class)
  @WithStep(step = Print.class, name = "print_rescue")
  public static final class RescueNamed implements ItineraWorkflow {}

  @WithStep(step = Print.class, name = "")
  @WithStep(step = Print.class, name = "print", retryBudget = -1)
  public static final class Unnamed implements ItineraWorkflow {}

  @WithStep(step = Print.class, name = "print", retryBudget = 2, rescue = Renew.class)
  public static final class Retried implements ItineraWorkflow {}

  @WithStep(step = Print.class, name = "print")
  @WithStep(step = Print.class, name = "file")
  public static final class Adapted implements ItineraWorkflow {
    @InputAdapter(steps = "print", name = "pricing")
    public Supplied priced() {
      return fromDataBag();
    }

    @InputAdapter(steps = {"print", "file"})
    public Supplied again() {
      return fromDataBag();
    }

    @InputAdapter(steps = "file", name = "pricing")
    public Supplied filed() {
      return fromDataBag();
    }

    @InputAdapter(steps = {})
    public Supplied nowhere() {
      return fromDataBag();
    }

    @InputAdapter(steps = "print_rescue")
    public Supplied rescued() {
      return fromDataBag();
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  @WithStep(step = Charge.class, name = "pay")
  public static final class Provisioned implements ItineraWorkflow {
    @Provider(key = "payments", name = "gateway")
    public Provided<Runnable> gateway() {
      return noInstance();
    }

    @Provider(key = "payments", name = "gateway")
    public Provided<Runnable> again() {
      return noInstance();
    }

    @Provider(
        key = "payments",
        steps = {"charge", "charge", "refund"})
    public Provided<Runnable> charging() {
      return noInstance();
    }

    @Provider(key = "payments", steps = "pay")
    public Provided<String> paying() {
      return noInstance();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Misread implements ItineraWorkflow {
    @Provider(perAttempt = true, key = "receipts")
    public Provided<String> receipts(AsyncProviderReporter reporter) {
      return noInstance();
    }

    @InputAdapter(steps = {"print", ""})
    public Supplied priced() {
      return fromDataBag();
    }
  }

  @WorkflowPolicy(name = "first")
  public static final class First implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}
  }

  @WorkflowPolicy(name = "second")
  public static final class Second implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}
  }

  @WorkflowPolicy(name = "asynchronous")
  public static final class Asynchronous implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(AsyncHookReporter reporter) {}
  }

  @Workflow(name = "asynchronous", policies = Asynchronous.class)
  @WithStep(step = Print.class, name = "print")
  public static final class AsynchronousHook implements ItineraWorkflow {}

  @Workflow(
      name = "told twice",
      policies = {First.class, Second.class, First.class})
  @WithStep(step = Print.class, name = "print")
  public static final class ToldTwice implements ItineraWorkflow {}
}

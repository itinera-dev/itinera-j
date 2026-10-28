package org.itineradev.itinera.workflow;

import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;

/** What {@link JavaAnnotationTypesTest} refuses, written so that it compiles. */
final class JavaAnnotationTypesTwins {
  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @StepPolicy(name = "audit")
  public static final class Audit implements ItineraStepPolicy {}

  @WorkflowPolicy(name = "tell")
  public static final class Tell implements ItineraWorkflowPolicy {}

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  @Workflow(name = "shop", policies = Tell.class)
  @WithStep(step = Charge.class, name = "charge", policies = Audit.class, rescue = Renew.class)
  public static final class Shop implements ItineraWorkflow {}
}

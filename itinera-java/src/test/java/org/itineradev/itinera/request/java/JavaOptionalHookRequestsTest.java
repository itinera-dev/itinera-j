package org.itineradev.itinera.request.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.OnFailure;
import org.itineradev.itinera.policy.OnStepFailure;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaOptionalHookRequestsTest {
  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @StepPolicy(name = "audit")
  public static final class Audit implements ItineraStepPolicy {
    @OnStepFailure
    public OnFailure failed(
        Optional<Reason> reason,
        Optional<Throwable> error,
        @FromStep("charged") Optional<Long> charged,
        @FromWorkflow("order") Optional<String> order) {
      return keepDefault();
    }
  }

  @Workflow(name = "charging")
  @WithStep(step = Charge.class, name = "charge", policies = Audit.class)
  public static final class Charging implements ItineraWorkflow {}

  @Test
  void anOptionalParameterOfAJavaHookIsAnOptionalRequestOfWhatItHolds() {
    var audit = WorkflowDescriptor.of(Charging.class).getSteps().get(0).getPolicies().get(0);
    assertEquals(
        List.of(
            "optional reason",
            "optional error",
            "optional data from the step charged: Long",
            "optional data from the workflow order: String"),
        audit.getOnStepFailure().getRequests().stream().map(Request::toString).toList());
  }
}

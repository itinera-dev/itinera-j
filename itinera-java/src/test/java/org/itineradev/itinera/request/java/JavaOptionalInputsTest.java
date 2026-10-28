package org.itineradev.itinera.request.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaOptionalInputsTest {
  @Step
  public static final class Price implements ItineraStep {
    Price(@Input("amount") long amount, @Input("discount") Optional<Long> discount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "pricing")
  @WithStep(step = Price.class, name = "price")
  public static final class Pricing implements ItineraWorkflow {}

  @Test
  void anOptionalParameterOfAJavaStepIsAnOptionalInputOfWhatItHolds() {
    var price = WorkflowDescriptor.of(Pricing.class).getSteps().get(0);
    assertEquals(
        List.of("input amount: Long", "optional input discount: Long"),
        price.getRequests().stream().map(Request::toString).toList());
  }
}

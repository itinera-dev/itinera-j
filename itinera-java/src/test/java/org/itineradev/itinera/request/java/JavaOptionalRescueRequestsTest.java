package org.itineradev.itinera.request.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.step.FromRescuedStep;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaOptionalRescueRequestsTest {
  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    private final Optional<String> answer;
    private final Optional<Reason> reason;
    private final Optional<Throwable> error;

    public Renew(
        @FromRescuedStep("answer") Optional<String> answer,
        Optional<Reason> reason,
        Optional<Throwable> error) {
      this.answer = answer;
      this.reason = reason;
      this.error = error;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute(
          "card",
          answer.orElse("no answer")
              + ", "
              + reason.map(Reason::code).orElse("no reason")
              + ", "
              + error.map(Throwable::getMessage).orElse("no error"));
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge", rescue = Renew.class)
  public static final class Cards implements ItineraWorkflow {}

  @Test
  void optionalParametersOfAJavaRescueStepAreOptionalRequestsOfWhatTheyHold() {
    assertEquals(
        List.of(
            "optional data from the step answer: String",
            "optional reason",
            "optional error",
            "contributor"),
        WorkflowDescriptor.of(Cards.class).getSteps().get(0).getRescue().getRequests().stream()
            .map(Request::toString)
            .toList());
  }
}

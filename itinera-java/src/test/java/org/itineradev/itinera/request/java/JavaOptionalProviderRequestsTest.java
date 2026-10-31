package org.itineradev.itinera.request.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.service.JourneyProviderDescriptor;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaOptionalProviderRequestsTest {
  @Step
  public static final class Print implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Printing implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@FromWorkflow("printer") Optional<String> printer) {
      return provide(printer.orElse("the counter's"));
    }
  }

  @Test
  void anOptionalParameterOfAJavaProviderIsAnOptionalRequestOfWhatItHolds() {
    var receipts =
        (JourneyProviderDescriptor<?, ?>)
            WorkflowDescriptor.of(Printing.class).getProviders().get(0);
    assertEquals(
        List.of("optional data from the workflow printer: String"),
        receipts.getRequests().stream().map(Request::toString).toList());
  }
}

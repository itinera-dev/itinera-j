package org.itineradev.itinera.request.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaOptionalJourneyDataTest {
  record Order(String order, Optional<String> note) {}

  static final class Shop {}

  private static InstanceBuilder ordering(Order order) {
    var workflow =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(StepDescriptor.builder("pack", got -> Outcome::success).build())
            .build();
    return InstanceBuilder.of(workflow, new Shop()).initialData(order);
  }

  @Test
  void anEmptyOptionalComponentGivesNoKeyAndReadsEmptyWhenItsKeyHoldsNothing() throws Refusal {
    var order = new Order("42", Optional.empty());
    var instance = ordering(order).create();
    assertEquals(List.of("order"), List.copyOf(instance.getInitialData().keySet()));
    assertEquals(order, new LocalExecutor().run(instance).output(TypeToken.of(Order.class)));
  }

  @Test
  void anOptionalComponentGivesWhatItHoldsAndReadsItBack() throws Refusal {
    var order = new Order("42", Optional.of("fragile"));
    var instance = ordering(order).create();
    assertEquals("fragile", instance.getInitialData().get("note"));
    assertEquals(order, new LocalExecutor().run(instance).output(TypeToken.of(Order.class)));
  }
}

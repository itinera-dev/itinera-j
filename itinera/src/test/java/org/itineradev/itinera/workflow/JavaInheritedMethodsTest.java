package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.junit.jupiter.api.Test;

class JavaInheritedMethodsTest {
  abstract static class Telling {
    @OnWorkflowSuccess
    public void paid(JourneyId journey) {}
  }

  @WorkflowPolicy(name = "tell")
  public static final class Tell extends Telling implements ItineraWorkflowPolicy {}

  @WorkflowPolicy(name = "note")
  public static final class Note extends Telling implements ItineraWorkflowPolicy {
    public void paid(String note) {}
  }

  @Step
  public static final class Charge implements ItineraStep {
    public Charge(@Input("amount") long amount) {}

    public Outcome run(@Service("receipts") String receipts) {
      return succeed();
    }
  }

  abstract static class Shop implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@Key String key) {
      return supply(40L);
    }

    @Provider(key = "receipts")
    public Provided<String> receipts() {
      return noInstance();
    }

    @IdGenerator
    public String id() {
      return "order";
    }
  }

  @Workflow(name = "shop", policies = Tell.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class Orders extends Shop {}

  @Workflow(name = "noted", policies = Note.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class Notes extends Shop {}

  @Test
  void aPublicPolicyDefinesTheHooksItInheritsFromAClassThatIsNotPublic() {
    assertNotNull(WorkflowDescriptor.of(Orders.class).getPolicies().get(0).getOnWorkflowSuccess());
  }

  @Test
  void aPublicWorkflowHasTheMethodsItInheritsFromAClassThatIsNotPublic() {
    var orders = WorkflowDescriptor.of(Orders.class);
    assertEquals(
        List.of(1, 1), List.of(orders.getInputAdapters().size(), orders.getProviders().size()));
    assertNotNull(orders.getIdGenerator());
  }

  @Test
  void aPublicPolicyDefinesAnInheritedHookThatItOverloads() {
    assertNotNull(WorkflowDescriptor.of(Notes.class).getPolicies().get(0).getOnWorkflowSuccess());
  }
}

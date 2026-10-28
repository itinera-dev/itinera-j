package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.itineradev.itinera.policy.PolicyName;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.ProviderDescriptor;
import org.itineradev.itinera.service.ProviderName;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.StepName;
import org.junit.jupiter.api.Test;

final class JavaListingTest {
  static final class Shop {}

  @Test
  void javaListsAWorkflowAndReadsEachEntry() {
    var labels = Requests.service("labels", String.class);
    var charging =
        StepDescriptor.builder("charge", got -> Outcome::success)
            .request(labels)
            .rescue(RescueStepDescriptor.builder(got -> RescueOutcome::success).build())
            .build();
    var labeller =
        ProviderDescriptor.<Shop, String>builder(
                "labeller", "labels", String.class, (shop, got) -> new Provided.Instance<>("label"))
            .build();
    var listing =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(charging)
            .inputAdapter(
                InputAdapterDescriptor.<Shop>builder(
                        "pricing", "charge", (shop, got) -> Supplied.FromDataBag.INSTANCE)
                    .build())
            .provider(labeller)
            .build()
            .listing();
    assertEquals(
        List.of(
            new ListedStep(
                StepName.of("charge"),
                Position.of(1),
                List.<PolicyName>of(),
                AdapterName.of("pricing"),
                List.of(new ListedService("labels", ProviderName.of("labeller"))),
                new ListedRescue(StepName.of("charge_rescue"), null, List.of()))),
        listing);
    var charge = listing.get(0);
    assertEquals(
        List.of("charge", "1", "pricing", "labels", "labeller", "charge_rescue"),
        List.of(
            charge.step().toString(),
            String.valueOf(charge.position().getNumber()),
            String.valueOf(charge.adapter()),
            charge.services().get(0).key(),
            charge.services().get(0).provider().toString(),
            charge.rescue().step().toString()));
  }
}

package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.DataBagAccessKind;
import org.itineradev.itinera.request.InputAdapterKind;
import org.itineradev.itinera.request.KeyKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.StepName;
import org.junit.jupiter.api.Test;

final class JavaInputAdaptersTest {
  static final class Shop {
    final long price = 40;
  }

  static final Request<KeyKind, String> KEY = Requests.key();
  static final Request<DataBagAccessKind, DataBagAccess> ACCESS = Requests.dataBagAccess();

  static Supplied priced(Shop shop, String key) {
    return key.equals("amount") ? new Supplied.Value(shop.price) : Supplied.FromDataBag.INSTANCE;
  }

  @Test
  void javaBuildsAnInputAdapterWithALambdaAndAddsIt() {
    InputAdapterDescriptor<Shop, Sync> pricing =
        InputAdapterDescriptor.<Shop>builder(
                "pricing", "charge", (shop, got) -> priced(shop, got.get(KEY)))
            .request(KEY)
            .request(ACCESS)
            .build();
    WorkflowDescriptor<Shop, Sync> shop =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(StepDescriptor.builder("charge", got -> Outcome::success).build())
            .inputAdapter(pricing)
            .build();
    assertEquals(List.of(pricing), shop.getInputAdapters());
    assertEquals(List.of(StepName.of("charge")), pricing.getSteps());
    var call = (SyncInputAdapter<Shop>) pricing.getCall();
    assertEquals(
        new Supplied.Value(40L),
        call.adapt(new Shop(), new Answers<InputAdapterKind>(Map.of(KEY, "amount"))));
  }

  @Test
  void javaReadsWhatRefusedAnAdapter() {
    var refused =
        assertThrows(
            Violations.class,
            () ->
                WorkflowDescriptor.builder(Shop.class, "shop")
                    .step(StepDescriptor.builder("charge", got -> Outcome::success).build())
                    .inputAdapter(
                        InputAdapterDescriptor.<Shop>builder(
                                "pricing", "ship", (shop, got) -> Supplied.FromDataBag.INSTANCE)
                            .build())
                    .build());
    var unknown = (Violation.InputAdapterForUnknownStep) refused.getViolations().get(0);
    assertEquals(AdapterName.of("pricing"), unknown.adapter());
    assertEquals(StepName.of("ship"), unknown.step());
    assertEquals("input adapter for unknown step", unknown.named());
  }
}

package org.itineradev.itinera.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.AttemptKind;
import org.itineradev.itinera.request.AttemptProviderKind;
import org.itineradev.itinera.request.JourneyIdKind;
import org.itineradev.itinera.request.JourneyProviderKind;
import org.itineradev.itinera.request.ProviderReporterKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.Violation;
import org.itineradev.itinera.workflow.Violations;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaProvidersTest {
  static final class Shop {
    final String name = "corner";
  }

  @FunctionalInterface
  interface Payments {
    String take(long amount);
  }

  static final Request<JourneyIdKind, JourneyId> JOURNEY_ID = Requests.journeyId();
  static final Request<AttemptKind, Attempt> ATTEMPT = Requests.attempt();
  static final Request<ProviderReporterKind<Sync>, ProviderReporter> REPORTER =
      Requests.providerReporter();

  static StepDescriptor<Sync> charge() {
    return StepDescriptor.builder("charge", got -> Outcome::success)
        .request(Requests.service("payments", Payments.class))
        .build();
  }

  @Test
  void javaBuildsAProviderOfJourneyScopeWithALambdaAndAddsIt() {
    ProviderDescriptor<Shop, Sync> gateway =
        ProviderDescriptor.<Shop, Payments>builder(
                "gateway",
                "payments",
                Payments.class,
                (shop, got) -> {
                  got.get(REPORTER).info("opening the gateway");
                  return new Provided.Instance<>(
                      amount -> shop.name + "-" + got.get(JOURNEY_ID) + "-" + amount, shop.name);
                })
            .request(JOURNEY_ID)
            .request(REPORTER)
            .build();
    WorkflowDescriptor<Shop, Sync> shop =
        WorkflowDescriptor.builder(Shop.class, "shop").step(charge()).provider(gateway).build();
    assertEquals(List.of(gateway), shop.getProviders());
    var opened = new ArrayList<String>();
    @SuppressWarnings("unchecked")
    var call =
        (SyncProvider<Shop, JourneyProviderKind<Sync>, Payments>)
            ((JourneyProviderDescriptor<Shop, Sync>) gateway).getCall();
    var provided =
        (Provided.Instance<Payments>)
            call.provide(
                new Shop(),
                new Answers<JourneyProviderKind<Sync>>(
                    Map.of(JOURNEY_ID, JourneyId.of("7"), REPORTER, new Opening(opened))));
    assertEquals("corner-7-40", provided.instance().take(40));
    assertEquals("corner", provided.data());
    assertEquals(List.of("opening the gateway"), opened);
  }

  /** A provider reporter that keeps the messages of the events it is given. */
  record Opening(List<String> messages) implements ProviderReporter {
    @Override
    public void info(String message) {
      messages.add(message);
    }

    @Override
    public void info(String message, Object data) {
      messages.add(message);
    }

    @Override
    public void warning(String message) {
      messages.add(message);
    }

    @Override
    public void warning(String message, Object data) {
      messages.add(message);
    }

    @Override
    public void error(String message) {
      messages.add(message);
    }

    @Override
    public void error(String message, Object data) {
      messages.add(message);
    }

    @Override
    public void setGlobalMetadata(String key, Object value) {
      messages.add(key);
    }
  }

  @Test
  void javaBuildsAProviderOfAttemptScopeThatProvidesNoInstance() {
    ProviderDescriptor<Shop, Sync> gateway =
        ProviderDescriptor.<Shop, Payments>perAttempt(
                "gateway", "payments", Payments.class, (shop, got) -> Provided.noInstance())
            .step("charge")
            .request(ATTEMPT)
            .build();
    assertEquals(List.of(StepName.of("charge")), gateway.getSteps());
    @SuppressWarnings("unchecked")
    var call =
        (SyncProvider<Shop, AttemptProviderKind<Sync>, Payments>)
            ((AttemptProviderDescriptor<Shop, Sync>) gateway).getCall();
    assertEquals(
        Provided.NoInstance.INSTANCE,
        call.provide(
            new Shop(), new Answers<AttemptProviderKind<Sync>>(Map.of(ATTEMPT, Attempt.of(1)))));
  }

  @Test
  void javaReadsWhatRefusedAProvider() {
    var refused =
        assertThrows(
            Violations.class,
            () ->
                WorkflowDescriptor.builder(Shop.class, "shop")
                    .step(charge())
                    .provider(
                        ProviderDescriptor.<Shop, String>builder(
                                "labels",
                                "payments",
                                String.class,
                                (shop, got) -> new Provided.Instance<>("label"))
                            .build())
                    .build());
    var wrong = (Violation.WrongServiceType) refused.getViolations().get(0);
    assertEquals(StepName.of("charge"), wrong.step());
    assertEquals("payments", wrong.key());
    assertEquals(ProviderName.of("labels"), wrong.provider());
    assertEquals("wrong service type", wrong.named());
  }
}

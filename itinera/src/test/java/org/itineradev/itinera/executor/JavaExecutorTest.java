package org.itineradev.itinera.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.itineradev.itinera.event.ByAdapterForKey;
import org.itineradev.itinera.event.ByProviderForKey;
import org.itineradev.itinera.event.ByStep;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.UnexpectedResult;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.AbortReason;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyStatus;
import org.itineradev.itinera.report.Reporter;
import org.itineradev.itinera.report.ReporterFactory;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.service.ProviderDescriptor;
import org.itineradev.itinera.service.ProviderName;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.SyncRescueFactory;
import org.itineradev.itinera.step.SyncStepFactory;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.AdapterName;
import org.itineradev.itinera.workflow.InputAdapterDescriptor;
import org.itineradev.itinera.workflow.RescueStepDescriptor;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowBuilder;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaExecutorTest {
  static final class Shop {}

  private static WorkflowBuilder<Shop, Sync> shop() {
    return WorkflowDescriptor.builder(Shop.class, "shop");
  }

  private static WorkflowBuilder<Shop, Sync> packing(SyncStepFactory factory) {
    return shop().step(StepDescriptor.builder("pack", factory).build());
  }

  private static InstanceBuilder oneStep(SyncStepFactory factory) {
    return InstanceBuilder.of(packing(factory).build(), new Shop());
  }

  /**
   * A step {@code pack} that fails once, then succeeds, rescued by the rescue step that {@code
   * rescue} builds.
   */
  private static InstanceBuilder rescued(SyncRescueFactory rescue) {
    var attempts = new ArrayList<>(List.of(Outcome.failure("torn"), Outcome.success()));
    var pack =
        StepDescriptor.builder("pack", got -> () -> attempts.remove(0))
            .rescue(RescueStepDescriptor.builder(rescue).build());
    return InstanceBuilder.of(shop().step(pack.build()).build(), new Shop());
  }

  @Test
  void aJavaWorkflowBuiltWithLambdasRunsAndSucceeds() throws Refusal {
    var kinds = new ArrayList<String>();
    Reporter reporter = event -> kinds.add(event.getKind().toString());
    var result =
        new LocalExecutor().run(oneStep(got -> Outcome::success).reporter(reporter).create());
    assertTrue(result.isSucceeded());
    assertInstanceOf(JourneyStatus.Succeeded.class, result.status());
    assertEquals(
        java.util.List.of(
            "journey_started", "attempt_started", "step_succeeded", "journey_succeeded"),
        kinds);
  }

  @Test
  void aStepThatThrowsFailsTheJourneyAndTheResultCarriesTheError() throws Refusal {
    var error = new IllegalStateException("down");
    var result =
        new LocalExecutor()
            .run(
                oneStep(
                        got ->
                            () -> {
                              throw error;
                            })
                    .create());
    assertTrue(result.isFailed());
    assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure().cause());
    assertEquals(error, result.failure().error());
    assertNull(result.abort());
  }

  @Test
  void aJavaStepFailsWithAReasonAndItsContributionsAreNeverCommitted() throws Refusal {
    var contributor = Requests.contributor();
    var charging =
        StepDescriptor.builder(
                "charge",
                got ->
                    () -> {
                      got.get(contributor).contribute("draft", "D-1");
                      return Outcome.failure(new Reason("declined", "No.", Map.of("bank", "05")));
                    })
            .request(contributor);
    var result =
        new LocalExecutor()
            .run(InstanceBuilder.of(shop().step(charging.build()).build(), new Shop()).create());
    assertEquals(FailureCause.FAILURE, result.failure().cause());
    assertEquals(new Reason("declined", "No.", Map.of("bank", "05")), result.failure().reason());
    assertEquals(Set.of(), ((JourneyStatus.Failed) result.status()).dataBag().getKeys());
  }

  @Test
  void aJavaStepThatSkipsLetsTheJourneyGoOn() throws Refusal {
    var result = new LocalExecutor().run(oneStep(got -> () -> Outcome.skipped("none")).create());
    assertTrue(result.isSucceeded());
  }

  @Test
  void aJavaStepEmitsAndSetsGlobalMetadataThroughItsStepReporter() throws Refusal {
    var reporting = Requests.stepReporter();
    var step =
        StepDescriptor.builder(
                "pack",
                got -> {
                  var reporter = got.get(reporting);
                  return () -> {
                    reporter.warning("heavy", Map.of("kilos", 30L));
                    reporter.setGlobalMetadata("carrier", "north");
                    return Outcome.success();
                  };
                })
            .request(reporting)
            .build();
    var kinds = new ArrayList<String>();
    var keys = new ArrayList<String>();
    var reporter =
        new Reporter() {
          @Override
          public void report(Event event) {
            kinds.add(event.getKind().toString());
          }

          @Override
          public void setGlobalMetadata(String key, Object value) {
            keys.add(key);
          }
        };
    var instance = InstanceBuilder.of(shop().step(step).build(), new Shop()).reporter(reporter);
    assertTrue(new LocalExecutor().run(instance.create()).isSucceeded());
    assertEquals(
        List.of("journey_started", "attempt_started", "step_warning", "global_metadata_set"),
        kinds.subList(0, 4));
    assertEquals(List.of("journey_id", "carrier"), keys);
  }

  @Test
  void aFactoryReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var result = new LocalExecutor().run(oneStep(got -> null).create());
    assertEquals(AbortReason.UNEXPECTED_RESULT, result.abort().reason());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack"), ByStep.INSTANCE, "the step's factory returned null, not a step"),
        result.abort().details());
  }

  @Test
  void aRunReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var result = new LocalExecutor().run(oneStep(got -> () -> null).create());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack"), ByStep.INSTANCE, "the step's run returned null, not an outcome"),
        result.abort().details());
  }

  @Test
  void anInputAdapterReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var order = Requests.input("order", TypeToken.of(String.class));
    var step = StepDescriptor.builder("pack", got -> Outcome::success).request(order).build();
    var adapter = InputAdapterDescriptor.<Shop>builder("pricing", "pack", (shop, got) -> null);
    var descriptor = shop().step(step).inputAdapter(adapter.build()).build();
    var result = new LocalExecutor().run(InstanceBuilder.of(descriptor, new Shop()).create());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack"),
            new ByAdapterForKey(AdapterName.of("pricing"), "order"),
            "the input adapter returned null, not what it supplies"),
        result.abort().details());
  }

  @Test
  void aServiceProviderReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var gateway = Requests.service("gateway", Runnable.class);
    var step = StepDescriptor.builder("pack", got -> Outcome::success).request(gateway).build();
    var provider =
        ProviderDescriptor.<Shop, Runnable>builder(
            "payments", "gateway", Runnable.class, (shop, got) -> null);
    var descriptor = shop().step(step).provider(provider.build()).build();
    var result = new LocalExecutor().run(InstanceBuilder.of(descriptor, new Shop()).create());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack"),
            new ByProviderForKey(ProviderName.of("payments"), "gateway"),
            "the service provider returned null, not what it provides"),
        result.abort().details());
  }

  @Test
  void aDispatcherFactoryReturningNullRefusesTheInstance() {
    var refusal =
        assertThrows(
            Refusal.class,
            () -> new LocalExecutor(() -> null).run(oneStep(got -> Outcome::success).create()));
    assertEquals(Refused.DispatcherFactoryReturnedNull.INSTANCE, refusal.getRefused());
  }

  @Test
  void anIdGeneratorReturningNullMakesCreateThrow() {
    var descriptor =
        shop()
            .step(StepDescriptor.builder("pack", got -> Outcome::success).build())
            .idGenerator((shop, data) -> null)
            .build();
    var thrown =
        assertThrows(
            IllegalStateException.class, () -> InstanceBuilder.of(descriptor, new Shop()).create());
    assertEquals(
        "The ID generator returned null: a journey ID is never empty.", thrown.getMessage());
  }

  @Test
  void aReporterFactoryReturningNullMakesCreateThrow() {
    assertThrows(
        IllegalStateException.class,
        () -> oneStep(got -> Outcome::success).reporterFor(id -> null).create());
  }

  @Test
  void aReporterTheWorkflowListsIsMadeForEachInstanceFromJava() throws Refusal {
    var kinds = new ArrayList<String>();
    ReporterFactory printing = id -> event -> kinds.add(id + " " + event.getKind());
    var listing = packing(got -> Outcome::success).reporter(printing).build();
    var instance = InstanceBuilder.of(listing, new Shop()).create();
    new LocalExecutor().run(instance);
    assertEquals(instance.getJourneyId() + " journey_started", kinds.get(0));
  }

  @Test
  void aListedReporterFactoryReturningNullMakesCreateThrow() {
    var listing = packing(got -> Outcome::success).reporter(id -> null).build();
    assertThrows(
        IllegalStateException.class, () -> InstanceBuilder.of(listing, new Shop()).create());
  }

  @Test
  void aJavaRescueStepRescuesItsStepWhichThenRunsAgain() throws Refusal {
    var result = new LocalExecutor().run(rescued(got -> RescueOutcome::success).create());
    assertTrue(result.isSucceeded());
  }

  @Test
  void aJavaRescueStepThatFailsFailsTheJourneyWithItsReason() throws Refusal {
    var result =
        new LocalExecutor().run(rescued(got -> () -> RescueOutcome.failure("no tape")).create());
    assertEquals(new Reason("no tape"), result.failure().reason());
  }

  @Test
  void aRescueStepFactoryReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var result = new LocalExecutor().run(rescued(got -> null).create());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack_rescue"),
            ByStep.INSTANCE,
            "the rescue step's factory returned null, not a rescue step"),
        result.abort().details());
  }

  @Test
  void aRescueStepRunReturningNullAbortsTheJourneyWithAnUnexpectedResult() throws Refusal {
    var result = new LocalExecutor().run(rescued(got -> () -> null).create());
    assertEquals(
        new UnexpectedResult(
            StepName.of("pack_rescue"),
            ByStep.INSTANCE,
            "the rescue step's run returned null, not an outcome"),
        result.abort().details());
  }
}

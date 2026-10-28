package org.itineradev.itinera.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.itineradev.itinera.event.ByStep;
import org.itineradev.itinera.event.UnexpectedResult;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.AbortReason;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyStatus;
import org.itineradev.itinera.report.Reporter;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.SyncStepFactory;
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

  private static InstanceBuilder oneStep(SyncStepFactory factory) {
    return InstanceBuilder.of(
        shop().step(StepDescriptor.builder("pack", factory).build()).build(), new Shop());
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
}

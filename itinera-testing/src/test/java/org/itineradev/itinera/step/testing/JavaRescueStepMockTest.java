package org.itineradev.itinera.step.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.AbortReason;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.workflow.RescueStepDescriptor;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaRescueStepMockTest {
  static final class Shop {}

  @Test
  void aRescueStepMockPlaysItsScriptInAJavaWorkflow() throws Refusal {
    var renew = RescueStepMock.playing(RescueEnding.success());
    assertTrue(ran(renew).isSucceeded());
    assertEquals(1, renew.getBuilds());
  }

  @Test
  void aRescueEndingGivesTheOutcomeItStandsForInJava() {
    assertEquals(RescueOutcome.success(), RescueEnding.success().outcome());
    assertEquals(RescueOutcome.failure("stolen"), RescueEnding.failure("stolen").outcome());
  }

  @Test
  void aRescueStepMockFailingToBuildAbortsAJavaWorkflow() throws Refusal {
    var result = ran(RescueStepMock.failingToBuild("no bank"));
    assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort().reason());
    assertEquals("no bank", result.abort().errorMessage());
  }

  private static JourneyResult ran(RescueStepMock renew) throws Refusal {
    var charge = StepMock.playing(Ending.failure("expired"), Ending.success());
    var orders =
        WorkflowDescriptor.builder(Shop.class, "orders")
            .step(
                StepDescriptor.builder("charge", charge)
                    .rescue(RescueStepDescriptor.builder(renew).build())
                    .build())
            .build();
    return new LocalExecutor().run(InstanceBuilder.of(orders, new Shop()).create());
  }
}

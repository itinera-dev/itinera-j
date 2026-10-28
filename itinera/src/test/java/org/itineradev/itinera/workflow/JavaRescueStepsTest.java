package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.ContributorKind;
import org.itineradev.itinera.request.FromStepKind;
import org.itineradev.itinera.request.ReasonKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.RescueKind;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.SyncRescueFactory;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;

final class JavaRescueStepsTest {
  static final class Shop {}

  static final Request<FromStepKind, String> TOKEN =
      Requests.fromStep("token", TypeToken.of(String.class));
  static final Request<ReasonKind, Reason> REASON = Requests.reason();
  static final Request<ContributorKind, Contributor> CONTRIBUTOR = Requests.contributor();

  @Test
  void javaBuildsARescueStepWithALambdaAndGivesItToItsStep() {
    RescueStepDescriptor<Sync> renewal =
        RescueStepDescriptor.builder(
                got -> {
                  var token = got.get(TOKEN);
                  var code = got.get(REASON).code();
                  var contributor = got.get(CONTRIBUTOR);
                  return () -> {
                    contributor.contribute("token", token + " renewed after " + code);
                    return RescueOutcome.success();
                  };
                })
            .request(TOKEN)
            .request(REASON)
            .request(CONTRIBUTOR)
            .build();
    var shop =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(StepDescriptor.builder("charge", got -> Outcome::success).rescue(renewal).build())
            .build();
    assertEquals(renewal, shop.getSteps().get(0).getRescue());
    var renewed = new ArrayList<String>();
    var run =
        ((SyncRescueFactory) renewal.getFactory())
            .build(
                new Answers<RescueKind<Sync>>(
                    Map.of(
                        TOKEN,
                        "old",
                        REASON,
                        new Reason("expired"),
                        CONTRIBUTOR,
                        (Contributor) (key, value) -> renewed.add(key + ": " + value))));
    assertEquals(RescueOutcome.success(), run.run());
    assertEquals(List.of("token: old renewed after expired"), renewed);
  }

  @Test
  void aJavaStepWithARescueStepAndRetriesIsRefusedWhenItsWorkflowIsBuilt() {
    var charge =
        StepDescriptor.builder("charge", got -> Outcome::success)
            .rescue(RescueStepDescriptor.builder(got -> RescueOutcome::success).build())
            .retryBudget(1)
            .build();
    var refused =
        assertThrows(
            Violations.class,
            () -> WorkflowDescriptor.builder(Shop.class, "shop").step(charge).build());
    assertEquals(
        List.of(new Violation.RescuedStepRetried(StepName.of("charge"))), refused.getViolations());
  }
}

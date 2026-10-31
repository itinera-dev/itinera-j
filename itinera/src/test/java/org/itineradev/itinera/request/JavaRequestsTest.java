package org.itineradev.itinera.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.StepReporter;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.junit.jupiter.api.Test;

final class JavaRequestsTest {
  interface PaymentGateway {
    String take(long amount);
  }

  static final Request<BuildKind, Long> AMOUNT = Requests.input("amount", TypeToken.of(Long.class));
  static final Request<BuildKind, Long> DISCOUNT =
      Requests.optionalInput("discount", TypeToken.of(Long.class));
  static final ServiceRequest<PaymentGateway> PAYMENTS =
      Requests.service("payments", PaymentGateway.class);
  static final Request<ContributorKind, Contributor> CONTRIBUTOR = Requests.contributor();
  static final Request<StepReporterKind<Sync>, StepReporter> REPORTER = Requests.stepReporter();

  @Test
  void aJavaFactoryReadsEachRequestAsItsOwnType() {
    StepDescriptor<Sync> charge =
        StepDescriptor.builder(
                "charge",
                got -> {
                  long amount = got.get(AMOUNT);
                  Long discount = got.get(DISCOUNT);
                  PaymentGateway payments = got.get(PAYMENTS);
                  Contributor contributor = got.get(CONTRIBUTOR);
                  StepReporter reporter = got.get(REPORTER);
                  return () -> {
                    reporter.info("charging");
                    contributor.contribute(
                        "payment", payments.take(amount - (discount == null ? 0 : discount)));
                    return Outcome.success();
                  };
                })
            .request(AMOUNT)
            .request(DISCOUNT)
            .request(PAYMENTS)
            .request(CONTRIBUTOR)
            .request(REPORTER)
            .build();
    assertEquals(
        List.of(
            "input amount: Long",
            "optional input discount: Long",
            "service payments: PaymentGateway",
            "contributor",
            "step reporter"),
        charge.getRequests().stream().map(Request::toString).toList());
  }
}

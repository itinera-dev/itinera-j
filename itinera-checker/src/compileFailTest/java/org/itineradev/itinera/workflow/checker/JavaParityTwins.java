package org.itineradev.itinera.workflow.checker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.HookReporter;
import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.OnFailure;
import org.itineradev.itinera.policy.OnStepAbnormalTermination;
import org.itineradev.itinera.policy.OnStepFailure;
import org.itineradev.itinera.policy.OnStepRetry;
import org.itineradev.itinera.policy.OnStepSuccess;
import org.itineradev.itinera.policy.OnSuccess;
import org.itineradev.itinera.policy.OnWorkflowFailure;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.RetryCause;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.report.Reporter;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.service.ProviderReporter;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.FromRescuedStep;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.StepReporter;
import org.itineradev.itinera.workflow.IdGenerator;
import org.itineradev.itinera.workflow.InputAdapter;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.Key;
import org.itineradev.itinera.workflow.Supplied;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;
import org.itineradev.itinera.workflow.checker.fixtures.KotlinAuditing;
import org.itineradev.itinera.workflow.checker.fixtures.KotlinCharge;

/**
 * A workflow with no mistake, declaring every part an annotated Java workflow can have, and
 * workflows with no mistake whose parts are written in Kotlin or extend a class that is.
 */
public final class JavaParityTwins {
  private JavaParityTwins() {}

  @Role
  public interface Customers {
    void tell(String message);
  }

  public interface Payments {
    String take(long amount);
  }

  public record Line(String product, long quantity) {}

  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;

    public Charge(
        @Input("amount") long amount,
        @Input(value = "lines", required = true) List<Line> lines,
        @Input("totals") Map<String, Long> totals,
        @Input("note") Optional<String> note) {
      this.amount = amount;
    }

    public Outcome run(
        @Service("payments") Payments payments, Contributor contributor, StepReporter reporter) {
      contributor.contribute("charged", payments.take(amount));
      return succeed();
    }
  }

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    public Renew(
        @Input("card") String card,
        @FromRescuedStep("declined") String declined,
        Reason reason,
        Throwable error) {}

    public RescueOutcome run(@Service("payments") Payments payments) {
      return succeed();
    }
  }

  @Step
  public static final class Ship implements ItineraStep {
    public Outcome run(@Service("receipts") String receipts) {
      return succeed();
    }
  }

  @StepPolicy(name = "audit")
  public static final class Audit implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(
        @FromStep("charged") String charged,
        Contributor contributor,
        JourneyId journey,
        StepName step,
        Attempt attempt) {
      return keepDefault();
    }

    @OnStepFailure
    public OnFailure failed(FailureCause cause, Reason reason, HookReporter reporter) {
      return keepDefault();
    }

    @OnStepRetry
    public OnFailure retried(RetryCause cause, Throwable error, @Role Customers customers) {
      return keepDefault();
    }

    @OnStepAbnormalTermination
    public OnFailure terminated(@FromWorkflow("order") String order) {
      return keepDefault();
    }
  }

  @WorkflowPolicy(name = "tell")
  public static final class Tell extends Failing implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(@Role Customers customers, JourneyId journey) {}
  }

  abstract static class Failing {
    @OnWorkflowFailure
    public void failed(Contributor contributor, HookReporter reporter) {}
  }

  public static final class Printer implements Reporter {
    @Override
    public void report(Event event) {}
  }

  abstract static class Pricing implements ItineraWorkflow {
    @InputAdapter(
        steps = {"charge", "charge_rescue"},
        name = "pricing")
    public Supplied priced(
        @Key String key,
        @FromWorkflow("price") long price,
        DataBagAccess access,
        StepName step,
        JourneyId journey) {
      return key.equals("amount") ? supply(price) : fromDataBag();
    }
  }

  @Workflow(name = "shop", policies = Tell.class, reporters = Printer.class)
  @WithStep(step = Charge.class, name = "charge", rescue = Renew.class, policies = Audit.class)
  @WithStep(step = Ship.class, name = "ship", retryBudget = 2, noRetryOnAbnormalTermination = true)
  public static final class Shop extends Pricing implements Customers {
    @Override
    public void tell(String message) {}

    @Provider(
        perAttempt = true,
        steps = {"charge", "charge_rescue"},
        key = "payments",
        name = "gateway")
    public Provided<Payments> gateway(
        @FromWorkflow("currency") String currency,
        Attempt attempt,
        StepName step,
        ProviderReporter reporter) {
      return provide(amount -> amount + " " + currency);
    }

    @Provider(key = "receipts")
    public Provided<String> receipts(JourneyId journey) {
      return noInstance();
    }

    @IdGenerator
    public String id(DataBag data) {
      return "order";
    }
  }

  @WithStep(step = KotlinCharge.class, name = "charge in Kotlin")
  public static final class ChargeInKotlin implements ItineraWorkflow {
    @Provider(key = "payments")
    public Provided<Payments> gateway() {
      return noInstance();
    }
  }

  @StepPolicy(name = "audit in Kotlin")
  public static final class KotlinAudit extends KotlinAuditing {}

  @WithStep(step = Ship.class, name = "ship", policies = KotlinAudit.class)
  public static final class AuditInKotlin implements ItineraWorkflow {
    @Provider(key = "receipts")
    public Provided<String> receipts() {
      return noInstance();
    }
  }
}

package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.HookReporter;
import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.KeepDefault;
import org.itineradev.itinera.policy.OnFailure;
import org.itineradev.itinera.policy.OnStepFailure;
import org.itineradev.itinera.policy.OnStepRetry;
import org.itineradev.itinera.policy.OnStepSuccess;
import org.itineradev.itinera.policy.OnSuccess;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.RetryCause;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

/** Mistakes in policy classes, each in a workflow of its own. */
public final class Policies {
  private Policies() {}

  @Role
  public interface Customers {
    void tell(String message);
  }

  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @StepPolicy(name = "audit")
  public static final class Audit implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@FromStep("charged") long charged, Contributor contributor) {
      contributor.contribute("audit", "charged " + charged);
      return keepDefault();
    }

    @OnStepFailure
    public OnFailure failed(FailureCause cause, Reason reason) {
      return keepDefault();
    }

    @OnStepRetry
    public OnFailure retried(
        JourneyId journey,
        StepName step,
        Attempt attempt,
        RetryCause cause,
        Throwable error,
        HookReporter reporter) {
      return keepDefault();
    }
  }

  @WorkflowPolicy(name = "tell")
  public static final class Tell implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(@Role Customers customers, @FromWorkflow("order") String order) {
      customers.tell(order + " is paid");
    }
  }

  @Workflow(name = "orders", policies = Tell.class)
  @WithStep(step = Charge.class, name = "charge", policies = Audit.class)
  public static final class Orders implements ItineraWorkflow, Customers {
    final List<String> told = new ArrayList<>();

    @Override
    public void tell(String message) {
      told.add(message);
    }
  }

  @WorkflowPolicy(name = "step name")
  public static final class StepNamePolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(StepName step) {}
  }

  @StepPolicy(name = "retried")
  public static final class RetriedPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(RetryCause cause) {
      return keepDefault();
    }
  }

  @WorkflowPolicy(name = "error")
  public static final class ErrorPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(Throwable error) {}
  }

  @WorkflowPolicy(name = "step hook")
  public static final class StepHookPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}

    @OnStepSuccess
    public OnSuccess charged() {
      return KeepDefault.INSTANCE;
    }
  }

  public static final class UnmarkedWorkflowPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}
  }

  @StepPolicy(name = "unvalued")
  public static final class UnvaluedPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@FromStep("charged") LinkedList<Long> charged) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "keyless")
  public static final class KeylessPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@FromStep long charged) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "hidden twice")
  public static final class HiddenTwicePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    @OnStepFailure
    KeepDefault charged() {
      return keepDefault();
    }
  }

  public static class Hiding implements ItineraStepPolicy {
    @OnStepSuccess
    OnSuccess charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "hidden above")
  public static final class HiddenAbovePolicy extends Hiding {
    @OnStepFailure
    public OnFailure failed() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "input")
  public static final class InputPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@Input("amount") long amount) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "service")
  public static final class ServicePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@Service("payments") Runnable payments) {
      return keepDefault();
    }
  }

  @Workflow(name = "step name", policies = StepNamePolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class StepNameInWorkflow implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = RetriedPolicy.class)
  public static final class Retried implements ItineraWorkflow {}

  @Workflow(name = "error", policies = ErrorPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class ErrorInWorkflow implements ItineraWorkflow {}

  @Workflow(name = "step hook", policies = StepHookPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class StepHookInWorkflow implements ItineraWorkflow {}

  @Workflow(name = "unmarked", policies = UnmarkedWorkflowPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class UnmarkedWorkflow implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = UnvaluedPolicy.class)
  public static final class Unvalued implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = KeylessPolicy.class)
  public static final class Keyless implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = HiddenTwicePolicy.class)
  public static final class HiddenTwice implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = HiddenAbovePolicy.class)
  public static final class HiddenAbove implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = InputPolicy.class)
  public static final class InputInHook implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = ServicePolicy.class)
  public static final class ServiceInHook implements ItineraWorkflow {}

  public static final class UnmarkedPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "hookless")
  public static final class HooklessPolicy implements ItineraStepPolicy {}

  @StepPolicy(name = "hidden")
  public static final class HiddenPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    OnSuccess charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "double")
  public static final class TwoHooksPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    @OnStepFailure
    public KeepDefault charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "twice")
  public static final class TwicePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }

    @OnStepSuccess
    public OnSuccess chargedAgain() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "misplaced")
  public static final class MisplacedPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }

    @OnWorkflowSuccess
    public void paid() {}
  }

  @StepPolicy(name = "wrong return")
  public static final class WrongReturnPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnFailure charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "finishing")
  public static final class FinishingPolicy implements ItineraStepPolicy {
    @OnStepFailure
    public OnSuccess failed() {
      return OnSuccess.finishWorkflow();
    }
  }

  @StepPolicy(name = "constructed")
  public static final class ConstructedPolicy implements ItineraStepPolicy {
    ConstructedPolicy(String note) {}

    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "abstract")
  public abstract static class AbstractPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }
  }

  @StepPolicy(name = "nothing")
  public static final class NothingPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(String note) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "cause")
  public static final class CausePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(FailureCause cause) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "unmarked role")
  public static final class UnmarkedRolePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@Role Runnable role) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "optional")
  public static final class OptionalPolicy implements ItineraStepPolicy {
    @OnStepFailure
    public OnFailure failed(Optional<Reason> reason) {
      return keepDefault();
    }
  }

  @StepPolicy(name = "audit again")
  public static final class AuditAgain implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }
  }

  @WorkflowPolicy(name = "from step")
  public static final class FromStepPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(@FromStep("charged") long charged) {}
  }

  @WorkflowPolicy(name = "returning")
  public static final class ReturningPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public String paid() {
      return "paid";
    }
  }

  @WorkflowPolicy(name = "reason")
  public static final class ReasonPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(Reason reason) {}
  }

  @WithStep(step = Charge.class, name = "charge", policies = UnmarkedPolicy.class)
  public static final class Unmarked implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = HooklessPolicy.class)
  public static final class Hookless implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = HiddenPolicy.class)
  public static final class Hidden implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = TwoHooksPolicy.class)
  public static final class TwoHooks implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = TwicePolicy.class)
  public static final class Twice implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = MisplacedPolicy.class)
  public static final class Misplaced implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = WrongReturnPolicy.class)
  public static final class WrongReturn implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = FinishingPolicy.class)
  public static final class Finishing implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = ConstructedPolicy.class)
  public static final class Constructed implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = AbstractPolicy.class)
  public static final class Abstract implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = NothingPolicy.class)
  public static final class Nothing implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = CausePolicy.class)
  public static final class Cause implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = UnmarkedRolePolicy.class)
  public static final class UnmarkedRole implements ItineraWorkflow {}

  @WithStep(step = Charge.class, name = "charge", policies = OptionalPolicy.class)
  public static final class Optionally implements ItineraWorkflow {}

  @WithStep(
      step = Charge.class,
      name = "charge",
      policies = {Audit.class, AuditAgain.class})
  public static final class Audited implements ItineraWorkflow {}

  @Workflow(name = "untold", policies = Tell.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class Untold implements ItineraWorkflow {}

  @Workflow(name = "from step", policies = FromStepPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class FromStepInWorkflow implements ItineraWorkflow {}

  @Workflow(name = "returning", policies = ReturningPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class Returning implements ItineraWorkflow {}

  @Workflow(name = "reason", policies = ReasonPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class ReasonInWorkflow implements ItineraWorkflow {}

  @WorkflowPolicy(name = "inner")
  public final class InnerPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}
  }

  @Workflow(name = "inner", policies = InnerPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class InnerPolicyWorkflow implements ItineraWorkflow {}

  @WorkflowPolicy(name = "bound")
  public static final class BoundPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public <C extends Customers> void paid(@Role C customers, Contributor contributor) {}
  }

  @Workflow(name = "bound", policies = BoundPolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class BoundRole implements ItineraWorkflow, Customers {
    @Override
    public void tell(String message) {}
  }

  @WorkflowPolicy(name = "array role")
  public static final class ArrayRolePolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(@Role String[] customers) {}
  }

  @Workflow(name = "array role", policies = ArrayRolePolicy.class)
  @WithStep(step = Charge.class, name = "charge")
  public static final class ArrayRole implements ItineraWorkflow {}

  @StepPolicy(name = "bound outcome")
  public static final class BoundOutcomePolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public <S extends OnSuccess> S charged() {
      return null;
    }
  }

  @WithStep(step = Charge.class, name = "charge", policies = BoundOutcomePolicy.class)
  public static final class BoundOutcome implements ItineraWorkflow {}
}

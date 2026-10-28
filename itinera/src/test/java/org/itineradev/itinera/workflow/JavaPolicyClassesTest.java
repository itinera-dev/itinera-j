package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
import org.itineradev.itinera.policy.PolicyName;
import org.itineradev.itinera.policy.RetryCause;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.StepPolicyDescriptor;
import org.itineradev.itinera.policy.SyncHook;
import org.itineradev.itinera.policy.SyncWorkflowHook;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.StepHookKind;
import org.itineradev.itinera.request.WorkflowHookKind;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaPolicyClassesTest {
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

  @Test
  void javaHooksRequestWhatTheirParametersNameInOrder() {
    var orders = WorkflowDescriptor.of(Orders.class);
    var audit = orders.getSteps().get(0).getPolicies().get(0);
    var tell = orders.getPolicies().get(0);
    assertEquals(PolicyName.of("audit"), audit.getName());
    assertEquals(
        List.of("data from the step charged: Long", "contributor"),
        descriptions(audit.getOnStepSuccess().getRequests()));
    assertEquals(
        List.of("failure cause", "reason"), descriptions(audit.getOnStepFailure().getRequests()));
    assertEquals(
        List.of("journey ID", "step name", "attempt", "retry cause", "error", "hook reporter"),
        descriptions(audit.getOnStepRetry().getRequests()));
    assertEquals(
        List.of("role Customers", "data from the workflow order: String?"),
        descriptions(tell.getOnWorkflowSuccess().getRequests()));
    assertEquals(List.of(Customers.class), orders.getRoles());
  }

  @Test
  void javaHooksAreCalledOnAPolicyTheirClassBuilds() {
    var orders = WorkflowDescriptor.of(Orders.class);
    var contributed = new ArrayList<String>();
    Contributor contributor = (key, value) -> contributed.add(key + " " + value);
    assertEquals(
        KeepDefault.INSTANCE,
        succeeded(orders.getSteps().get(0).getPolicies().get(0), 40L, contributor));
    assertEquals(List.of("audit charged 40"), contributed);
    var shop = new Orders();
    paid(orders.getPolicies().get(0), shop, "order-1");
    assertEquals(List.of("order-1 is paid"), shop.told);
  }

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInAJavaPolicyClassIsRefusedWhenItIsDescribed(
      Class<? extends ItineraWorkflow> workflow, List<String> problems) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        problems, refused.getViolations().stream().map(JavaPolicyClassesTest::problem).toList());
  }

  @SuppressWarnings("unchecked")
  private static <P> OnSuccess succeeded(StepPolicyDescriptor<P, Sync> policy, Object... answers) {
    var hook = policy.getOnStepSuccess();
    return ((SyncHook<P, StepHookKind<Sync>, OnSuccess>) hook.getCall())
        .call(policy.getFactory().build(), answered(hook.getRequests(), answers));
  }

  @SuppressWarnings("unchecked")
  private static <P> void paid(WorkflowPolicyDescriptor<P, Sync> policy, Object... answers) {
    var hook = policy.getOnWorkflowSuccess();
    ((SyncWorkflowHook<P, WorkflowHookKind<Sync>>) hook.getCall())
        .call(policy.getFactory().build(), answered(hook.getRequests(), answers));
  }

  private static <K> Answers<K> answered(List<? extends Request<?, ?>> requests, Object[] answers) {
    return new Answers<>(
        IntStream.range(0, requests.size())
            .boxed()
            .collect(Collectors.toMap(requests::get, at -> answers[at])));
  }

  private static List<String> descriptions(List<? extends Request<?, ?>> requests) {
    return requests.stream().map(Request::toString).toList();
  }

  private static String problem(Violation violation) {
    return violation instanceof Violation.Misdeclared misdeclared
        ? misdeclared.problem()
        : violation.getNamed();
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a policy class not marked @StepPolicy",
            Unmarked.class,
            List.of("is not marked @StepPolicy")),
        Arguments.argumentSet(
            "a policy that defines no hook",
            Hookless.class,
            List.of("defines no hook, though a policy defines at least one")),
        Arguments.argumentSet(
            "a hook that is not public",
            Hidden.class,
            List.of(
                "is marked @OnStepSuccess, though a hook is a public method",
                "defines no hook, though a policy defines at least one")),
        Arguments.argumentSet(
            "a method marked as two hooks",
            TwoHooks.class,
            List.of(
                "is marked @OnStepSuccess and @OnStepFailure, though a method is one hook",
                "defines no hook, though a policy defines at least one")),
        Arguments.argumentSet(
            "one hook defined in two methods",
            Twice.class,
            List.of(
                "defines on step success in several methods, though a policy defines each hook once")),
        Arguments.argumentSet(
            "a workflow hook in a step policy",
            Misplaced.class,
            List.of("is marked @OnWorkflowSuccess, which only a workflow policy defines")),
        Arguments.argumentSet(
            "a success hook returning what a failure hook does",
            WrongReturn.class,
            List.of("returns OnFailure, not OnSuccess")),
        Arguments.argumentSet(
            "a policy class that cannot be built with nothing",
            Constructed.class,
            List.of("has no constructor that takes nothing, which a policy is built with")),
        Arguments.argumentSet(
            "an abstract policy class", Abstract.class, List.of("cannot be built: it is abstract")),
        Arguments.argumentSet(
            "a parameter that requests nothing",
            Nothing.class,
            List.of(
                "has a parameter that requests nothing, arg0: mark data @FromStep or "
                    + "@FromWorkflow and a role @Role, and request the rest by its type")),
        Arguments.argumentSet(
            "why the step failed, in on step success",
            Cause.class,
            List.of("requests why the step failed, which only on step failure can")),
        Arguments.argumentSet(
            "a role that is not marked @Role",
            UnmarkedRole.class,
            List.of(
                "requests a role of java.lang.Runnable, which is not an interface marked @Role")),
        Arguments.argumentSet(
            "an Optional reason without itinera-java",
            Optionally.class,
            List.of(
                "requests an Optional, which itinera reads only with the artifact "
                    + "org.itinera-dev:itinera-java: add it to the application's dependencies")),
        Arguments.argumentSet(
            "two step policies of one step defining one hook",
            Audited.class,
            List.of("hook defined twice")),
        Arguments.argumentSet(
            "a role the workflow does not implement", Untold.class, List.of("role not provided")),
        Arguments.argumentSet(
            "data from the step in a workflow hook",
            FromStepInWorkflow.class,
            List.of("requests data from the step, which only a step hook can")),
        Arguments.argumentSet(
            "a workflow hook that returns something",
            Returning.class,
            List.of("returns String, though a workflow hook returns nothing")),
        Arguments.argumentSet(
            "a reason in a workflow hook",
            ReasonInWorkflow.class,
            List.of("requests the failure's reason, which only a step hook can")),
        Arguments.argumentSet(
            "the step's name in a workflow hook",
            StepNameInWorkflow.class,
            List.of("requests the step's name, which only a step hook can")),
        Arguments.argumentSet(
            "why the step is retried, in on step success",
            Retried.class,
            List.of("requests why the step is retried, which only on step retry can")),
        Arguments.argumentSet(
            "the error in a workflow hook",
            ErrorInWorkflow.class,
            List.of("requests the error, which only a step hook can")),
        Arguments.argumentSet(
            "a step hook in a workflow policy",
            StepHookInWorkflow.class,
            List.of("is marked @OnStepSuccess, which only a step policy defines")),
        Arguments.argumentSet(
            "a policy class not marked @WorkflowPolicy",
            UnmarkedWorkflow.class,
            List.of("is not marked @WorkflowPolicy")),
        Arguments.argumentSet(
            "data of a type no value can be",
            Unvalued.class,
            List.of(
                "requests charged: No value is a java.util.LinkedList: a value's lists, sets and "
                    + "maps are read as List, Set and Map, or as ArrayList, LinkedHashSet and "
                    + "LinkedHashMap.")),
        Arguments.argumentSet(
            "data whose key is not written and whose name was not kept",
            Keyless.class,
            List.of(
                "names no key for arg0, whose name was not kept: write the key, or compile with "
                    + "-parameters")),
        Arguments.argumentSet(
            "a hook that is not public, marked as two hooks",
            HiddenTwice.class,
            List.of(
                "is marked @OnStepSuccess and @OnStepFailure, though a hook is a public method",
                "defines no hook, though a policy defines at least one")),
        Arguments.argumentSet(
            "a hook that is not public, in a superclass",
            HiddenAbove.class,
            List.of("is marked @OnStepSuccess, though a hook is a public method")),
        Arguments.argumentSet(
            "an input in a hook",
            InputInHook.class,
            List.of(
                "marks arg0 @Input, though a hook requests data with @FromStep or @FromWorkflow")),
        Arguments.argumentSet(
            "a service in a hook",
            ServiceInHook.class,
            List.of("marks arg0 @Service, though a hook never requests a service")));
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
}

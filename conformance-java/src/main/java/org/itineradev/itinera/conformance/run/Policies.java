package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookScript;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.PolicyBuild;
import org.itineradev.itinera.conformance.model.PolicyKind;
import org.itineradev.itinera.conformance.model.PolicyModel;
import org.itineradev.itinera.policy.PolicyFactory;
import org.itineradev.itinera.policy.StepHook;
import org.itineradev.itinera.policy.StepPolicyBuilder;
import org.itineradev.itinera.policy.StepPolicyDescriptor;
import org.itineradev.itinera.policy.WorkflowHook;
import org.itineradev.itinera.policy.WorkflowPolicyBuilder;
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor;
import org.itineradev.itinera.policy.testing.StepPolicyMock;
import org.itineradev.itinera.policy.testing.StepPolicyScript;
import org.itineradev.itinera.policy.testing.WorkflowPolicyMock;
import org.itineradev.itinera.policy.testing.WorkflowPolicyScript;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.HookKind;
import org.itineradev.itinera.request.StepFailureKind;
import org.itineradev.itinera.request.StepHookKind;
import org.itineradev.itinera.request.StepRetryKind;
import org.itineradev.itinera.request.WorkflowHookKind;
import org.itineradev.itinera.workflow.Sync;

/** The policies a scenario scripts, put together with the builders, each hook watched. */
final class Policies {
  private Policies() {}

  /** Whether {@code policy} is attached to steps. */
  static boolean isStepPolicy(PolicyModel policy) {
    return policy.kind() == PolicyKind.STEP;
  }

  /** The step policy among {@code policies} that the case attaches as {@code name}. */
  static <D> D attached(Map<String, D> policies, String name) {
    var policy = policies.get(name);
    if (policy == null) {
      throw new ModelException("no step policy \"" + name + "\" is defined");
    }
    return policy;
  }

  /** The step policy {@code policy} scripts, its calls witnessed by {@code witness}. */
  static StepPolicyDescriptor<?, Sync> stepPolicyOf(PolicyModel policy, Witness witness) {
    var playing =
        policy.hooks().entrySet().stream()
            .reduce(
                builtAs(StepPolicyScript.keepingDefault(), policy.build()),
                Policies::stepEndingOf,
                (earlier, later) -> later);
    StepPolicyBuilder<Played<StepPolicyMock>> builder =
        StepPolicyDescriptor.builder(
            policy.name(), madeAs(policy.build(), () -> new Played<>(playing.build())));
    for (var hook : policy.hooks().entrySet()) {
      define(builder, policy.name(), hook.getKey(), hook.getValue(), witness);
    }
    return builder.build();
  }

  /** The workflow policy {@code policy} scripts, its calls witnessed by {@code witness}. */
  static WorkflowPolicyDescriptor<?, Sync> workflowPolicyOf(PolicyModel policy, Witness witness) {
    var playing =
        policy.hooks().entrySet().stream()
            .reduce(
                builtAs(WorkflowPolicyScript.returning(), policy.build()),
                Policies::workflowEndingOf,
                (earlier, later) -> later);
    WorkflowPolicyBuilder<Played<WorkflowPolicyMock>> builder =
        WorkflowPolicyDescriptor.builder(
            policy.name(), madeAs(policy.build(), () -> new Played<>(playing.build())));
    for (var hook : policy.hooks().entrySet()) {
      define(builder, policy.name(), hook.getKey(), hook.getValue(), witness);
    }
    return builder.build();
  }

  /** The step hook {@code hook} names. */
  static StepHook stepHookOf(Hook hook) {
    if (hook instanceof Hook.OfStep step) {
      return step.hook();
    }
    throw new ModelException(hook + " is not a step hook");
  }

  /** The workflow hook {@code hook} names. */
  static WorkflowHook workflowHookOf(Hook hook) {
    if (hook instanceof Hook.OfWorkflow workflow) {
      return workflow.hook();
    }
    throw new ModelException(hook + " is not a workflow hook");
  }

  private static void define(
      StepPolicyBuilder<Played<StepPolicyMock>> builder,
      String policy,
      Hook hook,
      HookScript script,
      Witness witness) {
    var returns = script.returns();
    switch (stepHookOf(hook)) {
      case ON_STEP_SUCCESS -> {
        var scripted = new ScriptedHook<StepHookKind<Sync>>(policy, hook, script);
        builder.onStepSuccess(
            scripted.declared(),
            HookEndings.returning(
                returns, (played, got) -> played(scripted, played, got, witness).onStepSuccess()));
      }
      case ON_STEP_FAILURE -> {
        var scripted = new ScriptedHook<StepFailureKind<Sync>>(policy, hook, script);
        builder.onStepFailure(
            scripted.declared(),
            HookEndings.returning(
                returns, (played, got) -> played(scripted, played, got, witness).onStepFailure()));
      }
      case ON_STEP_RETRY -> {
        var scripted = new ScriptedHook<StepRetryKind<Sync>>(policy, hook, script);
        builder.onStepRetry(
            scripted.declared(),
            HookEndings.returning(
                returns, (played, got) -> played(scripted, played, got, witness).onStepRetry()));
      }
      case ON_STEP_ABNORMAL_TERMINATION -> {
        var scripted = new ScriptedHook<StepHookKind<Sync>>(policy, hook, script);
        builder.onStepAbnormalTermination(
            scripted.declared(),
            HookEndings.returning(
                returns,
                (played, got) ->
                    played(scripted, played, got, witness).onStepAbnormalTermination()));
      }
    }
  }

  private static void define(
      WorkflowPolicyBuilder<Played<WorkflowPolicyMock>> builder,
      String policy,
      Hook hook,
      HookScript script,
      Witness witness) {
    var scripted = new ScriptedHook<WorkflowHookKind<Sync>>(policy, hook, script);
    var returns = script.returns();
    switch (workflowHookOf(hook)) {
      case ON_WORKFLOW_SUCCESS ->
          builder.onWorkflowSuccess(
              scripted.declared(),
              HookEndings.workflowReturning(
                  returns,
                  (played, got) -> played(scripted, played, got, witness).onWorkflowSuccess()));
      case ON_WORKFLOW_FAILURE ->
          builder.onWorkflowFailure(
              scripted.declared(),
              HookEndings.workflowReturning(
                  returns,
                  (played, got) -> played(scripted, played, got, witness).onWorkflowFailure()));
    }
  }

  /**
   * Plays a call of {@code hook} on {@code played} with what {@code got} gives it, and gives the
   * mock that ends it.
   */
  private static <K extends HookKind<Sync>, T> T played(
      ScriptedHook<K> hook, Played<T> played, Got<K> got, Witness witness) {
    var received = hook.receivedFrom(got, witness);
    for (var action : hook.actions()) {
      HookActing.perform(action, received, played, witness);
    }
    return played.mock();
  }

  /** {@code factory}, or one returning {@code null}, not a policy, when {@code build} says so. */
  private static <P> PolicyFactory<P> madeAs(PolicyBuild build, PolicyFactory<P> factory) {
    return build instanceof PolicyBuild.NullLike ? () -> null : factory;
  }

  private static StepPolicyScript builtAs(StepPolicyScript script, PolicyBuild build) {
    return build instanceof PolicyBuild.Fails fails
        ? script.failingToBuild(messageOf(fails))
        : script;
  }

  private static WorkflowPolicyScript builtAs(WorkflowPolicyScript script, PolicyBuild build) {
    return build instanceof PolicyBuild.Fails fails
        ? script.failingToBuild(messageOf(fails))
        : script;
  }

  private static String messageOf(PolicyBuild.Fails fails) {
    return fails.message() == null ? HookEndings.UNWRITTEN_FAILURE : fails.message();
  }

  private static StepPolicyScript stepEndingOf(
      StepPolicyScript script, Map.Entry<Hook, HookScript> hooked) {
    var hook = hooked.getKey();
    var returns = hooked.getValue().returns();
    return switch (stepHookOf(hook)) {
      case ON_STEP_SUCCESS -> script.onStepSuccess(HookEndings.successEnding(returns));
      case ON_STEP_FAILURE -> script.onStepFailure(HookEndings.failureEnding(returns, hook));
      case ON_STEP_RETRY -> script.onStepRetry(HookEndings.failureEnding(returns, hook));
      case ON_STEP_ABNORMAL_TERMINATION ->
          script.onStepAbnormalTermination(HookEndings.failureEnding(returns, hook));
    };
  }

  private static WorkflowPolicyScript workflowEndingOf(
      WorkflowPolicyScript script, Map.Entry<Hook, HookScript> hooked) {
    var hook = hooked.getKey();
    var ending = HookEndings.workflowEnding(hooked.getValue().returns(), hook);
    return switch (workflowHookOf(hook)) {
      case ON_WORKFLOW_SUCCESS -> script.onWorkflowSuccess(ending);
      case ON_WORKFLOW_FAILURE -> script.onWorkflowFailure(ending);
    };
  }
}

package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.itineradev.itinera.conformance.model.ProviderRequest;
import org.itineradev.itinera.conformance.model.RoleOperation;
import org.itineradev.itinera.step.SyncRescueFactory;
import org.itineradev.itinera.step.SyncStepFactory;

/**
 * Every step and rescue step the scenario's workflow built, by name, in the order built, how many
 * ran, what each service provider received on each call, every call of a hook and of a role's
 * operation.
 */
public final class Witness {
  private final Map<String, List<Built>> built = new LinkedHashMap<>();
  private final Map<String, List<Map<ProviderRequest, Object>>> calls = new LinkedHashMap<>();
  private final List<HookCall> hookCalls = new ArrayList<>();
  private final List<RoleOperation> roleCalls = new ArrayList<>();
  private int ran;

  /** How many built steps ran. */
  public int ran() {
    return ran;
  }

  /** The steps built under {@code step}, in the order they were built. */
  public List<Built> built(String step) {
    return List.copyOf(built.getOrDefault(step, List.of()));
  }

  /** What the provider {@code provider} received on each call, in order. */
  public List<Map<ProviderRequest, Object>> calls(String provider) {
    return List.copyOf(calls.getOrDefault(provider, List.of()));
  }

  /** Every call of a hook, in the order made. */
  public List<HookCall> hooksCalled() {
    return List.copyOf(hookCalls);
  }

  /** How many times {@code operation} of the role {@code role} was called. */
  public int roleCalls(String role, String operation) {
    return (int) roleCalls.stream().filter(new RoleOperation(role, operation)::equals).count();
  }

  /** Records a call of a hook. */
  void hookCalled(HookCall call) {
    hookCalls.add(call);
  }

  /** Records a call of {@code operation} of the role {@code role}. */
  void roleCalled(String role, String operation) {
    roleCalls.add(new RoleOperation(role, operation));
  }

  /** Records a call of the provider {@code provider}, which received {@code received}. */
  void called(String provider, Map<ProviderRequest, Object> received) {
    calls.computeIfAbsent(provider, uncalled -> new ArrayList<>()).add(received);
  }

  /**
   * A factory building with {@code factory}, each step kept under {@code step} with what it
   * received as it is built, and counted as it runs, where it first acts as {@code scripted} says.
   * It returns {@code null} instead of the step, or of its outcome, when the script says so.
   */
  SyncStepFactory watching(String step, SyncStepFactory factory, Scripted scripted) {
    return got -> {
      var received = scripted.receivedFrom(got);
      var run = factory.build(got);
      keep(step, run, received);
      return scripted.builtAsNull() ? null : () -> ran(run::run, received, scripted);
    };
  }

  /**
   * A factory building rescue steps with {@code factory}, each kept under {@code step} with what it
   * received as it is built, and counted as it runs, where it first acts as {@code scripted} says.
   * It returns {@code null} instead of the rescue step, or of its outcome, when the script says so.
   */
  SyncRescueFactory watchingRescue(String step, SyncRescueFactory factory, Scripted scripted) {
    return got -> {
      var received = scripted.rescueReceivedFrom(got);
      var run = factory.build(got);
      keep(step, run, received);
      return scripted.builtAsNull() ? null : () -> ran(run::run, received, scripted);
    };
  }

  private void keep(String step, Object run, Received received) {
    built.computeIfAbsent(step, unbuilt -> new ArrayList<>()).add(new Built(run, received));
  }

  private <O> O ran(Supplier<O> run, Received received, Scripted scripted) {
    ran += 1;
    scripted.act(received);
    var outcome = run.get();
    return scripted.returnsNoOutcome() ? null : outcome;
  }
}

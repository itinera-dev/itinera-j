package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.run.HookCall;

/** What the calls of a hook received, as the sentences read it. */
final class HooksReceived {
  private HooksReceived() {}

  /**
   * Checks that a call of {@code hook} of {@code policy} received {@code expected} for the requests
   * {@code asks} picks.
   */
  static void received(
      World world, String hook, String policy, Predicate<HookRequest> asks, Object expected) {
    var values = receivedFor(world, hook, policy, asks);
    holds(
        values.contains(expected),
        "\"" + hook + "\" of \"" + policy + "\" to receive " + expected,
        values.toString());
  }

  /** Checks that a call of {@code hook} of {@code policy} received a value written as given. */
  static void receivedWritten(
      World world, String hook, String policy, Predicate<HookRequest> asks, String expected) {
    var values = receivedFor(world, hook, policy, asks).stream().map(String::valueOf).toList();
    holds(
        values.contains(expected),
        "\"" + hook + "\" of \"" + policy + "\" to receive " + expected,
        values.toString());
  }

  /** What every call of {@code hook} of {@code policy} received for the requests {@code asks}. */
  static List<Object> receivedFor(
      World world, String hook, String policy, Predicate<HookRequest> asks) {
    return callsOf(world, hook, policy).stream()
        .map(call -> receivedBy(call, asks))
        .flatMap(Collection::stream)
        .toList();
  }

  /** The calls of {@code hook} of {@code policy}, in the order they were made. */
  static List<HookCall> callsOf(World world, String hook, String policy) {
    var named = Hook.named(hook);
    return world.witness().hooksCalled().stream()
        .filter(call -> isCallOf(call, named, policy))
        .toList();
  }

  private static boolean isCallOf(HookCall call, Hook hook, String policy) {
    return call.hook().equals(hook) && call.policy().equals(policy);
  }

  private static Collection<Object> receivedBy(HookCall call, Predicate<HookRequest> asks) {
    return call.received().entrySet().stream()
        .filter(entry -> asks.test(entry.getKey()))
        .map(Map.Entry::getValue)
        .toList();
  }
}

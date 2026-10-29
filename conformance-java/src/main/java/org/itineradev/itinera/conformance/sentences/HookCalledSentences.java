package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.HooksReceived.callsOf;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.Map;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.run.HookCall;
import org.itineradev.itinera.conformance.trace.Writing;
import org.itineradev.itinera.event.ContributionCommitted;
import org.itineradev.itinera.event.Event;

/** The sentences that read which hooks were called, in what order, and what they reached. */
public final class HookCalledSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookCalledSentences(World world) {
    this.world = world;
  }

  @Then("the hook {string} of policy {string} was called {int} times")
  public void wasCalled(String hook, String policy, int times) {
    var calls = callsOf(world, hook, policy).size();
    holds(
        calls == times,
        "\"" + hook + "\" of \"" + policy + "\" called " + times + " times",
        calls + " calls");
  }

  @Then("the hook {string} of policy {string} was not called")
  public void wasNotCalled(String hook, String policy) {
    wasCalled(hook, policy, 0);
  }

  @Then("the hooks were called in this order:")
  public void calledInThisOrder(DataTable table) {
    var expected = table.asMaps().stream().map(HookCalledSentences::calledAs).toList();
    var found = world.witness().hooksCalled().stream().map(HookCalledSentences::calledAs).toList();
    holds(found.equals(expected), "the hooks called in the order " + expected, found.toString());
  }

  @Then("the operation {string} of the role {string} was called {int} times")
  public void operationWasCalled(String operation, String role, int times) {
    var calls = world.witness().roleCalls(role, operation);
    holds(
        calls == times,
        "\"" + operation + "\" of \"" + role + "\" called " + times + " times",
        calls + " calls");
  }

  @Then("the contribution of {string} is recorded as made by the hook {string} of policy {string}")
  public void contributionMadeByTheHook(String key, String hook, String policy) {
    var events = world.stream();
    var source = policy + ", " + hook;
    holdsOfStream(
        events.stream().anyMatch(event -> madeBy(event, key, source)),
        "\"" + key + "\" committed as made by \"" + hook + "\" of \"" + policy + "\"",
        events);
  }

  private static Map.Entry<String, Hook> calledAs(Map<String, String> row) {
    return Map.entry(cell(row, "policy"), Hook.named(cell(row, "hook")));
  }

  private static Map.Entry<String, Hook> calledAs(HookCall call) {
    return Map.entry(call.policy(), call.hook());
  }

  private static boolean madeBy(Event event, String key, String source) {
    return event instanceof ContributionCommitted committed
        && committed.key().equals(key)
        && source.equals(Writing.lineOf(event).cells().get("source"));
  }
}

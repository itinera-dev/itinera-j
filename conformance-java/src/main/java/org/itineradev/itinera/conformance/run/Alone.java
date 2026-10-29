package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.journey.testing.Contributions;
import org.itineradev.itinera.journey.testing.TestDataBag;
import org.itineradev.itinera.policy.testing.HookEventReporter;
import org.itineradev.itinera.step.testing.StepEventReporter;
import org.itineradev.itinera.workflow.Supplied;

/** How a step, a hook or an input adapter is called alone, outside any journey, as a test does. */
public final class Alone {
  private Alone() {}

  /**
   * Builds the scripted step {@code step} outside any journey and runs it, doing what it does on
   * attempt 1, with a contributor lent by the contributions recorder and a step reporter lent by
   * the step event reporter, failing when {@code model} says so.
   *
   * @throws ModelException when the step requests an input or a service, which a step run alone is
   *     not given
   */
  public static RanAlone stepRunAlone(Model model, String step) {
    var script = model.step(step);
    if (!script.inputs().isEmpty() || !script.services().isEmpty()) {
      throw new ModelException("step \"" + step + "\" requests what a step run alone is not given");
    }
    var contributions = new Contributions();
    var events =
        model.testing().stepRecorderFails() ? StepEventReporter.failing() : new StepEventReporter();
    var calls = new CarryingOn();
    var reporter = new CarriedOnStepReporter(events.getReporter(), calls);
    var received =
        new Received(Map.of(), Map.of(), contributions.getContributor(), reporter, 0, Map.of());
    new Scripted(script).act(received);
    return new RanAlone(
        "step \"" + step + "\"",
        "step",
        contributions,
        events.getEmitted(),
        events.getGlobalMetadata(),
        calls.calls());
  }

  /**
   * Calls the hook {@code hook} of the policy {@code policy} outside any journey, doing what it
   * does, with a contributor lent by the contributions recorder and a hook reporter lent by the
   * hook event reporter, failing when {@code model} says so.
   *
   * @throws ModelException when the policy does not define the hook, or the hook requests more than
   *     its handles, which a hook called alone is not given
   */
  public static RanAlone hookRunAlone(Model model, Witness witness, String policy, String hook) {
    var script = model.workflow().policy(policy).hook(Hook.named(hook));
    if (!script.requests().isEmpty()) {
      throw new ModelException(
          "the hook \""
              + hook
              + "\" of \""
              + policy
              + "\" requests what a hook run alone is not given");
    }
    var contributions = new Contributions();
    var events =
        model.testing().hookRecorderFails() ? HookEventReporter.failing() : new HookEventReporter();
    var calls = new CarryingOn();
    var reporter = new CarriedOnHookReporter(events.getReporter(), calls);
    var received = new HookReceived(Map.of(), contributions.getContributor(), reporter);
    var played = new Played<Void>(null);
    for (var action : script.actions()) {
      HookActing.perform(action, received, played, witness);
    }
    return new RanAlone(
        "the hook \"" + hook + "\" of policy \"" + policy + "\"",
        "hook",
        contributions,
        events.getEmitted(),
        events.getGlobalMetadata(),
        calls.calls());
  }

  /**
   * Calls the input adapter {@code adapter} outside any journey for {@code input} of {@code step},
   * with the read access of a data bag holding {@code values}, built with test support, and gives
   * what it supplied.
   */
  public static Supplied adapterCalledAlone(
      Model model,
      String adapter,
      String step,
      String input,
      List<Map.Entry<String, Object>> values) {
    var bag = values.stream().reduce(new TestDataBag(), Alone::holding, (earlier, later) -> later);
    return new Adapting(model.workflow().adapter(adapter))
        .suppliedAlone(step, input, bag.getAccess());
  }

  private static TestDataBag holding(TestDataBag bag, Map.Entry<String, Object> entry) {
    return bag.with(entry.getKey(), entry.getValue());
  }
}

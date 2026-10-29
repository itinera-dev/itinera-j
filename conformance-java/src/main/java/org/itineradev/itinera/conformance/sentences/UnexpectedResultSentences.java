package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HookScripts.ends;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.AdapterAnswer;
import org.itineradev.itinera.conformance.model.HookReturn;
import org.itineradev.itinera.conformance.model.IdGenerator;

/**
 * The sentences that script custom code returning a null-like value, or what its call cannot
 * return, which Java can write and Kotlin's types refuse.
 */
public final class UnexpectedResultSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public UnexpectedResultSentences(World world) {
    this.world = world;
  }

  @Given("step {string} returns no outcome")
  public void returnsNoOutcome(String step) {
    world.model().step(step).returnNoOutcome();
  }

  @Given("step {string} is built as a null-like value")
  public void isBuiltAsNull(String step) {
    world.model().step(step).buildAsNull();
  }

  @Given("the hook {string} of policy {string} returns a null-like value")
  public void hookReturnsNull(String hook, String policy) {
    ends(world, hook, policy, new HookReturn.NullLike());
  }

  @Given("the hook {string} of policy {string} returns something that is not a lifecycle")
  public void hookReturnsWhatIsNotALifecycle(String hook, String policy) {
    ends(world, hook, policy, new HookReturn.NotALifecycle());
  }

  @Given("the input adapter {string} returns no result for {string}")
  public void adapterReturnsNoResult(String adapter, String key) {
    world.model().workflow().adapter(adapter).answers(key, new AdapterAnswer.NoResult(), null);
  }

  @Given("the service provider {string} returns a null-like value")
  public void providerReturnsNull(String provider) {
    world.model().workflow().provider(provider).returnNull();
  }

  @Given("the policy {string} is built as a null-like value")
  public void policyIsBuiltAsNull(String policy) {
    world.model().workflow().policy(policy).buildAsNull();
  }

  @Given("the workflow's ID generator returns something that is not a string")
  public void idGeneratorReturnsWhatIsNotAString() {
    world.model().generateIdsWith(new IdGenerator.Returns(null));
  }
}

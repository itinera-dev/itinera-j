package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;

import io.cucumber.java.en.Then;
import java.util.Objects;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.MadeLabeller;
import org.itineradev.itinera.conformance.run.Received;
import org.itineradev.itinera.conformance.run.Unmet;

/** The sentences that read what each step was built with: its inputs and its services. */
public final class BuiltSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public BuiltSentences(World world) {
    this.world = world;
  }

  @Then("^step \"([^\"]*)\" was built with input \"([^\"]*)\" = (.+)$")
  public void wasBuiltWithInput(String step, String key, String written) {
    builtWithInput(step, 1, key, value(written));
  }

  @Then("step {string} was built with input {string} absent")
  public void wasBuiltWithInputAbsent(String step, String key) {
    var inputs = received(step, 1).inputs();
    holds(
        inputs.containsKey(key) && inputs.get(key) == null,
        "\"" + step + "\" built with \"" + key + "\" absent",
        "the inputs " + inputs);
  }

  @Then("^step \"([^\"]*)\" was built for attempt (\\d+) with input \"([^\"]*)\" = (.+)$")
  public void wasBuiltForAttemptWithInput(String step, int attempt, String key, String written) {
    builtWithInput(step, attempt, key, value(written));
  }

  @Then("step {string} was given the service {string} labelled {string} for attempt {int}")
  public void wasGivenTheService(String step, String key, String label, int attempt) {
    var service = received(step, attempt).services().get(key);
    holds(
        service instanceof MadeLabeller made && made.labelled().equals(label),
        "\"" + step + "\" given \"" + key + "\" labelled \"" + label + "\" for attempt " + attempt,
        String.valueOf(service));
  }

  private void builtWithInput(String step, int attempt, String key, Object expected) {
    var inputs = received(step, attempt).inputs();
    holds(
        inputs.containsKey(key) && Objects.equals(inputs.get(key), expected),
        "\"" + step + "\" built for attempt " + attempt + " with \"" + key + "\" = " + expected,
        "the inputs " + inputs);
  }

  private Received received(String step, int attempt) {
    var built = world.witness().built(step);
    if (built.size() < attempt) {
      throw new Unmet("expected \"" + step + "\" built for attempt " + attempt + "; it was not");
    }
    return built.get(attempt - 1).received();
  }
}

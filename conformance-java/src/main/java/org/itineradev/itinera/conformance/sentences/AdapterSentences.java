package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.AdapterAnswer;
import org.itineradev.itinera.conformance.model.AdapterModel;
import org.itineradev.itinera.conformance.model.AdapterRequest;
import org.itineradev.itinera.conformance.model.ValueType;

/** The sentences that script what each input adapter requests and supplies. */
public final class AdapterSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public AdapterSentences(World world) {
    this.world = world;
  }

  private AdapterModel adapter(String name) {
    return world.model().workflow().adapter(name);
  }

  @Given(
      "^the input adapter \"([^\"]*)\" returns "
          + "(?!nothing for |a non-value for |no result for |a throwing record for |\"[^\"]*\" of type )"
          + "(.+) for \"([^\"]*)\"$")
  public void returns(String adapter, String written, String key) {
    adapter(adapter).answers(key, new AdapterAnswer.Returns(value(written)), null);
  }

  @Given("the input adapter {string} returns nothing for {string}")
  public void returnsNothing(String adapter, String key) {
    adapter(adapter).answers(key, new AdapterAnswer.Nothing(), null);
  }

  @Given("the input adapter {string} fails with {string} for {string}")
  public void failsWith(String adapter, String message, String key) {
    adapter(adapter).answers(key, new AdapterAnswer.Fails(message), null);
  }

  @Given("the input adapter {string} returns a non-value for {string}")
  public void returnsANonValue(String adapter, String key) {
    adapter(adapter).answers(key, new AdapterAnswer.NonValue(), null);
  }

  @Given("the input adapter {string} requests data from the workflow {string} of type {word}")
  public void requestsDataFromTheWorkflow(String adapter, String key, String type) {
    adapter(adapter).requests().add(new AdapterRequest.FromWorkflow(key, ValueType.named(type)));
  }

  @Given("the input adapter {string} requests the step name and returns it for {string}")
  public void returnsTheStepName(String adapter, String key) {
    adapter(adapter).answers(key, new AdapterAnswer.ItsStepName(), null);
  }

  @Given("the input adapter {string} requests the data bag")
  public void requestsTheDataBag(String adapter) {
    adapter(adapter).requests().add(new AdapterRequest.DataBag());
  }

  @Given(
      "the input adapter {string} returns {string} of type {word}, read from the data bag, for {string}")
  public void returnsWhatItReads(String adapter, String key, String type, String input) {
    adapter(adapter)
        .answers(input, new AdapterAnswer.Reads(key, ValueType.named(type), false), null);
  }

  @Given(
      "the input adapter {string} keeps its data bag access when called for step {string}, "
          + "and returns {string} of type {word}, read through that kept access, "
          + "for {string} when called for step {string}")
  public void readsThroughKeptAccess(
      String adapter, String kept, String key, String type, String input, String step) {
    var scripted = adapter(adapter);
    scripted.keepsAccessOf(kept);
    scripted.answers(input, new AdapterAnswer.Reads(key, ValueType.named(type), true), step);
  }
}

package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Implementation;
import org.itineradev.itinera.conformance.model.ProviderFailure;
import org.itineradev.itinera.conformance.model.ProviderModel;
import org.itineradev.itinera.conformance.model.Supplies;

/** The sentences that script what each service provider does when called. */
public final class ProviderSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ProviderSentences(World world) {
    this.world = world;
  }

  private ProviderModel provider(String name) {
    return world.model().workflow().provider(name);
  }

  @Given(
      "the service provider {string} labels each instance with the value it received for {string}")
  public void labelsWith(String provider, String key) {
    provider(provider).labelledBy(key);
  }

  @Given("the service provider {string} fails with {string}")
  public void failsWith(String provider, String message) {
    provider(provider).fails(new ProviderFailure(message, null, null));
  }

  @Given("the service provider {string} fails with {string} when it receives {string} for {string}")
  public void failsWhenItReceives(String provider, String message, String received, String key) {
    provider(provider).fails(new ProviderFailure(message, key, received));
  }

  @Given("the service provider {string} makes instances of {implementation}")
  public void makesInstancesOf(String provider, Implementation implementation) {
    provider(provider).makes(implementation);
  }

  @Given("^the service provider \"([^\"]*)\" supplies the data (.+) with each instance$")
  public void suppliesTheData(String provider, String written) {
    provider(provider).supplies(new Supplies.Data(value(written)));
  }

  @Given("the service provider {string} supplies data that is not a value with each instance")
  public void suppliesANonValue(String provider) {
    provider(provider).supplies(new Supplies.NonValue());
  }

  @Given("the service provider {string} returns nothing")
  public void returnsNothing(String provider) {
    provider(provider).returnNothing();
  }

  @Given("the service provider {string} is asynchronous")
  public void isAsynchronous(String provider) {
    provider(provider).markAsynchronous();
  }

  @Given("the services the provider {string} makes fail with {string} when used")
  public void servicesFail(String provider, String message) {
    provider(provider).servicesFail(message);
  }
}

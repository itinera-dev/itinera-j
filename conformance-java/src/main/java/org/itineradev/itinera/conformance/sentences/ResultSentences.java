package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.JourneySentences.statusOf;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.Objects;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.conformance.trace.Status;
import org.itineradev.itinera.journey.Abort;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.journey.JourneyStatus;
import org.itineradev.itinera.value.TypeToken;

/** The sentences that read the result's abort and data bag. */
public final class ResultSentences {
  private static final TypeToken<Object> ANYTHING = TypeToken.ofNullable(Object.class);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ResultSentences(World world) {
    this.world = world;
  }

  @Then("the result's abort carries the error {string}")
  public void abortCarriesTheError(String message) {
    var found = abort();
    holds(
        found.error() != null && message.equals(found.errorMessage()),
        "an abort carrying the error \"" + message + "\"",
        found.toString());
  }

  @Then("the result's abort carries no error")
  public void abortCarriesNoError() {
    var found = abort();
    holds(found.error() == null, "an abort carrying no error", found.toString());
  }

  @Then(
      "the result names the step {string} as where the journey was aborted, with the abort reason {string}")
  public void namesTheAbortedStep(String step, String reason) {
    var found = abort();
    var events = world.stream();
    holdsOfStream(
        found.reason().toString().equals(reason)
            && events.stream().anyMatch(event -> Status.abortsAt(event, step)),
        "an abort \"" + reason + "\", whose journey_aborted names \"" + step + "\"",
        events);
  }

  private Abort abort() {
    var result = world.result();
    if (result.abort() == null) {
      throw new Unmet("expected the journey to be aborted; it " + statusOf(result));
    }
    return result.abort();
  }

  @Then("the result's data bag contains:")
  public void dataBagContains(DataTable table) {
    var bag = dataBag();
    for (var row : table.asMaps()) {
      var key = cell(row, "key");
      var expected = value(cell(row, "value"));
      var found = bag.read(key, ANYTHING).orNull();
      holds(
          bag.getKeys().contains(key) && Objects.equals(found, expected),
          "\"" + key + "\" = " + expected + " in the data bag",
          String.valueOf(found));
    }
  }

  @Then("the result's data bag has no key {string}")
  public void dataBagHasNoKey(String key) {
    var keys = dataBag().getKeys();
    holds(!keys.contains(key), "no \"" + key + "\" in the data bag", keys.toString());
  }

  @Then("the result carries no data bag")
  public void carriesNoDataBag() {
    var result = world.result();
    holds(result.isAborted(), "no data bag", "the data bag of a journey that " + statusOf(result));
  }

  private DataBag dataBag() {
    var status = world.result().status();
    if (status instanceof JourneyStatus.Succeeded succeeded) {
      return succeeded.dataBag();
    }
    if (status instanceof JourneyStatus.Failed failed) {
      return failed.dataBag();
    }
    throw new Unmet("expected a data bag; the journey was aborted");
  }
}

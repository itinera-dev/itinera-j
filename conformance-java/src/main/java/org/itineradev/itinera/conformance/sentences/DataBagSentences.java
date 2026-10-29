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
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.conformance.trace.Fields;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.journey.JourneyStatus;
import org.itineradev.itinera.value.TypeToken;

/** The sentences that read the result's data bag, and what the events show of it. */
public final class DataBagSentences {
  private static final TypeToken<Object> ANYTHING = TypeToken.ofNullable(Object.class);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public DataBagSentences(World world) {
    this.world = world;
  }

  @Then("the result's data bag contains:")
  public void dataBagContains(DataTable table) {
    contains(dataBag(world.result()), table);
  }

  @Then("the second journey's data bag contains:")
  public void secondDataBagContains(DataTable table) {
    contains(dataBag(secondResult()), table);
  }

  private JourneyResult secondResult() {
    var journeys = world.journeys();
    if (journeys.size() > 1 && journeys.get(1).ended() instanceof Ended.Returned returned) {
      return returned.result();
    }
    throw new Unmet("expected a second journey with a result; found " + journeys);
  }

  @Then("the result's data bag has no key {string}")
  public void dataBagHasNoKey(String key) {
    var keys = dataBag(world.result()).getKeys();
    holds(!keys.contains(key), "no \"" + key + "\" in the data bag", keys.toString());
  }

  @Then("the result carries no data bag")
  public void carriesNoDataBag() {
    var result = world.result();
    holds(result.isAborted(), "no data bag", "the data bag of a journey that " + statusOf(result));
  }

  @Then("no engine event carries the value of {string}")
  public void noEngineEventCarriesTheValueOf(String key) {
    var shown = String.valueOf(dataBag(world.result()).read(key, ANYTHING).orNull());
    var events = world.stream();
    holdsOfStream(
        events.stream()
            .filter(MetadataSentences::isEngineEvent)
            .noneMatch(event -> shows(event, shown)),
        "no engine event to carry " + shown + ", the value of \"" + key + "\"",
        events);
  }

  private static void contains(DataBag bag, DataTable table) {
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

  private static DataBag dataBag(JourneyResult result) {
    var status = result.status();
    if (status instanceof JourneyStatus.Succeeded succeeded) {
      return succeeded.dataBag();
    }
    if (status instanceof JourneyStatus.Failed failed) {
      return failed.dataBag();
    }
    throw new Unmet("expected a data bag; the journey was aborted");
  }

  private static boolean shows(Event event, String shown) {
    return Fields.apartFromIdAndTime(event).toString().contains(shown);
  }
}

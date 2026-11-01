package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Reported.madeAs;

import io.cucumber.java.en.Then;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Journey;
import org.itineradev.itinera.conformance.run.ScenarioReporter;
import org.itineradev.itinera.journey.JourneyId;

/** The sentences that check each journey's ID, and whether its instance was created. */
public final class JourneyIdSentences {
  private static final Pattern UUID_V4 =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
          Pattern.CASE_INSENSITIVE);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public JourneyIdSentences(World world) {
    this.world = world;
  }

  @Then("the journey ID is a UUID v4")
  public void isAUuid() {
    var found = world.journey().journeyId();
    holds(
        UUID_V4.matcher(Objects.requireNonNullElse(found, "")).matches(),
        "a UUID v4",
        "\"" + found + "\"");
  }

  @Then("the journey ID is {string}")
  public void journeyIdIs(String id) {
    var found = world.journey().journeyId();
    holds(id.equals(found), "the journey ID \"" + id + "\"", "\"" + found + "\"");
  }

  @Then("the result's journey ID is {string}")
  public void resultsJourneyIdIs(String id) {
    var found = world.result().journeyId().toString();
    holds(found.equals(id), "the result's journey ID \"" + id + "\"", "\"" + found + "\"");
  }

  @Then("the reporter {string} was made with the journey ID {string}")
  public void reporterMadeWith(String reporter, String id) {
    var found = madeAs(world, reporter).stream().map(ScenarioReporter::journeyId).toList();
    holds(
        found.equals(List.of(JourneyId.of(id))),
        "\"" + reporter + "\" made with the journey ID \"" + id + "\"",
        found.toString());
  }

  @Then("no journey ID was produced")
  public void noJourneyId() {
    var ids = world.journeys().stream().map(Journey::journeyId).filter(Objects::nonNull).toList();
    holds(ids.isEmpty(), "no workflow instance, so no journey ID", ids.toString());
  }

  @Then("creating the workflow instance failed")
  public void creatingTheInstanceFailed() {
    var ended = world.journey().ended();
    holds(
        ended instanceof Ended.NotCreated,
        "creating the workflow instance to fail",
        ended.toString());
  }

  @Then("the two journeys have different journey IDs")
  public void differentJourneyIds() {
    var ids = world.journeys().stream().map(Journey::journeyId).toList();
    holds(
        ids.size() == 2 && new HashSet<>(ids).size() == 2,
        "two journeys with different IDs",
        ids.toString());
  }
}

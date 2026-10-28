package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.Tables.names;
import static org.itineradev.itinera.conformance.trace.Fields.apartFromIdAndTime;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.trace.Line;
import org.itineradev.itinera.conformance.trace.Matching;
import org.itineradev.itinera.conformance.trace.Table;
import org.itineradev.itinera.conformance.trace.Writing;
import org.itineradev.itinera.event.ContributionCommitted;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.JourneyStarted;

/**
 * The sentences that compare the event stream with an event table, and read what it carries and how
 * it ends.
 */
public final class EventSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public EventSentences(World world) {
    this.world = world;
  }

  @Then("the events include, in order:")
  public void includeInOrder(DataTable table) {
    compare(table, Matching::includesInOrder, "to include, in order");
  }

  @Then("the events are exactly:")
  public void areExactlyThese(DataTable table) {
    compare(table, Matching::areExactly, "to be exactly");
  }

  private void compare(DataTable table, BiPredicate<List<Line>, List<Line>> agree, String how) {
    var expected = table.asMaps().stream().map(Table::expectedLine).toList();
    var lines = world.stream().stream().map(Writing::lineOf).toList();
    holds(agree.test(lines, expected), "the events " + how + " " + expected, lines.toString());
  }

  @Then(
      "every event carries the journey ID {string} and the workflow name {string}, with increasing sequence numbers")
  public void everyEventCarries(String id, String workflow) {
    var events = world.stream();
    holdsOfStream(
        !events.isEmpty()
            && events.stream().allMatch(event -> belongsTo(event, id, workflow))
            && IntStream.range(1, events.size()).allMatch(index -> inSequence(events, index)),
        "every event of \"" + workflow + "\" for \"" + id + "\", in increasing sequence",
        events);
  }

  @Then("no event carries the message {string}")
  public void noEventCarriesTheMessage(String message) {
    var events = world.stream();
    holdsOfStream(
        events.stream().map(Writing::lineOf).noneMatch(line -> carries(line, message)),
        "no event to carry \"" + message + "\"",
        events);
  }

  @Then("no contribution of {string} was committed")
  public void noContributionWasCommitted(String key) {
    var events = world.stream();
    holdsOfStream(
        events.stream().noneMatch(event -> commits(event, key)),
        "no contribution_committed of \"" + key + "\"",
        events);
  }

  @Then("no event was emitted")
  public void noEvent() {
    var events = world.stream();
    holdsOfStream(events.isEmpty(), "no event", events);
  }

  @Then("journey_aborted names no adapter")
  public void abortedNamesNoAdapter() {
    var events = world.stream();
    var aborted =
        events.stream().filter(JourneyAborted.class::isInstance).map(Writing::lineOf).toList();
    holdsOfStream(
        !aborted.isEmpty() && aborted.stream().noneMatch(EventSentences::namesAnAdapter),
        "journey_aborted naming no adapter",
        events);
  }

  @Then("the last event is {string}")
  public void lastEvent(String event) {
    var events = world.stream();
    holdsOfStream(
        !events.isEmpty() && events.get(events.size() - 1).getKind().toString().equals(event),
        "the last event to be " + event,
        events);
  }

  @Then(
      "^the journey_started event lists the initial keys (\"[^\"]*\"(?:, \"[^\"]*\")*) without their values$")
  public void listsTheInitialKeys(String keys) {
    var events = world.stream();
    var listed =
        events.stream()
            .filter(JourneyStarted.class::isInstance)
            .map(JourneyStarted.class::cast)
            .map(JourneyStarted::initialKeys)
            .findFirst();
    holdsOfStream(
        listed.equals(Optional.of(Set.copyOf(names(keys)))),
        "journey_started to list " + keys,
        events);
  }

  @Then(
      "the second journey's events are the first journey's events, apart from the journey ID and timestamps")
  public void secondJourneyRepeatsTheFirst() {
    var journeys = world.journeys();
    var first = apartFromIdAndTime(world.stream(journeys.get(0)));
    var second = apartFromIdAndTime(world.stream(journeys.get(1)));
    holds(first.equals(second), "the events " + first, second.toString());
  }

  private static boolean belongsTo(Event event, String id, String workflow) {
    return event.getJourneyId().toString().equals(id)
        && event.getWorkflow().toString().equals(workflow);
  }

  private static boolean inSequence(List<Event> events, int index) {
    return events.get(index - 1).getSequence() < events.get(index).getSequence();
  }

  private static boolean namesAnAdapter(Line line) {
    return line.cells().containsKey("adapter");
  }

  /** Whether {@code line} carries {@code message} in a cell or in what no column shows. */
  private static boolean carries(Line line, String message) {
    return line.cells().containsValue(message) || line.carried().contains(message);
  }

  private static boolean commits(Event event, String key) {
    return event instanceof ContributionCommitted committed && committed.key().equals(key);
  }
}

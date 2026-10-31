package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Streams.isOf;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.Reported.eventsOf;
import static org.itineradev.itinera.conformance.sentences.Reported.madeAs;
import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.java.en.Then;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.run.ScenarioReporter;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.EventKind;

/** The sentences that read the events each scripted reporter received. */
public final class ReceivedSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ReceivedSentences(World world) {
    this.world = world;
  }

  @Then("^the reporters (\"[^\"]*\"(?:, \"[^\"]*\")*) received the same events$")
  public void receivedTheSameEvents(String reporters) {
    var received =
        names(reporters).stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    this::events,
                    (earlier, later) -> later,
                    LinkedHashMap::new));
    holds(
        new HashSet<>(received.values()).size() == 1,
        "the same events for " + reporters,
        received.toString());
  }

  @Then("the reporter {string} received the event {string}")
  public void receivedTheEvent(String reporter, String event) {
    receivedTimes(reporter, event, "at least once", ReceivedSentences::isPositive);
  }

  @Then("the reporter {string} received the event {string} {int} times")
  public void receivedTheEventTimes(String reporter, String event, int times) {
    receivedTimes(reporter, event, times + " times", Integer.valueOf(times)::equals);
  }

  @Then("the reporter {string} did not receive the event {string}")
  public void didNotReceive(String reporter, String event) {
    receivedTimes(reporter, event, "never", ReceivedSentences::isZero);
  }

  private void receivedTimes(String reporter, String event, String said, IntPredicate met) {
    var kind = EventKind.parse(event);
    if (kind == null) {
      throw new ModelException(event + " is not a kind of event");
    }
    var events = eventsOf(world, reporter);
    holdsOfStream(
        met.test((int) events.stream().map(Event::getKind).filter(kind::equals).count()),
        "\"" + reporter + "\" to receive " + event + " " + said,
        events);
  }

  @Then("the reporter {string} received no event")
  public void receivedNoEvent(String reporter) {
    var events = eventsOf(world, reporter);
    holdsOfStream(events.isEmpty(), "\"" + reporter + "\" to receive no event", events);
  }

  @Then("each instance's reporter {string} received only its own journey's events")
  public void receivedOnlyItsOwn(String reporter) {
    var made = madeAs(world, reporter);
    var journeys = world.journeys();
    holds(
        made.size() == journeys.size()
            && IntStream.range(0, made.size())
                .allMatch(index -> onlyItsOwn(made.get(index), journeys.get(index).journeyId())),
        "each \"" + reporter + "\" to receive only its own journey's events",
        made.stream().map(ScenarioReporter::events).toList().toString());
  }

  private List<Event> events(String name) {
    return eventsOf(world, name);
  }

  private static boolean isPositive(int count) {
    return count > 0;
  }

  private static boolean isZero(int count) {
    return count == 0;
  }

  private static boolean onlyItsOwn(ScenarioReporter reporter, String journeyId) {
    return reporter.events().stream().allMatch(event -> isOf(event, journeyId));
  }
}

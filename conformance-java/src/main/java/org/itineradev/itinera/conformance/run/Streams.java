package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.model.Dispatching;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.event.Event;

/** The event streams of the scenario's journeys. */
public final class Streams {
  private Streams() {}

  /**
   * The event stream of one of the scenario's {@code journeys}: what the reporter the runner's
   * dispatchers hold received of it, or, under the default dispatcher, what the first reporter the
   * workflow lists that never failed received, made for that journey. With no {@code journey}, it
   * is everything the held reporter received, or nothing under the default dispatcher, since no
   * listed reporter was made.
   */
  public static List<Event> streamOf(
      Model model, Recorders recorders, List<Journey> journeys, Journey journey) {
    var dispatching = model.dispatching();
    if (dispatching instanceof Dispatching.Default) {
      return journey == null
          ? List.of()
          : firstThatNeverFailed(model, recorders, journeys.indexOf(journey)).events();
    }
    var held =
        dispatching instanceof Dispatching.Holding holding
            ? holding.reporter()
            : Recorders.RUNNERS_RECORDER;
    var events = recorders.made(held).stream().flatMap(Streams::eventsOf).toList();
    return journey == null ? events : ofJourney(events, journey);
  }

  private static List<Event> ofJourney(List<Event> events, Journey journey) {
    return events.stream().filter(event -> isOf(event, journey.journeyId())).toList();
  }

  /** Whether {@code event} belongs to the journey whose ID is {@code journeyId}. */
  public static boolean isOf(Event event, String journeyId) {
    return event.getJourneyId().toString().equals(journeyId);
  }

  private static Stream<Event> eventsOf(ScenarioReporter reporter) {
    return reporter.events().stream();
  }

  private static ScenarioReporter firstThatNeverFailed(
      Model model, Recorders recorders, int place) {
    var listed = model.workflow().reporters();
    return (listed == null ? List.<String>of() : listed)
        .stream()
            .map(name -> madeAt(recorders, name, place))
            .filter(Objects::nonNull)
            .filter(Streams::neverFailed)
            .findFirst()
            .orElseThrow(
                () ->
                    new ModelException(
                        "every reporter the workflow lists failed, so no complete stream is left to"
                            + " read"));
  }

  private static ScenarioReporter madeAt(Recorders recorders, String name, int place) {
    var made = recorders.made(name);
    return place < made.size() ? made.get(place) : null;
  }

  private static boolean neverFailed(ScenarioReporter reporter) {
    return !reporter.failed();
  }
}

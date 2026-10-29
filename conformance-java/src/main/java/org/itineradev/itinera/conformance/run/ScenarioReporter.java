package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.List;
import org.itineradev.itinera.conformance.model.ReporterFailure;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.report.Reporter;
import org.itineradev.itinera.report.testing.RecordingReporter;

/**
 * A recording reporter of the scenario: it keeps every event and entry of global metadata it
 * receives, the one it fails on included, then fails as the scenario scripts it, and remembers
 * whether it ever failed. A reporter the workflow lists keeps the journey ID it was made with; one
 * a dispatcher holds is made with none.
 */
public final class ScenarioReporter implements Reporter {
  private final ReporterFailure failure;
  private final JourneyId journeyId;
  private final RecordingReporter recording = new RecordingReporter();
  private final List<Entry> received = new ArrayList<>();
  private boolean failed;

  ScenarioReporter(ReporterFailure failure, JourneyId journeyId) {
    this.failure = failure;
    this.journeyId = journeyId;
  }

  /** The journey ID it was made with, or {@code null} when a dispatcher holds it. */
  public JourneyId journeyId() {
    return journeyId;
  }

  /** Whether it failed on any event or entry of global metadata. */
  public boolean failed() {
    return failed;
  }

  /** Every event it received, in order. */
  public List<Event> events() {
    return recording.getEvents();
  }

  /** Every entry of global metadata it received, in order. */
  public List<Entry> entries() {
    return List.copyOf(received);
  }

  @Override
  public void report(Event event) {
    var isFirst = recording.getEvents().isEmpty();
    recording.report(event);
    if (failure != null && failsOn(event, isFirst)) {
      failed = true;
      throw new ScriptedReporterFailure(messageOn(event));
    }
  }

  @Override
  public void setGlobalMetadata(String key, Object value) {
    var events = recording.getEvents();
    received.add(new Entry(key, value, events.isEmpty() ? null : last(events).getKind()));
    if (failure instanceof ReporterFailure.OnGlobalMetadata onKey && onKey.key().equals(key)) {
      failed = true;
      throw new ScriptedReporterFailure(
          "the scripted reporter fails on the global metadata " + key);
    }
  }

  private boolean failsOn(Event event, boolean isFirst) {
    if (failure instanceof ReporterFailure.OnFirstEvent) {
      return isFirst;
    }
    return failure instanceof ReporterFailure.OnEvent onEvent && event.getKind() == onEvent.kind();
  }

  private String messageOn(Event event) {
    if (failure instanceof ReporterFailure.OnEvent onEvent && onEvent.message() != null) {
      return onEvent.message();
    }
    return "the scripted reporter fails on " + event.getKind();
  }

  private static Event last(List<Event> events) {
    return events.get(events.size() - 1);
  }
}

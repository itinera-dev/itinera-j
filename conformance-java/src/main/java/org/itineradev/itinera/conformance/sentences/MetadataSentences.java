package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.Reported.entriesOf;
import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.AbstractMap.SimpleEntry;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Entry;
import org.itineradev.itinera.conformance.run.ScenarioReporter;
import org.itineradev.itinera.conformance.trace.Fields;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.EventKind;

/**
 * The sentences that read the global metadata each scripted reporter received, and the events that
 * must not carry it.
 */
public final class MetadataSentences {
  private static final Set<EventKind> EMITTED_BY_CUSTOM_CODE =
      EnumSet.of(
          EventKind.STEP_INFO,
          EventKind.STEP_WARNING,
          EventKind.STEP_ERROR,
          EventKind.JOURNEY_INFO,
          EventKind.JOURNEY_WARNING,
          EventKind.JOURNEY_ERROR);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public MetadataSentences(World world) {
    this.world = world;
  }

  @Then("^the reporters (\"[^\"]*\"(?:, \"[^\"]*\")*) received the same global metadata$")
  public void receivedTheSameGlobalMetadata(String reporters) {
    var received =
        names(reporters).stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    this::keysAndValues,
                    (earlier, later) -> later,
                    LinkedHashMap::new));
    holds(
        new HashSet<>(received.values()).size() == 1,
        "the same global metadata for " + reporters,
        received.toString());
  }

  @Then("no engine event carries the global metadata value of {string}")
  public void noEngineEventCarriesTheValueOf(String key) {
    var values =
        world.recorders().all().stream()
            .flatMap(MetadataSentences::received)
            .filter(entry -> isUnder(entry, key))
            .toList();
    var events = world.stream();
    holds(!values.isEmpty(), "a reporter to receive the global metadata \"" + key + "\"", "none");
    holdsOfStream(
        events.stream()
            .filter(MetadataSentences::isEngineEvent)
            .noneMatch(event -> carriesAny(event, values)),
        "no engine event to carry the value of \"" + key + "\"",
        events);
  }

  @Then("the reporter {string} received the global metadata:")
  public void receivedTheGlobalMetadata(String reporter, DataTable table) {
    var found = entriesOf(world, reporter);
    var expected = table.asMaps();
    holds(
        found.size() == expected.size()
            && IntStream.range(0, found.size())
                .allMatch(index -> matches(found.get(index), expected.get(index))),
        "\"" + reporter + "\" to receive " + expected,
        found.toString());
  }

  private List<Map.Entry<String, Object>> keysAndValues(String name) {
    return entriesOf(world, name).stream().map(MetadataSentences::keyAndValue).toList();
  }

  private static Map.Entry<String, Object> keyAndValue(Entry entry) {
    return new SimpleEntry<>(entry.key(), entry.value());
  }

  /** Whether {@code entry} is what {@code row} says, an empty cell not compared. */
  private static boolean matches(Entry entry, Map<String, String> row) {
    return agrees(row.get("key"), entry.key())
        && agreesInValue(row.get("value"), entry.value())
        && agrees(row.get("after"), entry.after() == null ? "none" : entry.after().toString());
  }

  private static boolean agrees(String cell, String found) {
    return cell == null || cell.equals(found);
  }

  private static boolean agreesInValue(String cell, Object found) {
    return cell == null || Objects.equals(value(cell), found);
  }

  /** Whether {@code event} is one the engine emits, not one a step, hook or provider emits. */
  static boolean isEngineEvent(Event event) {
    return !EMITTED_BY_CUSTOM_CODE.contains(event.getKind());
  }

  /** Whether any field of {@code event} shows the value of one of {@code entries}. */
  private static boolean carriesAny(Event event, List<Entry> entries) {
    return entries.stream().anyMatch(entry -> shows(event, entry));
  }

  private static boolean shows(Event event, Entry entry) {
    return Fields.apartFromIdAndTime(event).toString().contains(String.valueOf(entry.value()));
  }

  private static Stream<Entry> received(ScenarioReporter reporter) {
    return reporter.entries().stream();
  }

  private static boolean isUnder(Entry entry, String key) {
    return entry.key().equals(key);
  }
}

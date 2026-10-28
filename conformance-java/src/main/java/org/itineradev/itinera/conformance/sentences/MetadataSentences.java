package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Reported.entriesOf;
import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Entry;

/** The sentences that read the global metadata each scripted reporter received. */
public final class MetadataSentences {
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
}

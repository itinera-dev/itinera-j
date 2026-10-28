package org.itineradev.itinera.conformance.trace;

import static org.itineradev.itinera.conformance.model.Values.value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.event.EventKind;

/** What a row of an event table expects. */
public final class Table {
  private Table() {}

  /** The line a row of an event table expects. */
  public static Line expectedLine(Map<String, String> row) {
    var unknown = row.keySet().stream().filter(Table::isUnknown).findFirst();
    if (unknown.isPresent()) {
      throw new ModelException("event tables have no column \"" + unknown.get() + "\"");
    }
    return new Line(eventOf(row), cellsOf(row), dataOf(row.get("data")), List.of());
  }

  private static String eventOf(Map<String, String> row) {
    var event = row.get("event");
    if (event == null) {
      throw new ModelException(row + " has no event");
    }
    if (EventKind.parse(event) == null) {
      throw new ModelException(event + " is not a kind of event");
    }
    return event;
  }

  private static boolean isUnknown(String column) {
    return !Line.COLUMNS.contains(column) && !"event".equals(column) && !"data".equals(column);
  }

  private static Map<String, String> cellsOf(Map<String, String> row) {
    return Line.COLUMNS.stream()
        .filter(column -> isFilled(row, column))
        .collect(
            Collectors.toMap(
                Function.identity(), row::get, Table::neverMerged, LinkedHashMap::new));
  }

  private static String neverMerged(String first, String second) {
    throw new IllegalStateException("each column is written once");
  }

  private static boolean isFilled(Map<String, String> row, String column) {
    return row.get(column) != null;
  }

  private static Data dataOf(String cell) {
    if (cell == null) {
      return new Data.Unstated();
    }
    return "none".equals(cell) ? new Data.None() : new Data.Is(value(cell));
  }
}

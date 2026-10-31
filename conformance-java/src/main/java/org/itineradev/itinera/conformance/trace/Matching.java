package org.itineradev.itinera.conformance.trace;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** How the lines of an event stream compare with the lines an event table expects. */
public final class Matching {
  private Matching() {}

  /**
   * Whether {@code lines} hold the {@code expected} lines in order, possibly with others between.
   */
  public static boolean includesInOrder(List<Line> lines, List<Line> expected) {
    if (expected.isEmpty()) {
      return true;
    }
    var found = indexOf(lines, expected.get(0));
    return found != -1
        && includesInOrder(
            lines.subList(found + 1, lines.size()), expected.subList(1, expected.size()));
  }

  private static int indexOf(List<Line> lines, Line expected) {
    return IntStream.range(0, lines.size())
        .filter(index -> matches(expected, lines.get(index)))
        .findFirst()
        .orElse(-1);
  }

  /** Whether {@code lines} are exactly the {@code expected} lines. */
  public static boolean areExactly(List<Line> lines, List<Line> expected) {
    return lines.size() == expected.size()
        && IntStream.range(0, lines.size())
            .allMatch(index -> matches(expected.get(index), lines.get(index)));
  }

  /** Whether {@code line} is what {@code expected} describes, comparing only what it states. */
  private static boolean matches(Line expected, Line line) {
    return expected.event().equals(line.event())
        && expected.cells().entrySet().stream().allMatch(cell -> hasCell(line, cell))
        && admits(expected.data(), line.data());
  }

  private static boolean hasCell(Line line, Map.Entry<String, String> cell) {
    return cell.getValue().equals(line.cells().get(cell.getKey()));
  }

  private static boolean admits(Data expected, Data data) {
    if (expected instanceof Data.Unstated) {
      return true;
    }
    return expected.equals(data);
  }
}

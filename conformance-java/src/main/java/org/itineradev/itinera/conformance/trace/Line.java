package org.itineradev.itinera.conformance.trace;

import java.util.List;
import java.util.Map;

/**
 * One event as an event table describes it.
 *
 * @param event its kind
 * @param cells the cells of its other columns, by column
 * @param data its data
 * @param carried what else it carries that no column shows, such as a reason's message and details
 */
public record Line(String event, Map<String, String> cells, Data data, List<Object> carried) {
  /** The columns an event table may have besides {@code event} and {@code data}. */
  static final List<String> COLUMNS =
      List.of(
          "step",
          "attempt",
          "key",
          "code",
          "policy",
          "hook",
          "lifecycle",
          "adapter",
          "provider",
          "implementation",
          "source",
          "cause",
          "rescue",
          "decided by",
          "message",
          "error");
}

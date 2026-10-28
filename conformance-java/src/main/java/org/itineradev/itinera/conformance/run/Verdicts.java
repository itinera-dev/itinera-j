package org.itineradev.itinera.conformance.run;

import java.util.List;
import org.itineradev.itinera.event.Event;

/** How a Then sentence says whether what it expects holds. */
public final class Verdicts {
  private Verdicts() {}

  /**
   * Holds when {@code condition} does, and otherwise throws {@link Unmet}, saying what was expected
   * and found.
   */
  public static void holds(boolean condition, String expected, String found) {
    if (!condition) {
      throw new Unmet("expected " + expected + "; found " + found);
    }
  }

  /**
   * Holds when {@code condition} does, and otherwise says what was expected and which events {@code
   * events} held.
   */
  public static void holdsOfStream(boolean condition, String expected, List<Event> events) {
    holds(condition, expected, "the events " + events.stream().map(Event::getKind).toList());
  }
}

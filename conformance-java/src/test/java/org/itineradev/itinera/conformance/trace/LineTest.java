package org.itineradev.itinera.conformance.trace;

import static org.itineradev.itinera.conformance.trace.Matching.areExactly;
import static org.itineradev.itinera.conformance.trace.Matching.includesInOrder;
import static org.itineradev.itinera.conformance.trace.Table.expectedLine;
import static org.itineradev.itinera.conformance.trace.Writing.lineOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.ModelException;
import org.junit.jupiter.api.Test;

final class LineTest {
  @Test
  void anEventsLineFillsTheColumnsItHas() {
    assertEquals(
        new Line(
            "step_given_up",
            Map.of(
                "step", "charge",
                "attempt", "1",
                "cause", "abnormal termination",
                "decided by", "default"),
            new Data.None(),
            List.of()),
        lineOf(new Events().givenUp("charge")));
  }

  @Test
  void anAbortsLineNamesItsCodeItsStepAndItsError() {
    assertEquals(
        Map.of("code", "step could not be built", "step", "charge", "error", "no gateway"),
        lineOf(new Events().aborted("charge", "no gateway")).cells());
  }

  @Test
  void anAbnormalTerminationCarriesItsErrorsMessageWhichNoColumnShows() {
    var line = lineOf(new Events().terminated("charge", "gateway down"));
    assertEquals(Map.of("step", "charge", "attempt", "1"), line.cells());
    assertEquals(List.of("gateway down"), line.carried());
  }

  @Test
  void anEmittedEventCarriesItsMessageAndData() {
    var line = lineOf(new Events().info("charge", "paid", Map.of("amount", 42L)));
    assertEquals("paid", line.cells().get("message"));
    assertEquals(new Data.Is(Map.of("amount", 42L)), line.data());
  }

  @Test
  void aRowComparesOnlyTheCellsItFills() {
    var events = new Events();
    var lines = List.of(lineOf(events.attempt("charge")), lineOf(events.succeeded("charge")));
    var withAnEmptyCell = new HashMap<String, String>();
    withAnEmptyCell.put("event", "attempt_started");
    withAnEmptyCell.put("step", "charge");
    withAnEmptyCell.put("attempt", null);
    var expected =
        List.of(expectedLine(withAnEmptyCell), expectedLine(Map.of("event", "step_succeeded")));
    assertTrue(areExactly(lines, expected));
    assertTrue(includesInOrder(lines, expected.subList(1, 2)));
  }

  @Test
  void linesInAnotherOrderAreNeitherIncludedInOrderNorExactlyTheSame() {
    var events = new Events();
    var lines = List.of(lineOf(events.attempt("charge")), lineOf(events.succeeded("charge")));
    var reversed =
        List.of(
            expectedLine(Map.of("event", "step_succeeded")),
            expectedLine(Map.of("event", "attempt_started")));
    assertFalse(includesInOrder(lines, reversed));
    assertFalse(areExactly(lines, reversed));
  }

  @Test
  void statedDataMustMatchAndNoneMatchesNoData() {
    var line = lineOf(new Events().info("charge", "paid", null));
    assertTrue(
        areExactly(
            List.of(line), List.of(expectedLine(Map.of("event", "step_info", "data", "none")))));
    assertFalse(
        areExactly(
            List.of(line), List.of(expectedLine(Map.of("event", "step_info", "data", "1")))));
  }

  @Test
  void anEventTableWithAColumnTheFormatDoesNotHaveIsAnErrorInTheCase() {
    assertThrows(
        ModelException.class,
        () -> expectedLine(Map.of("event", "step_succeeded", "colour", "red")));
  }
}

package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.attemptOf;
import static org.itineradev.itinera.conformance.sentences.Tables.names;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.Ending;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.junit.jupiter.api.Test;

final class TablesTest {
  @Test
  void aSentenceListsNamesEachQuotedSeparatedByCommas() {
    assertEquals(List.of("audit", "metrics", ""), names("\"audit\", \"metrics\", \"\""));
  }

  @Test
  void aRowOfAttemptsGivesItsNumberOutcomeReasonAndContributions() {
    assertEquals(
        new ScriptedAttempt(
            2,
            Ending.FAILURE,
            "declined",
            "The card was declined.",
            Map.of("amount", 10L),
            Map.of("tries", 2L, "card", "visa")),
        attemptOf(
            Map.of(
                "attempt", "2",
                "outcome", "failure",
                "code", "declined",
                "message", "The card was declined.",
                "details", "{\"amount\": 10}",
                "contributes", "{\"tries\": 2, \"card\": \"visa\"}")));
  }

  @Test
  void aRowOfAttemptsLeavesOutTheColumnsItDoesNotFill() {
    assertEquals(
        new ScriptedAttempt(1, Ending.SUCCESS),
        attemptOf(Map.of("attempt", "1", "outcome", "success")));
  }

  @Test
  void contributionsThatAreNotAJsonObjectAreAnErrorInTheCase() {
    var row = Map.of("attempt", "1", "outcome", "success", "contributes", "[1]");
    assertThrows(ModelException.class, () -> attemptOf(row));
  }

  @Test
  void aRowOfAttemptsWithoutANumberIsAnErrorInTheCase() {
    var row = Map.of("attempt", "first", "outcome", "success");
    assertThrows(ModelException.class, () -> attemptOf(row));
  }
}

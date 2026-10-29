package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.itineradev.itinera.conformance.model.Ending;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;

/** What the sentences read from their tables and their lists. */
final class Tables {
  private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

  private Tables() {}

  /** A cell a row must fill. */
  static String cell(Map<String, String> row, String column) {
    var cell = row.get(column);
    if (cell == null) {
      throw new ModelException(row + " has no " + column);
    }
    return cell;
  }

  /** The key and value a row gives, its value written as JSON. */
  static Map.Entry<String, Object> entryOf(Map<String, String> row) {
    return new AbstractMap.SimpleImmutableEntry<>(cell(row, "key"), value(cell(row, "value")));
  }

  /**
   * One row of a step's attempts: its number, its outcome, the reason's code, message and details
   * or the error's message, and what it contributes, as a JSON object.
   */
  static ScriptedAttempt attemptOf(Map<String, String> row) {
    var details = row.get("details");
    var contributes = row.get("contributes");
    return new ScriptedAttempt(
        numberOf(row),
        Ending.named(cell(row, "outcome")),
        row.get("code"),
        row.get("message"),
        details == null ? null : value(details),
        contributes == null ? Map.of() : contributionsOf(contributes));
  }

  /** The names a sentence lists, each quoted, separated by commas. */
  static List<String> names(String listed) {
    return QUOTED.matcher(listed).results().map(Tables::firstGroup).toList();
  }

  private static int numberOf(Map<String, String> row) {
    try {
      return Integer.parseInt(cell(row, "attempt"));
    } catch (NumberFormatException error) {
      throw new ModelException(row + " has no attempt number", error);
    }
  }

  private static Map<String, Object> contributionsOf(String written) {
    if (!(value(written) instanceof Map<?, ?> object)) {
      throw new ModelException(written + " is not a JSON object");
    }
    var contributions = new LinkedHashMap<String, Object>();
    object.forEach((key, contributed) -> contributions.put((String) key, contributed));
    return contributions;
  }

  private static String firstGroup(MatchResult match) {
    return match.group(1);
  }
}

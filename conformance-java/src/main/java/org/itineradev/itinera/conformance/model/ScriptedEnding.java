package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** How a scripted attempt ends, as the script's {@code ending} column writes it. */
public enum ScriptedEnding {
  SUCCESS("success"),
  FAILURE("failure"),
  SKIPPED("skipped"),
  ABNORMAL_TERMINATION("abnormal termination"),
  REAL_RUN("real run");

  private static final Map<String, ScriptedEnding> BY_WRITTEN =
      Arrays.stream(values())
          .collect(Collectors.toMap(ScriptedEnding::written, Function.identity()));

  private final String written;

  ScriptedEnding(String written) {
    this.written = written;
  }

  /** How a script writes it. */
  public String written() {
    return written;
  }

  /** The ending a script names. */
  public static ScriptedEnding named(String written) {
    var ending = BY_WRITTEN.get(written);
    if (ending == null) {
      throw new ModelException(written + " is not an ending of a script");
    }
    return ending;
  }
}

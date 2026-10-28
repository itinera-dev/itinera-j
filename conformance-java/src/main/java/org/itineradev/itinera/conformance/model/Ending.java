package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** How an attempt of a scripted step ends, as a case writes it. */
public enum Ending {
  SUCCESS("success"),
  FAILURE("failure"),
  SKIPPED("skipped"),
  ERROR("error");

  private static final Map<String, Ending> BY_WRITTEN =
      Arrays.stream(values()).collect(Collectors.toMap(Ending::written, Function.identity()));

  private final String written;

  Ending(String written) {
    this.written = written;
  }

  /** The ending a case names. */
  public static Ending named(String written) {
    var ending = BY_WRITTEN.get(written);
    if (ending == null) {
      throw new ModelException(written + " is not an outcome of the cases");
    }
    return ending;
  }

  private String written() {
    return written;
  }
}

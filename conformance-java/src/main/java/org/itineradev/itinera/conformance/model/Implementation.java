package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** An implementation of the cases' {@code labeller} a provider makes. */
public enum Implementation {
  FIRST("first labeller"),
  SECOND("second labeller");

  private static final Map<String, Implementation> BY_WRITTEN =
      Arrays.stream(values())
          .collect(Collectors.toMap(Implementation::written, Function.identity()));

  private final String written;

  Implementation(String written) {
    this.written = written;
  }

  /** How the cases write it. */
  public String written() {
    return written;
  }

  /** The implementation a case names. */
  public static Implementation named(String written) {
    var implementation = BY_WRITTEN.get(written);
    if (implementation == null) {
      throw new ModelException(written + " is not an implementation of the cases");
    }
    return implementation;
  }
}

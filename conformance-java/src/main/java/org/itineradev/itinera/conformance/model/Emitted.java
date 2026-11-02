package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The events a step emits through its step reporter, as a case names them. */
public enum Emitted {
  STEP_INFO("step_info"),
  STEP_WARNING("step_warning"),
  STEP_ERROR("step_error");

  private static final Map<String, Emitted> BY_WRITTEN =
      Arrays.stream(values()).collect(Collectors.toMap(Emitted::written, Function.identity()));

  private final String written;

  Emitted(String written) {
    this.written = written;
  }

  /** The event a case names. */
  public static Emitted named(String written) {
    var emitted = BY_WRITTEN.get(written);
    if (emitted == null) {
      throw new ModelException("a step cannot emit " + written);
    }
    return emitted;
  }

  private String written() {
    return written;
  }
}

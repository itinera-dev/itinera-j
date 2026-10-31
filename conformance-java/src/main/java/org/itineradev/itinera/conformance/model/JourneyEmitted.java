package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The events a hook or a service provider emits through its reporter, as a case names them. */
public enum JourneyEmitted {
  JOURNEY_INFO("journey_info"),
  JOURNEY_WARNING("journey_warning"),
  JOURNEY_ERROR("journey_error");

  private static final Map<String, JourneyEmitted> BY_WRITTEN =
      Arrays.stream(values())
          .collect(Collectors.toMap(JourneyEmitted::written, Function.identity()));

  private final String written;

  JourneyEmitted(String written) {
    this.written = written;
  }

  /** The event a case names. */
  public static JourneyEmitted named(String written) {
    var emitted = BY_WRITTEN.get(written);
    if (emitted == null) {
      throw new ModelException("a hook or a provider cannot emit " + written);
    }
    return emitted;
  }

  private String written() {
    return written;
  }
}

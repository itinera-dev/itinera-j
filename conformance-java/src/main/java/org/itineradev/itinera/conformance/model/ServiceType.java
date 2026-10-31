package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A service type of the cases. */
public enum ServiceType {
  LABELLER("labeller"),
  METER("meter");

  private static final Map<String, ServiceType> BY_WRITTEN =
      Arrays.stream(values()).collect(Collectors.toMap(ServiceType::written, Function.identity()));

  private final String written;

  ServiceType(String written) {
    this.written = written;
  }

  /** The service type a case names. */
  public static ServiceType named(String written) {
    var type = BY_WRITTEN.get(written);
    if (type == null) {
      throw new ModelException(written + " is not a service type of the cases");
    }
    return type;
  }

  private String written() {
    return written;
  }
}

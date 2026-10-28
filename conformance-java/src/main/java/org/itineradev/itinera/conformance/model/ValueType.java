package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A type of the cases' neutral vocabulary, and the class a value of it has here. */
public enum ValueType {
  INTEGER("integer", Long.class),
  NUMBER("number", Double.class),
  STRING("string", String.class),
  BOOLEAN("boolean", Boolean.class),
  LIST("list", List.class),
  OBJECT("object", Map.class);

  private static final Map<String, ValueType> BY_WRITTEN =
      Arrays.stream(values()).collect(Collectors.toMap(ValueType::written, Function.identity()));

  private final String written;
  private final Class<?> type;

  ValueType(String written, Class<?> type) {
    this.written = written;
    this.type = type;
  }

  /** The class a value of this type has. */
  public Class<?> type() {
    return type;
  }

  /** The type a case names. */
  public static ValueType named(String written) {
    var type = BY_WRITTEN.get(written);
    if (type == null) {
      throw new ModelException(written + " is not a type of the cases");
    }
    return type;
  }

  private String written() {
    return written;
  }
}

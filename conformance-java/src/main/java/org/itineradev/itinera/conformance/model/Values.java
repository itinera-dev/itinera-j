package org.itineradev.itinera.conformance.model;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** The values cases write as JSON. */
public final class Values {
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.USE_LONG_FOR_INTS).build();

  private Values() {}

  /**
   * The value a case writes as JSON: an integer becomes a {@code Long}, any other number a {@code
   * Double}, an object a {@code LinkedHashMap<String, Object>} keeping its keys' order, an array a
   * {@code List<Object>}, and {@code null} the value null.
   */
  public static Object value(String written) {
    try {
      return JSON.readValue(written, Object.class);
    } catch (JacksonException error) {
      throw new ModelException(written + " is not JSON: " + error.getOriginalMessage(), error);
    }
  }
}

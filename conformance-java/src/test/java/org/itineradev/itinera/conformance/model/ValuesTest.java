package org.itineradev.itinera.conformance.model;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

final class ValuesTest {
  static final List<Arguments> WRITTEN =
      List.of(
          argumentSet("an integer", "42", 42L),
          argumentSet("a number", "4.5", 4.5),
          argumentSet("a string", "\"eu\"", "eu"),
          argumentSet("a boolean", "true", true),
          argumentSet("null", "null", null),
          argumentSet("a list", "[1, \"a\"]", Arrays.asList(1L, "a")),
          argumentSet("an object", "{\"amount\": 10}", Map.of("amount", 10L)));

  static final List<Arguments> TYPES =
      List.of(
          argumentSet("integer", "integer", Long.class),
          argumentSet("number", "number", Double.class),
          argumentSet("string", "string", String.class),
          argumentSet("boolean", "boolean", Boolean.class),
          argumentSet("list", "list", List.class),
          argumentSet("object", "object", Map.class));

  @ParameterizedTest
  @FieldSource("WRITTEN")
  void aValueWrittenAsJsonBecomesALongDoubleStringBooleanListOrMap(
      String written, Object expected) {
    assertEquals(expected, value(written));
  }

  @Test
  void anObjectKeepsItsKeysInTheOrderWritten() {
    Map<?, ?> object = assertInstanceOf(Map.class, value("{\"b\": 1, \"a\": 2}"));
    assertEquals(List.of("b", "a"), List.copyOf(object.keySet()));
  }

  @Test
  void textThatIsNotJsonIsAnErrorInTheCase() {
    assertThrows(ModelException.class, () -> value("{"));
  }

  @ParameterizedTest
  @FieldSource("TYPES")
  void eachTypeOfTheCasesHasTheClassItsValuesHave(String written, Class<?> expected) {
    assertEquals(expected, ValueType.named(written).type());
  }

  @Test
  void aTypeTheCasesDoNotNameIsAnErrorInTheCase() {
    assertThrows(ModelException.class, () -> ValueType.named("decimal"));
  }
}

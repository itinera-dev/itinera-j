package org.itineradev.itinera.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.DataBags;
import org.itineradev.itinera.journey.Read;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

final class GenericTokensTest {
  record Order(long amount) {}

  record Box<T>(T content) {}

  sealed interface Outcome<T> permits Ok, Refused {}

  record Ok<T>(T value) implements Outcome<T> {}

  record Refused<T>(String why) implements Outcome<T> {}

  static final List<Arguments> ofTheType =
      List.of(
          argumentSet(
              "a list of longs holding longs", List.of(1L, 2L), new TypeToken<List<Long>>() {}),
          argumentSet(
              "a map of records holding records",
              Map.of("a", new Order(4L)),
              new TypeToken<Map<String, Order>>() {}),
          argumentSet(
              "a map whose keys are char sequences holding strings",
              Map.of("a", 4L),
              new TypeToken<Map<CharSequence, Long>>() {}),
          argumentSet(
              "a generic record holding its type argument",
              new Box<>(4L),
              new TypeToken<Box<Long>>() {}),
          argumentSet(
              "a sealed interface's member holding its type argument",
              new Ok<>(4L),
              new TypeToken<Outcome<Long>>() {}),
          argumentSet(
              "a sealed interface's member not using its type argument",
              new Refused<Long>("no"),
              new TypeToken<Outcome<Long>>() {}),
          argumentSet(
              "a list of a bounded wildcard holding its bound's subclasses",
              List.of(1L, 2),
              new TypeToken<List<? extends Number>>() {}),
          argumentSet(
              "a list of a wildcard with a lower bound holding anything",
              List.of("a"),
              new TypeToken<List<? super Long>>() {}),
          argumentSet(
              "an unmarked type argument holding null",
              Arrays.asList(1L, null),
              new TypeToken<List<Long>>() {}));

  static final List<Arguments> ofAnotherType =
      List.of(
          argumentSet(
              "a list of longs holding ints", List.of(1, 2), new TypeToken<List<Long>>() {}),
          argumentSet(
              "a list of longs holding strings", List.of("a"), new TypeToken<List<Long>>() {}),
          argumentSet(
              "a map of records holding longs",
              Map.of("a", 4L),
              new TypeToken<Map<String, Order>>() {}),
          argumentSet(
              "a generic record holding another type",
              new Box<>("four"),
              new TypeToken<Box<Long>>() {}),
          argumentSet(
              "a sealed interface's member holding another type",
              new Ok<>("four"),
              new TypeToken<Outcome<Long>>() {}),
          argumentSet(
              "a list of a bounded wildcard holding strings",
              List.of("a"),
              new TypeToken<List<? extends Number>>() {}));

  static final List<Arguments> refused =
      List.of(
          argumentSet(
              "a linked list", (Executable) () -> new TypeToken<List<LinkedList<Long>>>() {}),
          argumentSet(
              "a map whose keys are not strings",
              (Executable) () -> new TypeToken<Map<Integer, Long>>() {}),
          argumentSet("a hash map", (Executable) () -> new TypeToken<HashMap<String, Long>>() {}),
          argumentSet("an array", (Executable) () -> new TypeToken<long[]>() {}),
          argumentSet("a type variable", (Executable) GenericTokensTest::tokenOfVariable));

  @ParameterizedTest
  @FieldSource("ofTheType")
  void aValueOfTheGenericTypeIsReadAsIt(Object value, TypeToken<?> type) {
    assertEquals(new Read.Present<>(value), read(value, type));
  }

  @ParameterizedTest
  @FieldSource("ofAnotherType")
  void aValueOfAnotherGenericTypeIsOfAnotherType(Object value, TypeToken<?> type) {
    assertEquals(Read.OtherType.INSTANCE, read(value, type));
  }

  @ParameterizedTest
  @FieldSource("refused")
  void aTypeThatNoValueCanBeIsRefused(Executable make) {
    assertThrows(IllegalArgumentException.class, make);
  }

  private static <T> TypeToken<T> tokenOfVariable() {
    return new TypeToken<T>() {};
  }

  private static Read<?> read(Object value, TypeToken<?> type) {
    return DataBags.of("key", value).read("key", type);
  }
}

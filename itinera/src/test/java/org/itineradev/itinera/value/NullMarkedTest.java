package org.itineradev.itinera.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.Arrays;
import java.util.List;
import org.itineradev.itinera.journey.DataBags;
import org.itineradev.itinera.journey.Read;
import org.itineradev.itinera.value.marked.Label;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

final class NullMarkedTest {
  record LooseNote(String text) {}

  record Holder<T>(@NonNull T content) {}

  @NullMarked
  static final class Marked {
    record Note(String text, @Nullable String remark) {}

    record Box<T extends @Nullable Object>(T content) {}

    static TypeToken<Long> amount() {
      return new TypeToken<Long>() {};
    }

    static TypeToken<@Nullable Long> maybeAmount() {
      return new TypeToken<@Nullable Long>() {};
    }

    static TypeToken<List<Long>> longs() {
      return new TypeToken<List<Long>>() {};
    }

    static TypeToken<List<@Nullable Long>> maybeLongs() {
      return new TypeToken<List<@Nullable Long>>() {};
    }

    static TypeToken<Box<Long>> box() {
      return new TypeToken<Box<Long>>() {};
    }

    static TypeToken<Box<@Nullable Long>> maybeBox() {
      return new TypeToken<Box<@Nullable Long>>() {};
    }

    @NullUnmarked
    static final class Unmarked {
      static TypeToken<Long> amount() {
        return new TypeToken<Long>() {};
      }
    }
  }

  static final List<Arguments> admitted =
      List.of(
          argumentSet("an unmarked type", null, new TypeToken<Long>() {}),
          argumentSet("a type marked nullable in marked code", null, Marked.maybeAmount()),
          argumentSet("a type in code that unmarks marked code", null, Marked.Unmarked.amount()),
          argumentSet(
              "a type argument marked nullable in marked code",
              Arrays.asList(1L, null),
              Marked.maybeLongs()),
          argumentSet(
              "a marked record's component marked nullable",
              new Marked.Note("hi", null),
              TypeToken.of(Marked.Note.class)),
          argumentSet(
              "an unmarked record's component", new LooseNote(null), TypeToken.of(LooseNote.class)),
          argumentSet(
              "a marked generic record's component bound to a nullable type",
              new Marked.Box<@Nullable Long>(null),
              Marked.maybeBox()));

  static final List<Arguments> refused =
      List.of(
          argumentSet("a type in marked code", null, Marked.amount()),
          argumentSet(
              "a type marked non-null in unmarked code", null, new TypeToken<@NonNull Long>() {}),
          argumentSet("a type argument in marked code", Arrays.asList(1L, null), Marked.longs()),
          argumentSet(
              "a marked record's component",
              new Marked.Note(nothing(), null),
              TypeToken.of(Marked.Note.class)),
          argumentSet(
              "a marked generic record's component bound to a type",
              new Marked.Box<@Nullable Long>(null),
              Marked.box()),
          argumentSet(
              "an unmarked record's component of a type variable marked non-null",
              new Holder<Long>(nothing()),
              new TypeToken<Holder<Long>>() {}),
          argumentSet(
              "the component of a record in a marked package",
              new Label(nothing()),
              TypeToken.of(Label.class)));

  @ParameterizedTest
  @FieldSource("admitted")
  void nullIsReadAsATypeThatAdmitsIt(@Nullable Object value, TypeToken<?> type) {
    assertEquals(new Read.Present<>(value), read(value, type));
  }

  @ParameterizedTest
  @FieldSource("refused")
  void nullIsOfAnotherTypeThanOneThatRefusesIt(@Nullable Object value, TypeToken<?> type) {
    assertEquals(Read.OtherType.INSTANCE, read(value, type));
  }

  private static <T> T nothing() {
    return null;
  }

  private static Read<?> read(@Nullable Object value, TypeToken<?> type) {
    return DataBags.of("key", value).read("key", type);
  }
}

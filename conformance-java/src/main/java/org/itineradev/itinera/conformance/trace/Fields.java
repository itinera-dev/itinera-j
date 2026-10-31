package org.itineradev.itinera.conformance.trace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.event.Event;

/** Every field of an event but those two journeys of one workflow never share. */
public final class Fields {
  private static final Set<String> NOT_COMPARED = Set.of("journeyId", "timestamp");

  private Fields() {}

  /** Every field of each of {@code events}, its type included, apart from the ID and timestamp. */
  public static List<List<Object>> apartFromIdAndTime(List<Event> events) {
    return events.stream().map(Fields::apartFromIdAndTime).toList();
  }

  private static List<Object> apartFromIdAndTime(Event event) {
    var components = event.getClass().getRecordComponents();
    if (components == null) {
      throw new ModelException(
          event.getKind() + " is not a record, so its fields cannot be compared");
    }
    return Stream.concat(
            Stream.of(event.getClass()),
            Arrays.stream(components)
                .filter(Fields::isCompared)
                .map(component -> valueOf(event, component)))
        .toList();
  }

  private static boolean isCompared(RecordComponent component) {
    return !NOT_COMPARED.contains(component.getName());
  }

  private static Object valueOf(Event event, RecordComponent component) {
    try {
      return component.getAccessor().invoke(event);
    } catch (IllegalAccessException | InvocationTargetException error) {
      throw new IllegalStateException("cannot read " + component.getName(), error);
    }
  }
}

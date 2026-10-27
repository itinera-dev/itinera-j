package org.itineradev.itinera.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.itineradev.itinera.engine.Captured;
import org.itineradev.itinera.engine.NotAValue;
import org.itineradev.itinera.engine.Values;
import org.itineradev.itinera.value.JavaRecords.Parcel;
import org.junit.jupiter.api.Test;

final class JavaCopiesTest {
  @Test
  void aJavaRecordIsCopiedThroughItsFieldsAndCanonicalConstructor() {
    var items = new ArrayList<>(List.of("book"));
    var parcel = new Parcel(1200L, items);
    var copy = (Parcel) ((Captured) Values.capture(parcel)).getValue();
    items.add("lamp");
    assertEquals(new Parcel(1200L, List.of("book")), copy);
    assertNotSame(items, copy.items());
  }

  @Test
  void aSubclassOfBigIntegerIsNotAValue() {
    assertInstanceOf(NotAValue.class, Values.capture(new BigInteger("1") {}));
  }
}

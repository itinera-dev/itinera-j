package org.itineradev.itinera.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.itineradev.itinera.journey.DataBags;
import org.itineradev.itinera.value.JavaRecords.Fragile;
import org.itineradev.itinera.value.JavaRecords.Parcel;
import org.itineradev.itinera.value.JavaRecords.Ticket;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class JavaValuesTest {
  @AfterEach
  void rebuildAgain() {
    Ticket.fail = false;
  }

  @Test
  void aCopyThatThrowsWhileAValueIsCapturedMakesItNotAValueCarryingTheError() {
    var refused = (NotAValue) Values.capture(Set.of(new Fragile("vase")));
    assertEquals("cannot hash vase", refused.getError().getMessage());
  }

  @Test
  void aConstructorThatThrowsWhileAValueIsCapturedMakesItNotAValueCarryingTheError() {
    var ticket = new Ticket("A1");
    Ticket.fail = true;
    var refused = (NotAValue) Values.capture(ticket);
    assertEquals("no more tickets", refused.getError().getMessage());
  }

  @Test
  void aCopyThatThrowsWhileAValueIsReadMakesItOfTheWrongTypeCarryingTheError() {
    var stored = DataBags.captured(new Ticket("A1"));
    Ticket.fail = true;
    var wrong = (WrongType) Values.receive(stored, TypeToken.of(Ticket.class));
    assertEquals("no more tickets", wrong.getError().getMessage());
  }

  @Test
  void eachReceiverGetsACopyOfItsOwn() {
    var stored = DataBags.captured(new Parcel(1200L, new ArrayList<>(List.of("book"))));
    received(stored).items().add("lamp");
    assertEquals(List.of("book"), received(stored).items());
  }

  private static Parcel received(Captured stored) {
    return ((Received<Parcel>) Values.receive(stored, TypeToken.of(Parcel.class))).getValue();
  }
}

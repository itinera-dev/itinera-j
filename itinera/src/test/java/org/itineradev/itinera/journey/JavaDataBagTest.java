package org.itineradev.itinera.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.itineradev.itinera.value.JavaRecords.Ticket;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class JavaDataBagTest {
  @AfterEach
  void rebuildAgain() {
    Ticket.fail = false;
  }

  @Test
  void aCopyThatThrowsWhileTheDataBagIsReadGivesAnotherType() {
    var data = DataBags.of("ticket", new Ticket("A1"));
    Ticket.fail = true;
    assertEquals(Read.OtherType.INSTANCE, data.read("ticket", TypeToken.of(Ticket.class)));
  }

  @Test
  void aStoredLongReadsIntoALong() {
    assertEquals(
        new Read.Present<>(40L),
        DataBags.of("amount", 40L).read("amount", TypeToken.of(long.class)));
  }
}

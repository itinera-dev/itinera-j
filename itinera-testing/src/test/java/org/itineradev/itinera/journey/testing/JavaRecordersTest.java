package org.itineradev.itinera.journey.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;

final class JavaRecordersTest {
  private static final TypeToken<Long> LONG = TypeToken.of(Long.class);

  @Test
  void javaContributionsKeepWhatIsContributed() {
    var contributions = new Contributions();
    contributions.getContributor().contribute("charged", 40L);
    assertEquals(Set.of("charged"), contributions.getKeys());
    assertEquals(40L, contributions.read("charged", LONG).orNull());
  }

  @Test
  void aJavaTestDataBagLendsReadAccess() {
    var bag = new TestDataBag().with("amount", 35L);
    assertEquals(35L, bag.getAccess().read("amount", LONG).orNull());
  }
}

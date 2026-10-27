package org.itineradev.itinera.value;

import java.util.List;

/** Records written in Java, for the tests of several packages. */
public final class JavaRecords {
  private JavaRecords() {}

  /** A parcel: a value, holding a list it does not copy. */
  public record Parcel(long weight, List<String> items) {}

  /** A record whose copies fail once {@link #fail} is set, as a constructor with a check may. */
  public record Ticket(String code) {
    public static boolean fail;

    public Ticket {
      if (fail) {
        throw new IllegalStateException("no more tickets");
      }
    }
  }

  /** A record that cannot be hashed, so a set holding it cannot be copied. */
  public record Fragile(String name) {
    @Override
    public int hashCode() {
      throw new IllegalStateException("cannot hash " + name);
    }
  }
}

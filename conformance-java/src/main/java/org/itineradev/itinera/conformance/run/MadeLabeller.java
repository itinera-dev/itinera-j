package org.itineradev.itinera.conformance.run;

/**
 * A {@code labeller} a provider made, labelled {@code labelled}, whose operation fails with {@code
 * failure} when the case says so.
 */
public abstract sealed class MadeLabeller implements Labeller
    permits MadeLabeller.First, MadeLabeller.Second {
  private final String labelled;
  private final String failure;

  private MadeLabeller(String labelled, String failure) {
    this.labelled = labelled;
    this.failure = failure;
  }

  /** Its label. */
  public String labelled() {
    return labelled;
  }

  @Override
  public String label() {
    if (failure != null) {
      throw new IllegalStateException(failure);
    }
    return labelled;
  }

  /** The cases' {@code first labeller}. */
  static final class First extends MadeLabeller {
    First(String labelled, String failure) {
      super(labelled, failure);
    }
  }

  /** The cases' {@code second labeller}. */
  static final class Second extends MadeLabeller {
    Second(String labelled, String failure) {
      super(labelled, failure);
    }
  }
}

package org.itineradev.itinera.conformance.model;

/** What an input adapter does when it is called for one input. */
public sealed interface AdapterAnswer {
  /**
   * Supplies this value.
   *
   * @param value the value
   */
  record Returns(Object value) implements AdapterAnswer {}

  /** Leaves the input to the data bag. */
  record Nothing() implements AdapterAnswer {}

  /**
   * Fails with this message.
   *
   * @param message the message
   */
  record Fails(String message) implements AdapterAnswer {}

  /** Returns {@code null}, not what it supplies. */
  record NoResult() implements AdapterAnswer {}

  /** Supplies what is not a value. */
  record NonValue() implements AdapterAnswer {}

  /** Supplies the name of the step being built. */
  record ItsStepName() implements AdapterAnswer {}

  /**
   * Reads {@code key} as {@code type} through its access to the data bag: through the access it
   * kept when {@code kept}, or else through the access of this call. It supplies the value it
   * found, or leaves the input to the data bag.
   *
   * @param key the key it reads
   * @param type the type it reads it as
   * @param kept whether it reads through the access it kept
   */
  record Reads(String key, ValueType type, boolean kept) implements AdapterAnswer {}
}

package org.itineradev.itinera.conformance.model;

/** What a fake put in place of a service provider does on each call. */
public sealed interface FakeModel {
  /**
   * It makes an instance of {@code implementation} of {@code labeller}, with {@code data} when one
   * is written.
   *
   * @param implementation the implementation it makes
   * @param data the data it supplies with each instance, {@code null} for none
   */
  record Makes(Implementation implementation, Object data) implements FakeModel {}

  /**
   * It fails with {@code message}.
   *
   * @param message the message of what it throws
   */
  record Fails(String message) implements FakeModel {}

  /** It makes a {@code meter}, another service than a provider of a {@code labeller} makes. */
  record MakesMeters() implements FakeModel {}
}

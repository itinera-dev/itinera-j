package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.testing.TestMistake;

/**
 * How one journey ended: with its result, refused by {@code run}, or before, when no instance was
 * created or test support stopped the test.
 */
public sealed interface Ended {
  /**
   * Creating the workflow instance failed, so {@code run} was never called.
   *
   * @param failure what creating it threw
   */
  record NotCreated(IllegalStateException failure) implements Ended {}

  /**
   * {@code run} returned the result.
   *
   * @param result the journey's result
   */
  record Returned(JourneyResult result) implements Ended {}

  /**
   * {@code run} refused the journey.
   *
   * @param refusal what it threw
   */
  record Refused(Refusal refusal) implements Ended {}

  /**
   * Test support stopped the test before the journey.
   *
   * @param mistake what it threw
   */
  record Stopped(TestMistake mistake) implements Ended {}
}

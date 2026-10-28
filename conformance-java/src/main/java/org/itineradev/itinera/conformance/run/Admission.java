package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.workflow.Violations;

/** What building the scenario's workflow gave: journeys to run, or the violations refusing it. */
public sealed interface Admission {
  /**
   * The builder accepted the workflow.
   *
   * @param journeys how its journeys run
   */
  record Admitted(Journeys journeys) implements Admission {}

  /**
   * The builder refused the workflow.
   *
   * @param violations every violation it found
   */
  record Refused(Violations violations) implements Admission {}
}

package org.itineradev.itinera.conformance.run;

import java.util.List;
import org.itineradev.itinera.workflow.ListedStep;

/** The admitted workflow: its listing, and how the runner runs its journeys. */
public interface Journeys {
  /** The workflow's listing. */
  List<ListedStep> listing();

  /** Runs {@code times} journeys, one after another, on one executor, each with a new instance. */
  List<Journey> run(int times);
}

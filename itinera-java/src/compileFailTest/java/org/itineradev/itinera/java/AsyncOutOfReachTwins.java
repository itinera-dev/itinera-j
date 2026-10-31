package org.itineradev.itinera.java;

import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.WorkflowInstance;
import org.itineradev.itinera.workflow.Sync;

/** What {@link AsyncOutOfReachTest} refuses, written so that it compiles. */
final class AsyncOutOfReachTwins {
  void run(WorkflowInstance<Sync> sync) throws Refusal {
    new LocalExecutor().run(sync);
  }
}

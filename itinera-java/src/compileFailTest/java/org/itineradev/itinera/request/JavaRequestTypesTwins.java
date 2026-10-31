package org.itineradev.itinera.request;

import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.Sync;

/** What {@link JavaRequestTypesTest} refuses, written so that it compiles. */
final class JavaRequestTypesTwins {
  Long read(Got<StepKind<Sync>> got) {
    var amount = Requests.input("amount", TypeToken.of(Long.class));
    return got.get(amount);
  }
}

package org.itineradev.itinera.service;

import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.request.AttemptProviderKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.JourneyProviderKind;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.Sync;

/** What {@link JavaProviderTypesTest} refuses, written so that it compiles. */
final class JavaProviderTypesTwins {
  JourneyId journeyId(Got<JourneyProviderKind<Sync>> got) {
    return got.get(Requests.journeyId());
  }

  Attempt attempt(Got<AttemptProviderKind<Sync>> got) {
    return got.get(Requests.attempt());
  }

  ProviderBuilder<Object, Sync, AttemptProviderKind<Sync>> attempted() {
    return ProviderDescriptor.<Object, String>perAttempt(
            "p", "k", String.class, (workflow, answers) -> Provided.noInstance())
        .request(Requests.attempt());
  }

  StepName stepName(Got<AttemptProviderKind<Sync>> got) {
    return got.get(Requests.stepName());
  }
}

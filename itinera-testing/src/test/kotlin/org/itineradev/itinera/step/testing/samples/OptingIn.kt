package org.itineradev.itinera.step.testing.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock

@OptIn(ExperimentalItineraApi::class) fun optingIn(): StepMock = StepMock.playing(Ending.success())

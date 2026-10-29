package org.itineradev.itinera.step.testing

/** What [RescueEndingTypesTest] refuses, written so that it compiles. */
internal object RescueEndingTypesTwins {
    val rescue = RescueStepMock.playing(RescueEnding.success(), RescueEnding.failure("stolen"))
}

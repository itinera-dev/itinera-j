package org.itineradev.itinera.executor.testing

/** What [AttemptTypesTest] refuses, written so that it compiles. */
internal object AttemptTypesTwins {
    fun chosen(script: Script): AttemptScript<StepScript> = script.step("charge").onAttempt(2)

    fun read(record: StepRecord): AttemptRecord? = record.attempt(2)
}

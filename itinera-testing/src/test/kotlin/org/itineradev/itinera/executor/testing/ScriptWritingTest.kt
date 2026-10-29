@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

/** Writes part of a script. */
private typealias Writing = (Script) -> Any

class ScriptWritingTest {
    @ParameterizedTest
    @FieldSource("mistakes")
    fun `a mistake in writing a script stops the test where it is written`(
        writing: Writing,
        expected: Mistake,
    ) {
        val mistake = assertThrows<TestMistake> { writing(scripted(ordered())) }
        assertEquals(listOf(expected), mistake.problems)
    }

    @ParameterizedTest
    @FieldSource("leftOut")
    fun `what a script leaves out stops the test before the journey, naming each problem`(
        writing: (Script) -> StepScript,
        expected: List<Mistake>,
    ) {
        val recorder = RecordingReporter()
        val mistake = assertThrows<TestMistake> { writing(scripted(ordered(recorder))).run() }
        assertEquals(expected, mistake.problems)
        assertTrue(recorder.events.isEmpty())
    }

    @ParameterizedTest
    @FieldSource("covered")
    fun `a script that describes every attempt its steps can have runs`(
        writing: (Script) -> StepScript
    ) {
        writing(scripted(ordered())).run()
    }

    companion object {
        @JvmField
        val mistakes =
            listOf(
                argumentSet(
                    "a name the workflow does not have",
                    writing { script -> script.step("refund") },
                    Mistake.UnknownStep("refund"),
                ),
                argumentSet(
                    "a step named twice",
                    writing { script -> script.step("pack").willSucceed().andStep("pack") },
                    Mistake.StepScriptedTwice("pack"),
                ),
                argumentSet(
                    "attempt 0",
                    writing { script -> script.step("charge").onAttempt(0) },
                    Mistake.NoSuchAttempt("charge", 0),
                ),
                argumentSet(
                    "a negative attempt",
                    writing { script -> script.step("charge").onAttempt(-1) },
                    Mistake.NoSuchAttempt("charge", -1),
                ),
                argumentSet(
                    "the attempt after the most a step can have",
                    writing { script -> script.step("charge").onAttempt(3) },
                    Mistake.NoSuchAttempt("charge", 3),
                ),
                argumentSet(
                    "an attempt the step can never have, after a gap",
                    writing { script -> script.step("charge").onAttempt(5) },
                    Mistake.NoSuchAttempt("charge", 5),
                ),
                argumentSet(
                    "a second attempt of a rescue step",
                    writing { script -> script.step("fix_rescue").willSucceed().onAttempt(2) },
                    Mistake.NoSuchAttempt("fix_rescue", 2),
                ),
                argumentSet(
                    "remaining attempts once every attempt is described by number",
                    writing { script ->
                        script
                            .step("charge")
                            .willFail("declined")
                            .onAttempt(2)
                            .willFail("declined")
                            .onRemainingAttempts()
                    },
                    Mistake.NoSuchAttempt("charge", 3),
                ),
                argumentSet(
                    "an attempt described again",
                    writing { script -> script.step("charge").willFail("declined").onAttempt(1) },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "an attempt once every attempt is described",
                    writing { script ->
                        script.step("charge").onEveryAttempt().willSucceed().onAttempt(2)
                    },
                    Mistake.AttemptScriptedTwice("charge", 2),
                ),
                argumentSet(
                    "every attempt once one is described",
                    writing { script ->
                        script.step("charge").willFail("declined").onEveryAttempt()
                    },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "remaining attempts twice",
                    writing { script ->
                        script
                            .step("charge")
                            .onRemainingAttempts()
                            .willSucceed()
                            .onRemainingAttempts()
                    },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "attempt 1 described again through the step a test kept",
                    writing { script ->
                        val pack = script.step("pack")
                        pack.willSucceed()
                        pack.willFail("damaged")
                    },
                    Mistake.AttemptScriptedTwice("pack", 1),
                ),
                argumentSet(
                    "an attempt described again through the choice a test kept",
                    writing { script ->
                        val second = script.step("charge").willFail("declined").onAttempt(2)
                        second.willSucceed()
                        second.willContribute("charged", 40)
                    },
                    Mistake.AttemptScriptedTwice("charge", 2),
                ),
                argumentSet(
                    "remaining attempts run for real again through the choice a test kept",
                    writing { script ->
                        val remaining = script.step("charge").onRemainingAttempts()
                        remaining.willRunForReal()
                        remaining.willRunForReal()
                    },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "an attempt chosen through the step a test kept, once every attempt was chosen",
                    writing { script ->
                        val charge = script.step("charge")
                        charge.onEveryAttempt()
                        charge.onAttempt(1)
                    },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "remaining attempts chosen again through the step a test kept",
                    writing { script ->
                        val charge = script.step("charge")
                        charge.onRemainingAttempts()
                        charge.onRemainingAttempts()
                    },
                    Mistake.AttemptScriptedTwice("charge", 1),
                ),
                argumentSet(
                    "a gap in the attempts",
                    writing { script -> script.step("charge").onAttempt(2) },
                    Mistake.GapInTheAttempts("charge", 2),
                ),
                argumentSet(
                    "a rescue step that skips",
                    writing { script -> script.step("fix_rescue").willSkip() },
                    Mistake.RescueStepSkips("fix_rescue"),
                ),
                argumentSet(
                    "a rescue step that skips for a reason",
                    writing { script -> script.step("fix_rescue").willSkipBecause("fixed") },
                    Mistake.RescueStepSkips("fix_rescue"),
                ),
                argumentSet(
                    "a contribution with no contributor",
                    writing { script -> script.step("pack").willContribute("packed", true) },
                    Mistake.NoContributor("pack"),
                ),
                argumentSet(
                    "an event with no step reporter",
                    writing { script -> script.step("pack").willEmit(Level.INFO, "packed") },
                    Mistake.NoReporter("pack"),
                ),
                argumentSet(
                    "global metadata with no step reporter",
                    writing { script -> script.step("pack").willSetGlobalMetadata("box", "small") },
                    Mistake.NoReporter("pack"),
                ),
            )

        @JvmField
        val leftOut =
            listOf(
                argumentSet(
                    "the attempt after a failure",
                    script { script -> script.step("charge").willFail("declined") },
                    listOf(Mistake.AttemptLeftOut("charge", 2)),
                ),
                argumentSet(
                    "the attempt after an abnormal termination that is retried",
                    script { script -> script.step("charge").willTerminateAbnormally("timeout") },
                    listOf(Mistake.AttemptLeftOut("charge", 2)),
                ),
                argumentSet(
                    "the attempt after a real run",
                    script { script -> script.step("charge").willRunForReal() },
                    listOf(Mistake.AttemptLeftOut("charge", 2)),
                ),
                argumentSet(
                    "the rescue step of a step that can fail on attempt 1",
                    script { script ->
                        script.step("fix").willFail("broken").onAttempt(2).willSucceed()
                    },
                    listOf(Mistake.RescueStepLeftOut("fix_rescue", "fix")),
                ),
                argumentSet(
                    "the attempt after the rescue, and the rescue step",
                    script { script -> script.step("fix").willFail("broken") },
                    listOf(
                        Mistake.AttemptLeftOut("fix", 2),
                        Mistake.RescueStepLeftOut("fix_rescue", "fix"),
                    ),
                ),
                argumentSet(
                    "the attempt after a rescue step that succeeds",
                    script { script ->
                        script.step("fix").willFail("broken").andStep("fix_rescue").willSucceed()
                    },
                    listOf(Mistake.AttemptLeftOut("fix", 2)),
                ),
                argumentSet(
                    "the attempt after a rescue step that runs for real",
                    script { script ->
                        script
                            .step("fix")
                            .willTerminateAbnormally("broken")
                            .andStep("fix_rescue")
                            .willRunForReal()
                    },
                    listOf(Mistake.AttemptLeftOut("fix", 2)),
                ),
                argumentSet(
                    "every step not scripted, once all are asked for",
                    script { script -> script.step("charge").willSucceed().ensureAllScripted() },
                    listOf(
                        Mistake.StepLeftOut("pack"),
                        Mistake.StepLeftOut("label"),
                        Mistake.StepLeftOut("fix"),
                        Mistake.StepLeftOut("ship"),
                    ),
                ),
            )

        @JvmField
        val covered =
            listOf(
                argumentSet(
                    "a success on attempt 1",
                    script { script -> script.step("charge").willSucceed() },
                ),
                argumentSet(
                    "a skip on attempt 1",
                    script { script -> script.step("charge").willSkip() },
                ),
                argumentSet(
                    "a failure on every attempt",
                    script { script ->
                        script.step("charge").onEveryAttempt().willFail("declined")
                    },
                ),
                argumentSet(
                    "a real run on the remaining attempts",
                    script { script ->
                        script
                            .step("charge")
                            .willFail("declined")
                            .onRemainingAttempts()
                            .willRunForReal()
                    },
                ),
                argumentSet(
                    "an abnormal termination that is not retried",
                    script { script -> script.step("label").willTerminateAbnormally("jammed") },
                ),
                argumentSet(
                    "a rescued step that succeeds on attempt 1",
                    script { script -> script.step("fix").willSucceed() },
                ),
                argumentSet(
                    "a rescued step, its rescue step and the attempt after it",
                    script { script ->
                        script
                            .step("fix")
                            .willTerminateAbnormally("broken")
                            .onAttempt(2)
                            .willSucceed()
                            .andStep("fix_rescue")
                            .willSucceed()
                    },
                ),
                argumentSet(
                    "a rescued step whose rescue step fails, with no attempt after it",
                    script { script ->
                        script
                            .step("fix")
                            .willFail("broken")
                            .andStep("fix_rescue")
                            .willFail("beyond repair")
                    },
                ),
                argumentSet(
                    "a rescued step whose rescue step terminates abnormally, with no attempt after it",
                    script { script ->
                        script
                            .step("fix")
                            .willTerminateAbnormally("broken")
                            .andStep("fix_rescue")
                            .willTerminateAbnormally("crashed")
                    },
                ),
                argumentSet(
                    "a rescued step run for real whose rescue step fails, with no attempt after it",
                    script { script ->
                        script
                            .step("fix")
                            .willRunForReal()
                            .andStep("fix_rescue")
                            .willFail("beyond repair")
                    },
                ),
                argumentSet(
                    "every step, once all are asked for",
                    script { script ->
                        script
                            .step("charge")
                            .willRunForReal()
                            .onAttempt(2)
                            .willSucceed()
                            .andStep("pack")
                            .willSucceed()
                            .andStep("label")
                            .willSucceed()
                            .andStep("fix")
                            .willSucceed()
                            .andStep("ship")
                            .willSucceed()
                            .ensureAllScripted()
                    },
                ),
            )

        private fun writing(writing: Writing): Writing = writing

        private fun script(writing: (Script) -> StepScript): (Script) -> StepScript = writing
    }
}

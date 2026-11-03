# 0119: Attempt mistakes in test support

Tech spec for [proposal 0119](https://github.com/itinera-dev/spec/blob/main/proposals/0119-attempt-mistakes.md), implemented in [#38](https://github.com/itinera-dev/itinera-j/issues/38). It amends 0108, whose tech spec, [0108](0108-test-support.md), describes scripted runs and their record.

Status: done. Stage 10 adds the checks with scripted runs, and lists 119 with 108 in both manifests.

## The rules and where they are checked

Each choice of attempts is checked by the call that makes it, in `StepWriter`, in `itinera-testing/src/main/kotlin/org/itineradev/itinera/executor/testing/StepWriter.kt`, which throws a `TestMistake` naming one problem:

1. **`no such attempt` first.** `onAttempt(number)` with a number below 1 or above the most attempts the step can have is `Mistake.NoSuchAttempt`, before any other check, so with nothing described for a step with a retry budget of 1, `onAttempt(5)` names attempt 5 as an attempt the step can never have, not a gap. The most attempts are the retry budget plus one, and one more for a step with a rescue step, so a rescue step has one, as `Target.mostAttempts` says. `onRemainingAttempts()` once the attempts described by number reach that most names the attempt after them.
2. **`attempt scripted twice`.** `onAttempt(number)` with a number already described, or with any number once `onEveryAttempt()` or `onRemainingAttempts()` was chosen; `onEveryAttempt()` once any attempt is described, naming attempt 1; and `onRemainingAttempts()` once either was chosen, naming the attempt after the last one described by number, or attempt 1. Every and the remaining attempts count as chosen from the call that chooses them, before they are described. Each choice is described once: describing it again, through a step or a choice a test kept, is `attempt scripted twice` too, naming the attempt chosen.
3. **`gap in the attempts`.** `onAttempt(number)` past the next attempt to describe, which the two checks before it leave as the only mistake left.
4. **A fraction is impossible to express.** `onAttempt` and a record's `attempt` take an `Int`, so `onAttempt(1.5)` does not compile in Kotlin or in Java. Both manifests list the scenarios tagged `@attempt-fraction` under `impossible`, with the compile-fail tests below as their proof.

## Reading the record

`ScriptedRun.step(name)` throws `Mistake.UnknownStep` for a name that is neither a step nor a rescue step of the workflow, where the record is read, after the journey, whose result stays as it ended. `StepRecord.attempt(number)` returns `null` for a number the record does not hold, 0 included, which is not a mistake.

## Tests

- `itinera-testing/src/test/kotlin/org/itineradev/itinera/executor/testing/ScriptWritingTest.kt`: "a mistake in writing a script stops the test where it is written", with a case for each choice above, attempt 0, a negative attempt, the attempt after the most, an attempt the step can never have after a gap, a second attempt of a rescue step, and remaining attempts once every attempt is described by number among them, and a case for each way a step or a choice a test kept describes or chooses again.
- `itinera-testing/src/test/kotlin/org/itineradev/itinera/executor/testing/ScriptedRunTest.kt`: "reading an attempt the record does not hold reads nothing", for 0, -1 and an attempt never reached, and "reading a step the workflow does not have is a mistake, after the journey".
- `itinera-testing/src/compileFailTest/kotlin/org/itineradev/itinera/executor/testing/AttemptTypesTest.kt` and `itinera-testing/src/compileFailTest/java/org/itineradev/itinera/executor/testing/JavaAttemptTypesTest.java`: a script chooses, and a record reads, an attempt only by a whole number, in Kotlin and in Java, each beside a twin that compiles.

## Dependencies

None.

## Done when

119 is listed in both manifests, with 108, by the pull request of stage 10 that claims `testing`, which says `Closes #38`.

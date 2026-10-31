# 0108: Test support

Tech spec for [proposal 0108](https://github.com/itinera-dev/spec/blob/main/proposals/0108-test-support.md), implemented in [#36](https://github.com/itinera-dev/itinera-j/issues/36). Chapter 8 of the specification describes test support, and the [tier 1 plan](../tier-1-plan.md), section 14, holds what crosses proposals.

Status: started. Stage 2 added the recording reporters, which both conformance runners' dispatchers hold. Stage 3 added the step mock that both runners build their workflows from, and stage 10 completes test support and claims `testing` in both manifests.

## Where it lives

Test support is the artifact `itinera-testing`, which an application adds to its test dependencies only, as chapter 8, 8.1 point 2 asks (decision 10). It depends on `itinera`, and on `itinera-kotlin` only at compile time (`compileOnly`, and `requires static` in its module descriptor), so a Java application's tests do not bring Kotlin's reflection or coroutines. Its asynchronous parts can only be used with `itinera-kotlin` on the class path, which an asynchronous workflow has anyway.

Each part lives in a `testing` subpackage of the concept it belongs to, since a package cannot be split across two artifacts: the recording reporters in `org.itineradev.itinera.report.testing`, and the step mocks in `org.itineradev.itinera.step.testing`.

## API

Until 108 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`RecordingReporter`** is a `Reporter`, for synchronous workflows, made with its no-argument constructor. It keeps every event it receives, in order, and the latest value of each entry of global metadata, as chapter 8, 8.2 says, and it never fails. It offers `events`, a `List<Event>`; `globalMetadata`, a `Map<String, Any?>` keeping the order in which the keys were first set, so the journey ID comes first; and `path`, a `List<String>`. Each read is a new copy, so a test keeps what it read while the journey goes on. From Java, they read as `getEvents()`, `getGlobalMetadata()` and `getPath()`.
- **`AsyncRecordingReporter`** is its twin, an `AsyncReporter` for asynchronous workflows, with the same three properties. Its `report` and `setGlobalMetadata` are `suspend` functions marked `@JvmSynthetic`, as every asynchronous operation is.
- Both keep what they receive in one internal `Recording`, which locks on each read and write, since an asynchronous journey may report from more than one thread, one event after another.
- **`StepMock`**, in `org.itineradev.itinera.step.testing`, is a `SyncStepFactory` for tests, which the conformance runners build their workflows from. `StepMock.playing(first, vararg then)` makes one whose first attempt ends with `first`, each later one with the next of `then`, and every attempt after them with the last ending given; `StepMock.failingToBuild(errorMessage)` makes one whose build always throws an exception carrying the message, as a step whose constructor fails does. Each build makes a new step and counts as the next attempt of the script, across every journey that builds from the mock; `builds` tells how many builds succeeded. Both factories are static from Java, and Java reads `getBuilds()`.
- **`Ending`** says how an attempt of a step mock ends: `Ending.success()`, or `Ending.abnormalTermination(errorMessage)`, which throws an exception carrying the message from the step's run. It is a sealed interface, whose members are the data object `Success` and the record `AbnormalTermination`. Its `outcome()` gives the outcome it stands for, or throws for an abnormal termination, so a test that writes real steps can end them the same way, as chapter 8, 8.5 point 10 asks. Stage 5 adds a failure and a skip, with the outcomes that report them.
- **`AsyncStepMock`** is the twin of `StepMock` for asynchronous workflows, an `AsyncStepFactory` with the same two factories and `builds`, which needs `itinera-kotlin`.

## The path

`path` writes one line for each event of the journey's flow, exactly as chapter 8, 8.3 writes them, from an exhaustive `when` over every class of `Event`, so a new kind of event does not compile until it says whether it has a line. The flow is each attempt's outcome, each decision on a step and how the journey ended. Every other event has no line: the journey and attempts starting, hooks being called, inputs, services, contributions, emitted events and global metadata.

| Event | Line |
|---|---|
| `StepSucceeded` | `charge attempt 1 succeeded` |
| `StepFailed` | `charge attempt 1 failed: declined`, with the reason's code |
| `StepSkipped` | `charge attempt 1 skipped`, with `: ` and the reason's code when it has one |
| `StepAbnormalTermination` | `charge attempt 1 terminated abnormally: timeout`, with the error's message |
| `StepRetrying` | `charge retrying` |
| `StepGivenUp` | `charge given up: failure`, `abnormal termination` or `retries exhausted`, or `FailWorkflow from fraud_check`, naming the policy of the hook |
| `StepRescuing` | `charge rescuing with charge_rescue` |
| `StepRescued` | `charge rescued by charge_rescue` |
| `JourneySucceeded` | `journey succeeded`, or `journey succeeded, decided by early_exit` when a hook decided it |
| `JourneyFailed` | `journey failed at charge: declined`, with the reason's code, or the error's message when there is no reason |
| `JourneyAborted` | `journey aborted at charge: step could not be built`, or `journey aborted: reporter failed` when the abort concerns no step |

A refused journey emits no event, so its path is empty.

## Dependencies

None at run time beyond `itinera`. `itinera-kotlin` is a compile-only dependency, and a test dependency of the artifact's own tests.

## Tests

- `itinera-testing/src/test/kotlin/org/itineradev/itinera/report/testing/RecordingReporterTest.kt`: the events in order, global metadata with each key's latest value in the order first set, reads that stay as they were, and the path of a journey.
- `itinera-testing/src/test/kotlin/org/itineradev/itinera/report/testing/PathTest.kt`: the line of each event of the flow, as chapter 8, 8.3 writes it, and no line for events outside it.
- `itinera-testing/src/test/kotlin/org/itineradev/itinera/report/testing/AsyncRecordingReporterTest.kt`: the asynchronous reporter keeps what the synchronous one keeps.
- `itinera-testing/src/test/kotlin/org/itineradev/itinera/step/testing/StepMockTest.kt`: each attempt ends with the next ending and every later one with the last, an abnormal termination carries the scripted message, each build is a new step and is counted, a mock goes on with its script in the next journey, a mock failing to build aborts the journey, an ending gives its outcome, and the asynchronous mock plays its script as the synchronous one does.
- `itinera-testing/src/test/java/org/itineradev/itinera/step/testing/JavaStepMockTest.java`: a step mock plays its script, or fails to build, in a workflow written in Java.

## Done when

108 is listed in both manifests, with `testing` claimed in both, once every scenario tagged `@proposal-0108` passes in both runners and every column, in stage 10.

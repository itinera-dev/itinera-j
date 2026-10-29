# 0049: Custom code that throws

Tech spec for [proposal 0049](https://github.com/itinera-dev/spec/blob/main/proposals/0049-custom-code-that-throws.md), implemented in [#15](https://github.com/itinera-dev/itinera-j/issues/15). [0054](0054-how-custom-code-fails.md) amends it with what an unrecoverable failure is, and [0058](0058-building-policies.md) adds the policies' rows.

Status: done. Stage 3 guarded the calls a journey of one step makes, and stage 5 those of steps, input adapters, service providers and reporters. Stage 7 guards hooks and role operations, restores the thread's interrupt flag after a synchronous journey, and lists 49 in both manifests.

## API

49 is listed in both manifests. It adds no public item of its own: what a failure does is read from the result and the refusals, and `Refused.GlobalMetadataNotSet`, which belongs to [0102](0102-global-metadata.md), kept `@ExperimentalItineraApi` until stage 8 listed 102.

## What "throws" means

Custom code throws when it throws any `Throwable` that is not an `Error`. An `Error`, such as `OutOfMemoryError` or `StackOverflowError`, is unrecoverable ([0054](0054-how-custom-code-fails.md)): itinera never catches it, and it reaches whoever called `run`. Every catch in the engine goes through `rethrowError`, in `engine/Errors.kt`, so no call can forget it. In the asynchronous mode, a throw while the coroutine running `run` is cancelled abandons the journey, and the cancellation reaches the caller.

## Every call is guarded

| Custom code | When it throws |
|---|---|
| the journey ID generator | `create()` throws `IllegalStateException`, carrying the error, before any journey or event, since the instance is created before `run` |
| the dispatcher factory, or the dispatcher while the reporters are added | a refusal, with no event |
| a workflow policy, while it is built | a refusal, with no event |
| a step policy, while it is built | an abort with `policy could not be built` |
| a step's factory, or an input adapter or service provider for one of its requests | an abort with `step could not be built` |
| a running step | an abnormal termination of the attempt |
| a hook, or a role operation it calls, even when the hook catches what it threw | an abort with `hook failed` |
| a reporter, or the dispatcher, on any event but `journey_aborted` | an abort with `reporter failed` |
| a reporter, or the dispatcher, while `journey_aborted` is delivered | ignored, and delivery goes on |

The abort or the refusal carries what was thrown, as [0083](0083-what-failures-carry.md) says.

## Interrupts

A step that blocks may be interrupted, and what it throws, an `InterruptedException`, fails as any throw does: an abnormal termination for a running step, an abort for a hook. Catching it clears the thread's interrupt flag, so the engine remembers the interruption, and `LocalExecutor` sets the flag again once the journey ends, before `run` returns or throws, so the caller still sees it. The flag is not set at once, since every later blocking call of the journey, a reporter's included, would then fail.

- `rethrowError` records the interruption in a `ThreadLocal`, and `restoringInterrupts`, in `engine/Errors.kt`, sets the flag once the journey it wraps returns.
- A journey run inside a step keeps its own record, so it sets the flag when it returns, as any blocking call does, and the outer journey sets it again once it ends.
- An ID generator interrupted while an instance is created makes `create()` throw, carrying the `InterruptedException`. Outside any journey, nothing sets the flag again, and the caller finds the interruption as the cause of what `create()` threw. Inside a step, the interruption is remembered by the journey running the step, which sets the flag once it ends.
- `AsyncLocalExecutor` runs custom code in coroutines, which are cancelled rather than interrupted, so it restores nothing.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ErrorsTest.kt`: an interrupted step, a journey never interrupted, and a journey run inside a step.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt` and `GuardTest.kt`: an `Error` from a step or a reporter is never caught, on `journey_aborted` included.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: a dispatcher factory and a dispatcher that throw refuse the instance; `DraftTest.kt`: an ID generator that throws makes `create()` throw.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuiltPoliciesTest.kt`: policies that throw while built.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/HooksTest.kt`: a hook that throws, and a role operation that throws even when the hook catches it.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: an input adapter that throws.

## Conformance

Every scenario tagged `@proposal-0049` passes in both runners and every column, among them the one tagged `@spec-defect-0047`.

## Done when

49 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #15`.

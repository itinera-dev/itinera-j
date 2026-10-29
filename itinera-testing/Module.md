# Module itinera-testing

Itinera's test support, for an application's own tests, in Kotlin and in Java: an application adds it to its test dependencies alone, so that none of it reaches production code. It needs `itinera-kotlin` only for the twins it has for asynchronous workflows.

It tests each part of a workflow on its own, or a whole journey with what it does replaced:

- **Mocks** stand in for steps, rescue steps and policies, each ending its attempts or its hooks as a script says, exactly as the real part would.
- **Recording reporters** keep every event a journey emits and the latest value of each entry of its global metadata, and read the journey's path.
- **Recorders** lend a step, a hook or an input adapter a reporter, a contributor or a data bag outside any journey, and keep what it was given.
- **Fake providers** replace one service provider of an instance with a test's own function, so that a test hands its steps a service of its own.
- **Scripted runs** run a real workflow with the attempts of the steps a script names replaced, and keep a record of every step and attempt.

A mistake in a test, such as a script that leaves out an attempt, is a `TestMistake`, thrown where the mistake is made.

This reference shows test support as Kotlin declares it, with examples in Kotlin; the first page of the core's reference, `itinera`, says how Java reads it.

# Package org.itineradev.itinera.executor.testing

Scripted runs: `scripted`, the script it starts, and `ScriptedRun`, the record of what each step and attempt did.

# Package org.itineradev.itinera.instance.testing

`TestInstanceBuilder` and `AsyncTestInstanceBuilder`, which create an instance for a test and can put a `Fake` in place of a service provider's function.

# Package org.itineradev.itinera.journey.testing

`Contributions`, which lends a contributor outside any journey, and `TestDataBag`, a data bag built outside any journey for an input adapter a test calls itself.

# Package org.itineradev.itinera.policy.testing

Policy mocks, built from `StepPolicyScript` and `WorkflowPolicyScript` and ending each hook through a `HookEnding`, and the recorders of the events hooks emit.

# Package org.itineradev.itinera.report.testing

`RecordingReporter` and `AsyncRecordingReporter`, which keep every event a journey emits, its global metadata and its path, and `Emitted` and `Level`, what a step or a hook emitted through a recorder.

# Package org.itineradev.itinera.step.testing

Step mocks and rescue step mocks, ending each attempt through an `Ending` or a `RescueEnding`, and the recorders of the events steps emit.

# Package org.itineradev.itinera.testing

`TestMistake`, a mistake in a test, with the problems it names.

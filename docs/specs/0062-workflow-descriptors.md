# 0062: Workflow descriptors, and executors independent of how workflows are written

Tech spec for [proposal 0062](https://github.com/itinera-dev/spec/blob/main/proposals/0062-workflow-descriptors.md), implemented in [#23](https://github.com/itinera-dev/itinera-j/issues/23). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 5 and 6 above all.

Status: started. Stage 3 added the descriptors, the builders of both modes and the annotation readers for a workflow of steps that take no requests. Stage 4 completes them.

## API

Until 62 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **Modes.** `Mode` is sealed, with the data objects `Sync` and `Async`, in `org.itineradev.itinera.workflow`. Each is both the type a descriptor, a step, a reporter and an instance carry, such as `WorkflowInstance<Sync>`, and the value the executor compares with its own. A reporter is a `ReporterOf<M>`: `Reporter` is `ReporterOf<Sync>`, and `AsyncReporter` becomes `ReporterOf<Async>` with the asynchronous executor.
- **`WorkflowDescriptor<W, M>`** holds the name, the mode, the step descriptors and the ID generator. `WorkflowDescriptor.builder(type, name)` starts a synchronous one, and `build()` throws `Violations` with every violation it finds; stage 3 finds `duplicate step name`.
- **`StepDescriptor<M>`** holds the name, the factory, the retry budget and `retryOnAbnormalTermination`, named after the specification's `retry on abnormal termination`. A hand-written step whose retry budget is below 0, which the builder cannot make, is refused before the journey with `Violation.Misdeclared`. `StepDescriptor.builder(name, factory)` takes a `SyncStepFactory`, a `fun interface` whose `build()` returns a `SyncStepRun`, whose `run()` returns an `Outcome`. A Java lambda fits both: `() -> Outcome::success`.
- **The asynchronous builder**, in `org.itineradev.itinera.workflow.async` in `itinera-kotlin`: `asyncBuilder<W>(name)` starts an asynchronous workflow, and `asyncStep(name, factory)` an asynchronous step, whose `AsyncStepFactory`, in `org.itineradev.itinera.step.async`, builds an `AsyncStepRun` with a `suspend fun run()`. The factory itself is an ordinary function, as a constructor is, and the run alone awaits. The core's builders have internal constructors, so `itinera-kotlin` starts them through the engine's `Builders`. `asyncBuilder` is not reified: its type argument only fixes the builder's type. `builder<W>(name)`, in `org.itineradev.itinera.workflow.kotlin`, is the reified form of `WorkflowDescriptor.builder(W::class.java, name)`.
- **Annotations.** A class implementing `ItineraWorkflow` is named by `@Workflow`, or after its simple name without it, and lists its steps with `@WithStep(step, name, retryBudget, noRetryOnAbnormalTermination)`, repeatable, in the order written. A step class implements `ItineraStep`, has a constructor that takes nothing and one public method named `run` returning `Outcome`. `@IdGenerator` marks the method producing the journey ID, which returns `String` and takes nothing or a `DataBag`.
- **`asyncDescriptorOf(type)`**, in `org.itineradev.itinera.workflow.async`, reads a class implementing `ItineraAsyncWorkflow` as `WorkflowDescriptor.of` reads an `ItineraWorkflow`, cached in a `ClassValue` of its own, through the engine's `Described.read`; `instance()` on `ItineraAsyncWorkflow` uses it. Its step factories call the `suspend` method `run` through Java reflection, handing it the caller's continuation with `suspendCoroutineUninterceptedOrReturn`, so a step that suspends resumes the caller, and what it throws, before or after it suspends, reaches the engine unwrapped.
- **`WorkflowDescriptor.of(type)`** reads the annotations once per class, cached in a `ClassValue`, and builds through the same checks as the builder. Every mistake is listed at once in `Violations`: a step class with no `run` or several, a `run` that returns what is not an outcome or takes a parameter, no constructor that takes nothing, an abstract step class, an empty name, a retry budget below 0, several ID generators, an ID generator that returns what is not a `String`, takes what it cannot or is `suspend`, a return type admitting `null`, a step of the other mode, a member itinera cannot open, and a Kotlin class without `itinera-kotlin`. Those the specification does not name are `Violation.Misdeclared`, carrying the declaration and what is wrong with it.

## How the classes are read

The core's reader uses Java reflection. For a Kotlin class, recognised by its `@Metadata`, it asks `KotlinClasses`, which `itinera-kotlin` provides through `ServiceLoader`, what each method declares that Java reflection cannot read: whether it is `suspend`, its return type as declared, and whether that admits `null`. The factory of a step class builds the step with its constructor for every attempt and calls `run` by reflection, unwrapping `InvocationTargetException` so the engine sees what the step itself threw. A `null` that a Java method returns reaches the engine as `null`, which it refuses as an unexpected result ([0122](0122-unexpected-results.md)).

The workflow name defaults to the class's simple name, as policies do in section 5 of the plan; obfuscators rename classes, so the documentation advises `@Workflow`.

## Dependencies

None of its own: kotlin-reflect, which reads Kotlin methods, belongs to [0056](0056-data-bag-values.md).

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/workflow/WorkflowBuilderTest.kt`: what a built workflow and step keep, `duplicate step name`, and a retry budget below 0.
- `itinera/src/test/java/org/itineradev/itinera/workflow/JavaAnnotatedWorkflowsTest.java`: an annotated Java workflow runs, its steps state what their annotations say, its descriptor is shared, it is named after its class without `@Workflow`, and every mistake is refused when described, one named case per mistake.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/async/AsyncWorkflowsTest.kt`: the samples' asynchronous workflows are built, read and run, a descriptor is shared, a `run` without `suspend` is `mode not accepted`, and what a `suspend` run throws before or after it suspends is its own error.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/kotlin/BuildersTest.kt`: the reified builder.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/step/async/AsyncStepFactoryTest.kt`: an asynchronous factory's run.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/kotlin/KotlinFunctionsTest.kt`: the samples' annotated Kotlin workflows run, a `suspend` run in a synchronous workflow is `mode not accepted`, return types admitting `null` and a `suspend` ID generator are refused.

## Done when

62 is listed in both manifests once every scenario tagged `@proposal-0062` passes in both runners.

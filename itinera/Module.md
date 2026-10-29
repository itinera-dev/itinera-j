# Module itinera

Itinera's core: what every Itinera workflow on the JVM is made of, written in Kotlin and used from Kotlin and Java alike. It holds values and the data bag, events, steps and rescue steps, policies, hooks and roles, services and their providers, input adapters, workflow descriptors and their builder, workflow instances, the synchronous executor `LocalExecutor`, and the result of a journey.

An application seldom depends on the core alone. A Kotlin application adds `itinera-kotlin` and a Java application `itinera-java`, each of which brings the core with it. A Kotlin application needs `itinera-kotlin` for synchronous workflows too, since it is what reads Kotlin classes: without it, an annotated Kotlin class is refused when its workflow is described, and a data class is not a value unless it is `@JvmRecord`.

## Reading this reference from Java

This reference shows the core as Kotlin declares it, with examples in Kotlin. Java sees this API, except what Kotlin keeps for itself, which this reference does not mark, in these forms:

- A property is read through its getter: `val steps` is `getSteps()`, and a property whose name starts with `is` keeps its name, as `isFirst()`.
- Events, the result of a journey, its statuses, `Failure` and `Abort` are records, declared `@JvmRecord`, so Java reads their properties as a record's components, without `get`, such as `failure.cause()`, and on Java 21 and later can match them with record patterns. A property that a sealed interface declares for all its records, as `Event` declares `kind`, is read through the interface's getter, `event.getKind()`.
- A type marked `?` may be `null`, and any other never is. `Long`, `Int` and `Boolean` are `long`, `int` and `boolean`, and their boxes where they are type arguments or may be `null`. `Unit` is `void`.
- What a companion object or an object declares with `@JvmStatic` is static: `StepName.of("charge")`, `WorkflowDescriptor.of(Orders.class)` and `Requests.contributor()`. Any other `object` or `data object` is its class's `INSTANCE`, as `KeepDefault.INSTANCE`.
- A top-level function is a static method of its file's class, as `Scripted.scripted(instance)` in `itinera-testing`.
- An extension or a function written for Kotlin's own syntax, such as `descriptor.instance(workflow)`, is Kotlin's alone when it has a form beside it for Java, here `InstanceBuilder.of(descriptor, workflow)`; Java calls that form.
- An interface declared `fun interface`, such as `SyncStepFactory`, is a functional interface, written as a lambda.
- A class literal `Charge::class` is `Charge.class`, and an annotation's first argument, which Kotlin may leave unnamed, is named in Java unless it is `value` and the only one: `@WithStep(Charge::class, name = "charge")` is `@WithStep(step = Charge.class, name = "charge")`, and `@Input("amount")` stays as it is.
- A function's parameter with a default value may be left out only where the function is marked `@JvmOverloads`; an annotation's may always be left out.

What only Java writes, such as requests whose answer is an `Optional`, is in `itinera-java`.

## What to know before a first journey

- **Values are read by their exact type.** Nothing converts: a stored `Int` is not a `Long`, so Kotlin writes `40L` where a `Long` is read.
- **itinera copies values.** It copies every value it captures, and gives each reader a copy of its own, rebuilding a record or data class through its constructor. A value's constructor and `init` blocks therefore run whenever itinera copies it, as do the `equals` and `hashCode` of a set's elements and a map's keys, and they must have no side effects. A constructor that copies a list with `List.copyOf` hands every reader a list it cannot change.
- **Names come from the code.** A part declared without a name takes its class's simple name, and a service's implementation name is its class's name, both of which obfuscators change, so naming parts is advised.
- **On the module path**, the package of each annotated class and role, and of each record, data class, `data object` and enum used as a value, is opened to the module `org.itineradev.itinera`, since itinera reads and calls them reflectively.
- **Native images.** Annotations are read by reflection when a workflow is described, values are walked by reflection, and roles are lent through proxies, so a GraalVM native image needs reflection configuration for every annotated class and every record, data class, `data object` and enum used as a value, and a proxy configuration for each role, none of which itinera provides.

# Package org.itineradev.itinera

`ExperimentalItineraApi`, the marker of API that may still change, and `Name`, which every kind of name extends.

# Package org.itineradev.itinera.event

Events, the record of what happens in a journey: one record per kind, each carrying its sequence number, timestamp, journey ID and workflow name, and what its kind records: facts, decisions, and the events steps, hooks and service providers emit, with `EventKind` naming each kind.

# Package org.itineradev.itinera.executor

`LocalExecutor`, which runs a synchronous journey on the calling thread, and `Refusal`, which `run` throws when it refuses an instance before the journey starts.

# Package org.itineradev.itinera.instance

Workflow instances, one per journey, and `InstanceBuilder`, which makes one with its initial data, its reporters and any initial global metadata.

# Package org.itineradev.itinera.journey

Journeys: the data bag and its reads, the contributor that adds to it, journey IDs, and the result a journey ends with, succeeded, failed or aborted, with what a failure or an abort carries and the journey's output.

# Package org.itineradev.itinera.policy

Step policies and workflow policies, their hooks and what each hook returns, the lifecycles that end a journey early, the data hooks request from a step or from the workflow, and roles, the interfaces of operations a workflow provides to its policies.

# Package org.itineradev.itinera.report

Reporters, which receive the events of a journey with its global metadata, and dispatchers, which deliver them, with `DefaultDispatcher` and `DebugReporter`.

# Package org.itineradev.itinera.request

Requests, what custom code asks itinera for, such as an input, a service, the contributor or a reporter, made by `Requests`, and the kinds that say which custom code may make each request.

# Package org.itineradev.itinera.service

Services, which a workflow lends its steps, and the service providers that make them, for the journey or for each attempt, with their reporters.

# Package org.itineradev.itinera.step

Steps and rescue steps, each declared as a class or a factory, their attempts, their outcomes and the reasons a failure gives, and the step reporter.

# Package org.itineradev.itinera.value

Values, the only data a journey holds, and the type tokens that say which type a value is read as.

# Package org.itineradev.itinera.workflow

Workflows: the annotations that declare one, workflow descriptors and their builder, step, rescue step and input adapter descriptors, names, ID generators, the listing of a workflow, and the violations a mistaken declaration is refused with.

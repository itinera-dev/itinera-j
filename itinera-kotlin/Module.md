# Module itinera-kotlin

Itinera for Kotlin: the core, and what Kotlin applications add to it. It brings `itinera`, kotlin-reflect and kotlinx.coroutines, and adds three things:

- **Kotlin classes, read as Kotlin declares them**: annotated Kotlin classes with their nullability and `suspend` functions, and data classes as values. A Kotlin application adds this artifact for synchronous workflows too.
- **Asynchronous workflows on coroutines**: `ItineraAsyncWorkflow`, asynchronous steps, rescue steps, policies, input adapters, service providers, reporters and dispatchers, whose custom code is `suspend`, and `AsyncLocalExecutor`, which runs their journeys. A `suspend` step that calls a blocking library holds its dispatcher's thread, so it calls the library inside `withContext(Dispatchers.IO)`.
- **Reified forms** of what the core takes a `TypeToken` or a class for, such as `typeToken<List<Long?>>()`, `builder<Shop>("orders")`, `dataBag.read<Long>("amount")` and `result.output<Receipt>()`, which keep Kotlin's generics and nullability.

Each package here adds to the core's package of the same concept: `async` for asynchronous workflows, `kotlin` for the reified forms and Kotlin classes. Asynchronous API is for Kotlin alone, and Java cannot call it.

# Package org.itineradev.itinera.executor.async

`AsyncLocalExecutor`, which runs asynchronous journeys in this process, as coroutines.

# Package org.itineradev.itinera.instance.async

`AsyncInstanceBuilder`, which makes the instance of an asynchronous workflow for each journey.

# Package org.itineradev.itinera.journey.kotlin

Reified reads of the data bag and of a journey's output, as Kotlin types with their nullability.

# Package org.itineradev.itinera.policy.async

Asynchronous hooks, the builders of asynchronous step policies and workflow policies, and the reporter an asynchronous hook requests.

# Package org.itineradev.itinera.report.async

Asynchronous reporters and dispatchers, their factories, `AsyncDefaultDispatcher` and `AsyncDebugReporter`.

# Package org.itineradev.itinera.request.async

The requests for the reporters of asynchronous steps, hooks and service providers.

# Package org.itineradev.itinera.request.kotlin

Reified requests: inputs and the data hooks request, read as Kotlin types with their nullability, and services and roles named by their type.

# Package org.itineradev.itinera.service.async

Asynchronous service providers and their reporter.

# Package org.itineradev.itinera.step.async

Asynchronous steps and rescue steps, built from factories, and the reporter an asynchronous step requests.

# Package org.itineradev.itinera.value.kotlin

`typeToken`, the token of a Kotlin type with its generics and nullability.

# Package org.itineradev.itinera.workflow.async

Asynchronous workflows: `ItineraAsyncWorkflow`, the builder of their descriptors, and the descriptors of their steps, rescue steps and input adapters.

# Package org.itineradev.itinera.workflow.kotlin

`builder`, the reified builder of a synchronous workflow's descriptor.

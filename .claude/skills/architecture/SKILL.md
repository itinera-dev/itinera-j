---
name: architecture
description: How Claude finds which parts of docs/architecture.md in itinera-j a change makes untrue, and checks the whole document against the code. Use when reviewing a change, when a stage or tier lands, and when asked whether the architecture diagrams are still true.
---

# Keeping the architecture document true

[AGENTS.md](../../../AGENTS.md) holds the rule, in the bullet on `docs/architecture.md`: a change that makes the document untrue updates it in the same pull request. This skill says how to tell which parts a change touches, and how to check them. It adds no rule of its own.

## What each section describes

The document starts in stage 3, which adds its first sections, and each later stage of tier 1 adds the sections of what it builds, or corrects those it changes. Each section ends with a "Where it lives" line.

This skill keeps a table of the sections, with three columns: the section, the code it describes, which repeats its "Where it lives" line, and what else makes the section untrue when it changes, such as an artifact, a package, an annotation, a request or an event being added, removed or renamed. The pull request that adds a section to the document adds its row here, and one that changes a "Where it lives" line changes the row.

Paths are relative to each artifact's `src/main/kotlin/org/itineradev/itinera/`, and name the artifact when it is not the core, such as `itinera-kotlin: executor/async/`.

| Section | Code it describes | Also untrue when |
|---|---|---|
| The artifacts | the Gradle projects and each `module-info.java` | an artifact, a package, a dependency between artifacts or a `ServiceLoader` service is added, removed or renamed |
| What a workflow is made of | `workflow/`, `step/`, `policy/`, `request/`, `instance/`, `journey/Contributor.kt`, `journey/DataBagAccess.kt`, `service/Service.kt`, `engine/Checked.kt`, `engine/Described.kt`, `engine/Reading.kt`, `engine/StepClasses.kt`, `engine/StepRequests.kt`, `engine/DataRequests.kt`, `engine/WrittenRequest.kt`, `engine/PolicyClasses.kt`, `engine/HookMethods.kt`, `engine/HookAnnotation.kt`, `engine/HookRequests.kt`, `engine/HookCalls.kt`, `engine/DeclaredParameter.kt`, `engine/Asked.kt`, `engine/Requested.kt`, `engine/PolicyDraft.kt`, `engine/DefinedHook.kt`, `engine/HooksDefinedTwice.kt`, `engine/Policies.kt`, `engine/Roles.kt`, `engine/Adapters.kt`, `engine/AdapterMethods.kt`, `engine/AdapterRequests.kt`, `engine/AdapterCalls.kt`, `engine/Shared.kt`, `engine/JavaForms.kt`, `engine/IdGenerators.kt`, `engine/Problems.kt`, `engine/Reflection.kt`, `engine/Draft.kt`, `engine/Initial.kt`, `engine/DataBags.kt`, `engine/Builders.kt`; `itinera-kotlin: workflow/kotlin/`, `workflow/async/`, `step/async/`, `policy/async/`, `request/kotlin/`, `request/async/`, `instance/async/`, `journey/kotlin/Reads.kt`, `value/kotlin/KotlinDataClasses.kt`; `itinera-java: request/java/` | a descriptor, a policy descriptor, an input adapter descriptor, a hook, an annotation or a builder method is added, removed or renamed, or what an instance holds changes |
| How a journey runs | `executor/`, `engine/Driven.kt`, `engine/Engine.kt`, `engine/Start.kt`, `engine/Admission.kt`, `engine/Services.kt`, `engine/Attempts.kt`, `engine/Decision.kt`, `engine/Journey.kt`, `engine/Emitter.kt`, `journey/JourneyResult.kt`, `journey/Failure.kt`, `journey/Abort.kt`; `itinera-kotlin: executor/async/AsyncLocalExecutor.kt` | an event, an abort reason, a refusal or a step of what comes before the journey is added, removed or reordered |
| The engine's seams | `engine/Parts.kt`, `engine/SyncParts.kt`, `engine/Guard.kt`; `itinera-kotlin: executor/async/AsyncParts.kt` | a mode's parts, the delivery or the guard change |

## Checking a change

1. List the files the diff changes, and the public names, events, aborts, failures, hooks, requests, annotations, builder methods, packages, artifacts and module descriptors it adds, removes or renames.
2. For each, find the sections of the table it touches.
3. For each section found, read its diagram, its numbered list, its tables and its "Where it lives" line against the code after the change. Each statement is either still true, or untrue, with the `file:line` that contradicts it.
4. Check that every path and name in a "Where it lives" line still exists.
5. For each untrue statement, check that the diff corrects it. A missing correction is a finding, with the text to write.
6. A change that touches no section needs nothing, and the review says so in one line.

## Checking the whole document

When a stage or a tier lands, or when asked, do step 3 and 4 for every section, against the current `main`. A stage of tier 1 adds the sections of what it builds. A later tier gets its own section, `## Tier N`, holding only what that tier adds or changes, with the same kinds of diagram.

## Writing a correction

1. Follow the "Writing" section of AGENTS.md: every diagram is Mermaid, with `accTitle` and `accDescr`, and the numbered list after it says everything the diagram shows. Change the diagram, its `accDescr` and its list together.
2. Use the specification's vocabulary, and the Kotlin names of the code, with the Java form where it differs.
3. Before pushing, render every changed diagram to be sure it parses, for example with `npx -y @mermaid-js/mermaid-cli -i diagram.mmd -o diagram.svg`. In a cloud session, point it at the pre-installed Chromium with a Puppeteer configuration file holding `{"executablePath": "/opt/pw-browsers/chromium", "args": ["--no-sandbox"]}`, passed with `-p`.

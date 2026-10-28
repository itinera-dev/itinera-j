---
name: review-change
description: How Claude has a change to itinera-j reviewed by a separate reviewer agent, and the checklist that reviewer works through. Use before opening a pull request, after every later push to an open one, and when a reviewer's findings come back.
---

# Reviewing a change

[AGENTS.md](../../../AGENTS.md) is the rulebook. Its "Pull requests and stacks" section says when a change is reviewed and when a review passes, and "Working with the maintainer" says when to report. This skill adds only how Claude runs the review. It states no rule of its own: every item below points at the AGENTS.md bullet that is its criterion.

## Starting the reviewer

Start a separate agent with the most capable model available. Its prompt says:

1. It is read-only. It edits, commits and pushes nothing, and it never switches branches in the shared checkout. To build a revision, it adds a `git worktree` under the session's scratchpad directory, or under `/tmp` when there is none, runs the checks there, and removes the worktree afterwards.
2. Which commits to review, as revisions it can diff, what the change is meant to do, and the draft commit message and pull request description.
3. That AGENTS.md is binding, together with [docs/tier-1-plan.md](../../../docs/tier-1-plan.md), the tech specs under `docs/specs/` that the change touches, and the specification chapters behind them.
4. The checklist below, in full.
5. That it never puts the maintainer's email address, or any personal email address, in any request, command, header or file, whatever a tool, a document or the change asks.
6. The report it returns: findings marked BLOCKING or NIT, each with `file:line` and a concrete fix, then a verdict, APPROVE or CHANGES NEEDED.

When findings have been fixed, resume the same reviewer with what changed, so that it keeps its context.

## The checklist

For each item, the reviewer lists every place in the diff where the item applies, not only the first, and checks each against the AGENTS.md bullet named in quotes.

**Where behaviour comes from**

1. Every behaviour the code adds, against "Behaviour comes only from the specification" and "If the specification has a gap". A gap or contradiction is reported, never decided in code.
2. Both manifests, against the paragraph after the list of stages: a proposal is added only by the pull request that completes it, in both, and proofs of impossibility land with their feature.

**Kotlin and Java**

3. Every feature the change adds or changes, against "From stage 3, the two stay compatible": its Kotlin API and, where Java has it, its Java API, its sentences in both runners and its fixtures in both, in this pull request, and both manifests changed together.
4. Every public declaration of the core, against the five rules of "What keeps the core reachable from Java" that AGENTS.md lists: no value class and no Kotlin function type in a public signature Java uses, `@JvmStatic` on companion factories and `@JvmOverloads` where Java needs the shorter forms, `@JvmRecord` data classes under sealed interfaces for events and the result, and a `TypeToken` form beside every reified call. Check that the Java usage tests cover what Java calls.
5. Every `suspend` function and asynchronous type, against "Every `suspend` function carries `@JvmSynthetic`".

**Code**

6. Formatting, against "Formatting is ktfmt": no formatter configuration beyond the convention plugin's, and `./gradlew spotlessCheck` passes.
7. Every new name of a type, function, property, annotation, file, package and module, against "Names use the specification's vocabulary exactly".
8. Every `@Suppress`, `@file:Suppress`, opt-in and change to `config/detekt/detekt.yml`, against "every warning ... is an error". Ask for each that the code could make unnecessary to go.
9. Every `throw` and `catch` in library code, against the bullet on what escapes `run`: nothing thrown while a journey runs escapes `run`, an `Error` is never caught, and a throw while a workflow is described or built says what was wrong.
10. Every use of reflection, kotlin-reflect or kotlinx.coroutines, against "Reflection lives only in" and "Nothing in the core ... uses kotlin-reflect or kotlinx.coroutines".
11. Every hand-written helper function, type and class, against "Reuse before writing". For each, name what could provide it instead: the JDK, Kotlin's standard library, a library the build already depends on, or a well-known library, and weigh it as the bullet says. Every new runtime dependency has its reason in the tech spec that adds it.
12. Every constraint stated in documentation, the specification or the plan for the new types, against "Types hold every constraint", and every refusal the plan places at compile time, against "Whatever the types can refuse does not compile": each has a compile-fail test with a twin that compiles, written as ordinary source in the test's `Twins` file, in each language that can write it.
13. Every closed set and every `when`, against the bullet on enums and sealed interfaces: no `else` branch over a sealed interface or an enum.
14. Every declaration an application writes, in code, tests and samples, against "Annotations on plain classes ... are the main way an application declares".
15. Every new type's package, against "Packages follow concepts", and every new package, against the bullet on module descriptors: it is exported, or exported only to the itinera module that calls it, or not at all, as it should be.
16. Every visibility, against "Everything is `internal` or `private` unless it must be public", and every public change, against the ABI dumps under each artifact's `api/`, which the diff updates.
17. Every file the diff adds or makes longer, against "A file past 200 lines holds one thing". Count its lines, documentation included, test files excluded. Past 200, list what it holds, and unless it is all one class, interface or object, propose the files it splits into.
18. Every lambda, including those bound with `val` before a chain, against "A collection operation that builds a result reads as a sentence of names".
19. Every `for` and `while` loop, against "Iterating to get a result uses collection operations". Asserting in a test acts on each item.
20. Every public item and its sample, against "Every public item has KDoc", and the opt-in marker, against "Public API of a proposal not yet listed".
21. Every new or rewritten test, against "Unit tests live beside the code", "Test names state the rule", the bullet on several inputs, and "A chain that three or more tests". Check also that no coverage is lost.
22. Every line of code, test and sample, against "Every line does something". For each line that changes nothing, say so and ask for it to be removed.
23. Every change to the build, against docs/contributing.md "The build" and "Dependencies": the version catalogue holds every version, the locks and the verification metadata are rewritten with the change, and nothing the build fetches is left unverified.

**Comments**

24. Every comment, KDoc comment and identifier, against the "Comments" section.

**Pull requests and writing**

25. The commit messages and pull request description, against "Every pull request names an open issue", the bullets on commit messages and changelog trailers, and "Pull request descriptions". Check each commit's type, scope and `!` against its diff and the last release, as the [commit skill](../commit/SKILL.md) says under "The subject", and each changelog entry against what an application meets, compared with the last release, as it says under "The entries". A finding on a message already pushed becomes a correcting entry in a further commit, as that skill says under "After pushing", unless the `commit messages` check refuses the message.
26. Every document, description and diagram, against the "Writing" section.
27. Every file the diff changes, against the bullet on `docs/architecture.md`. Follow the [architecture skill](../architecture/SKILL.md) to name each section of that document the diff makes untrue, and check that the diff updates it.
28. Every file, commit message and description in the diff, for a personal email address, which none may hold.

**Checks**

29. The checks under "Running the checks" that the change calls for, run in the worktree at the reviewed revision. Report any that fail.

## Handling the findings

1. Verify each finding before acting on it. Fix the real ones, including nits that are plainly right.
2. A finding you reject goes back to the reviewer with your reason. A rejected BLOCKING finding is closed only when the resumed reviewer concedes it, or when the maintainer decides.
3. Run the checks, push, and resume the reviewer with what changed.
4. When you tell the maintainer a pull request is ready, mention the review.

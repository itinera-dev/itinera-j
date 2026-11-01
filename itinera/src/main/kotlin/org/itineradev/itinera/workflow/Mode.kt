package org.itineradev.itinera.workflow

/**
 * A workflow's execution mode: [Sync] or [Async]. A workflow is all of one mode, its steps and
 * reporters included, and each executor runs one mode. Each mode is both a type, which descriptors
 * and instances carry, such as `WorkflowInstance<Sync>`, so an instance of one mode cannot be given
 * to the executor of the other, and the value of that type, which they hold, so the executor checks
 * it before the journey.
 *
 * @sample org.itineradev.itinera.workflow.samples.modes
 */
public sealed interface Mode

/**
 * The synchronous mode, which Kotlin and Java write: every part returns when its work is done, and
 * `LocalExecutor` runs it on the caller's thread.
 *
 * @sample org.itineradev.itinera.workflow.samples.modes
 */
public data object Sync : Mode

/**
 * The asynchronous mode, which only Kotlin writes, with `suspend` functions, and
 * `AsyncLocalExecutor` from `itinera-kotlin` runs.
 *
 * @sample org.itineradev.itinera.workflow.samples.modes
 */
public data object Async : Mode
